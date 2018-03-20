package com.dmi.cointrader.app.neural

import com.dmi.cointrader.app.archive.History
import com.dmi.cointrader.app.archive.HistoryBatch
import com.dmi.cointrader.app.trade.TradeConfig
import com.dmi.util.io.ResourceContext
import com.dmi.util.math.Matrix2D
import com.dmi.util.math.Matrix4D
import jep.Jep
import jep.NDArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.cbor.CBOR.Companion.load
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

private val neuralNetworkCreated = AtomicBoolean(false)
private val neuralTrainerCreated = AtomicBoolean(false)

fun ResourceContext.trainedNetwork(): NeuralNetwork {
    val jep = jep().use()
    return NeuralNetwork.load(jep, Paths.get("data/network"), gpuMemoryFraction = 0.2).use()
}

fun ResourceContext.trainingNetwork(jep: Jep, config: TradeConfig): NeuralNetwork {
    return NeuralNetwork.init(jep, NeuralNetwork.Config(config.assets.all.size, config.historySize, 3), gpuMemoryFraction = 0.5).use()
}

fun ResourceContext.networkTrainer(jep: Jep, net: NeuralNetwork): NeuralTrainer {
    return NeuralTrainer(jep, net).use()
}

typealias Portions = List<Double>
typealias PortionsBatch = List<Portions>
typealias PriceIncs = List<Double>
typealias PriceIncsBatch = List<PriceIncs>
typealias Fees = List<Double>
typealias FeesBatch = List<PriceIncs>

class NeuralNetwork private constructor(
        private val jep: Jep,
        val config: Config,
        gpuMemoryFraction: Double,
        loadFile: Path?
): AutoCloseable {
    init {
        if (neuralNetworkCreated.getAndSet(true)) {
            throw UnsupportedOperationException("Two created neural networks doesn't support")
        }

        jep.eval("from cointrader.network import NeuralNetwork")
        jep.eval("network = None")
        jep.eval("""
                def create_network(coin_number, history_size, indicator_number, gpu_memory_fraction, load_path):
                    global network
                    network = NeuralNetwork(coin_number, history_size, indicator_number, gpu_memory_fraction, load_path)
            """.trimIndent())
        jep.eval("""
                def best_portfolio(current_portfolio, history):
                    return network.best_portfolio(current_portfolio, history)
            """.trimIndent())
        jep.invoke("create_network", config.coinNumber, config.historySize, config.indicatorCount, gpuMemoryFraction, loadFile?.toAbsolutePath()?.toString())
    }

    fun bestPortfolio(currentPortfolio: Portions, history: History): Portions {
        return bestPortfolio(currentPortfolio.toMatrix(), history.toMatrix()).toPortions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun bestPortfolio(currentPortfolio: Matrix2D, histories: Matrix4D): Matrix2D = synchronized(this) {
        require(currentPortfolio.n2 == config.coinNumber)
        require(histories.n2 == config.indicatorCount)
        require(histories.n3 == config.coinNumber)
        require(histories.n4 == config.historySize)
        require(currentPortfolio.n1 == histories.n1)

        val npportfolio = NDArray(currentPortfolio.data, currentPortfolio.n1, currentPortfolio.n2)
        val nphistory = NDArray(histories.data, histories.n1, histories.n2, histories.n3, histories.n4)

        val result = jep.invoke("best_portfolio", npportfolio, nphistory) as NDArray<FloatArray>

        val dataDouble = result.data.map { it.toDouble() }.toDoubleArray()
        return Matrix2D(result.dimensions[0], result.dimensions[1], dataDouble)
    }

    fun save(directory: Path) {
        val fileStr = directory.resolve("net").toAbsolutePath().toString()
        jep.eval("network.save(\"$fileStr\")")
        Files.write(directory.resolve("config"), dump(config))
    }

    override fun close() {
        jep.eval("network.recycle()")
        jep.eval("del network")
        neuralNetworkCreated.set(false)
    }

    @Serializable
    data class Config(
            val coinNumber: Int,
            val historySize: Int,
            val indicatorCount: Int
    )

    companion object {
        fun init(jep: Jep, config: Config, gpuMemoryFraction: Double = 0.2): NeuralNetwork {
            return NeuralNetwork(jep, config, gpuMemoryFraction, null)
        }

        fun load(jep: Jep, directory: Path, gpuMemoryFraction: Double = 0.2): NeuralNetwork {
            val config: Config = load(Files.readAllBytes(directory.resolve("config")))
            return NeuralNetwork(jep, config, gpuMemoryFraction, directory.resolve("net"))
        }
    }
}

class NeuralTrainer(
        private val jep: Jep,
        private val net: NeuralNetwork
): AutoCloseable {
    init {
        if (neuralTrainerCreated.getAndSet(true)) {
            throw UnsupportedOperationException("Two created neural trainers doesn't support")
        }
        if (!neuralNetworkCreated.get()) {
            throw UnsupportedOperationException("Neural network should be created")
        }

        jep.eval("from cointrader.network import NeuralTrainer")
        jep.eval("trainer = None")
        jep.eval("""
                def create_trainer():
                    global trainer
                    global network
                    trainer = NeuralTrainer(network)
            """.trimIndent())
        jep.eval("""
                def train(current_portfolio, history, future_price_incs, fees):
                    return trainer.train(current_portfolio, history, future_price_incs, fees)
            """.trimIndent())
        jep.invoke("create_trainer")
    }

    fun train(currentPortfolio: PortionsBatch, histories: HistoryBatch, futurePriceIncs: PriceIncsBatch, fees: FeesBatch): Result {
        val resultMatrix = train(currentPortfolio.toMatrix(), histories.toMatrix(), futurePriceIncs.toMatrix(), fees.toMatrix())
        return Result(
                resultMatrix.newPortions.toPortionsBatch(),
                resultMatrix.geometricMeanProfit
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun train(currentPortfolio: Matrix2D, histories: Matrix4D, futurePriceIncs: Matrix2D, fees: Matrix2D): ResultMatrix {
        require(currentPortfolio.n2 == net.config.coinNumber)
        require(histories.n2 == net.config.indicatorCount)
        require(histories.n3 == net.config.coinNumber)
        require(histories.n4 == net.config.historySize)
        require(futurePriceIncs.n2 == net.config.coinNumber)
        require(futurePriceIncs.n1 == currentPortfolio.n1)
        require(futurePriceIncs.n1 == histories.n1)

        val nphistory = NDArray(histories.data, histories.n1, histories.n2, histories.n3, histories.n4)
        val npportfolio = NDArray(currentPortfolio.data, currentPortfolio.n1, currentPortfolio.n2)
        val npFuturePriceIncs = NDArray(futurePriceIncs.data, futurePriceIncs.n1, futurePriceIncs.n2)
        val npFees = NDArray(fees.data, fees.n1, fees.n2)

        val result = jep.invoke("train", npportfolio, nphistory, npFuturePriceIncs, npFees) as Array<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double

        val portionsDataDouble = newPortions.data.map { it.toDouble() }.toDoubleArray()

        return ResultMatrix(
                Matrix2D(newPortions.dimensions[0], newPortions.dimensions[1], portionsDataDouble),
                geometricMeanProfit
        )
    }

    override fun close() {
        jep.eval("del trainer")
        neuralTrainerCreated.set(false)
    }

    data class ResultMatrix(val newPortions: Matrix2D, val geometricMeanProfit: Double)
    data class Result(val newPortions: PortionsBatch, val geometricMeanProfit: Double)
}

@JvmName("HistoryBatch_toMatrix")
fun HistoryBatch.toMatrix(): Matrix4D {
    val batchSize = size
    val historySize = first().size
    val coinsSize = first().first().coinIndexToCandle.size
    val indicatorSize = 3
    fun value(b: Int, c: Int, h: Int, i: Int) = this[b][h].coinIndexToCandle[c].indicator(i)
    return Matrix4D(batchSize, historySize, coinsSize, indicatorSize, ::value)
}

@JvmName("toMatrix2D")
fun List<List<Double>>.toMatrix(): Matrix2D {
    val batchSize = size
    val portfolioSize = first().size
    fun value(b: Int, c: Int) = this[b][c]
    return Matrix2D(batchSize, portfolioSize, ::value)
}

fun Matrix2D.toPortionsBatch(): PortionsBatch {
    val portfolios = ArrayList<Portions>(n1)
    (0 until n1).forEach { b ->
        val portfolio = ArrayList<Double>(n2)
        (0 until n2).forEach { c ->
            portfolio.add(this[b, c])
        }
    }
    return portfolios
}

fun Portions.toMatrix(): Matrix2D = listOf(this).toMatrix()
fun History.toMatrix(): Matrix4D = listOf(this).toMatrix()
fun Matrix2D.toPortions(): Portions = toPortionsBatch().first()