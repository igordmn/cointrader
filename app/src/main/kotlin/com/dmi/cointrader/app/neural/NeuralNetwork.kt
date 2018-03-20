package com.dmi.cointrader.app.neural

import com.dmi.cointrader.app.archive.History
import com.dmi.cointrader.app.archive.HistoryBatch
import com.dmi.cointrader.app.trade.TradeConfig
import com.dmi.cointrader.app.train.TrainConfig
import com.dmi.util.io.ResourceContext
import com.dmi.util.math.DoubleMatrix2D
import com.dmi.util.math.DoubleMatrix4D
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

suspend fun ResourceContext.trainedNetwork(): NeuralNetwork {
    val jep = jep().use()
    return NeuralNetwork.load(jep, Paths.get("data/network"), gpuMemoryFraction = 0.2).use()
}

fun ResourceContext.trainingNetwork(jep: Jep, config: TradeConfig): NeuralNetwork {
    return NeuralNetwork.init(jep, NeuralNetwork.Config(config.assets.all.size, config.historySize, 3), gpuMemoryFraction = 0.5).use()
}

fun ResourceContext.networkTrainer(jep: Jep, net: NeuralNetwork, config: TrainConfig): NeuralTrainer {
    return NeuralTrainer(jep, net, NeuralTrainer.Config(config.fee))
}

typealias Portions = List<Double>
typealias PortionsBatch = List<Portions>
data class PriceIncs(private val list: List<Double>): List<Double> by list
typealias PriceIncsBatch = List<PriceIncs>

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
                def best_portfolio(previous_w, history):
                    return network.best_portfolio(previous_w, history)
            """.trimIndent())
        jep.invoke("create_network", config.coinNumber, config.historySize, config.indicatorCount, gpuMemoryFraction, loadFile?.toAbsolutePath()?.toString())
    }

    fun bestPortfolio(currentPortions: Portions, history: History): Portions {
        return bestPortfolio(currentPortions.toMatrix(), history.toMatrix()).toPortions()
    }

    @Suppress("UNCHECKED_CAST")
    fun bestPortfolio(currentPortions: DoubleMatrix2D, histories: DoubleMatrix4D): DoubleMatrix2D = synchronized(this) {
        require(currentPortions.n2 == config.coinNumber)
        require(histories.n2 == config.indicatorCount)
        require(histories.n3 == config.coinNumber)
        require(histories.n4 == config.historySize)
        require(currentPortions.n1 == histories.n1)

        val npportfolio = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)
        val nphistory = NDArray(histories.data, histories.n1, histories.n2, histories.n3, histories.n4)

        val result = jep.invoke("best_portfolio", npportfolio, nphistory) as NDArray<FloatArray>

        val dataDouble = result.data.map { it.toDouble() }.toDoubleArray()
        return DoubleMatrix2D(result.dimensions[0], result.dimensions[1], dataDouble)
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

        suspend fun load(jep: Jep, directory: Path, gpuMemoryFraction: Double = 0.2): NeuralNetwork {
            val config: Config = load(Files.readAllBytes(directory.resolve("config")))
            return NeuralNetwork(jep, config, gpuMemoryFraction, directory.resolve("net"))
        }
    }
}

class NeuralTrainer(
        private val jep: Jep,
        private val net: NeuralNetwork,
        config: Config
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
                def create_trainer(fee):
                    global trainer
                    global network
                    trainer = NeuralTrainer(fee, network)
            """.trimIndent())
        jep.eval("""
                def train(previous_w, history, price_incs):
                    return trainer.train(previous_w, history, price_incs)
            """.trimIndent())
        jep.invoke("create_trainer", config.fee)
    }

    fun train(currentPortions: PortionsBatch, histories: HistoryBatch, futurePriceIncs: PriceIncsBatch): Result {
        val resultMatrix = train(currentPortions.toMatrix(), histories.toMatrix(), futurePriceIncs.toMatrix())
        return Result(
                resultMatrix.newPortions.toPortionsBatch(),
                resultMatrix.geometricMeanProfit
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun train(currentPortions: DoubleMatrix2D, histories: DoubleMatrix4D, futurePriceIncs: DoubleMatrix2D): ResultMatrix {
        require(currentPortions.n2 == net.config.coinNumber)
        require(histories.n2 == net.config.indicatorCount)
        require(histories.n3 == net.config.coinNumber)
        require(histories.n4 == net.config.historySize)
        require(futurePriceIncs.n2 == net.config.coinNumber)
        require(futurePriceIncs.n1 == currentPortions.n1)
        require(futurePriceIncs.n1 == histories.n1)

        val nphistory = NDArray(histories.data, histories.n1, histories.n2, histories.n3, histories.n4)
        val npportfolio = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)
        val npPriceIncs = NDArray(futurePriceIncs.data, futurePriceIncs.n1, futurePriceIncs.n2)

        val result = jep.invoke("train", npportfolio, nphistory, npPriceIncs) as Array<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double

        val portionsDataDouble = newPortions.data.map { it.toDouble() }.toDoubleArray()

        return ResultMatrix(
                DoubleMatrix2D(newPortions.dimensions[0], newPortions.dimensions[1], portionsDataDouble),
                geometricMeanProfit
        )
    }

    override fun close() {
        jep.eval("del trainer")
        neuralTrainerCreated.set(false)
    }

    data class Config(
            val fee: Double
    )

    data class ResultMatrix(val newPortions: DoubleMatrix2D, val geometricMeanProfit: Double)
    data class Result(val newPortions: PortionsBatch, val geometricMeanProfit: Double)
}

@JvmName("PortionsBatch_toMatrix")
fun PortionsBatch.toMatrix(): DoubleMatrix2D {
    val batchSize = size
    val portfolioSize = first().size
    fun value(b: Int, c: Int) = this[b][c]
    return DoubleMatrix2D(batchSize, portfolioSize, ::value)
}

@JvmName("HistoryBatch_toMatrix")
fun HistoryBatch.toMatrix(): DoubleMatrix4D {
    val batchSize = size
    val historySize = first().size
    val coinsSize = first().first().coinIndexToCandle.size
    val indicatorSize = 3
    fun value(b: Int, c: Int, h: Int, i: Int) = this[b][h].coinIndexToCandle[c].indicator(i)
    return DoubleMatrix4D(batchSize, historySize, coinsSize, indicatorSize, ::value)
}

@JvmName("PriceIncsBatch_toMatrix")
fun PriceIncsBatch.toMatrix(): DoubleMatrix2D {
    val batchSize = size
    val portfolioSize = first().size
    fun value(b: Int, c: Int) = this[b][c]
    return DoubleMatrix2D(batchSize, portfolioSize, ::value)
}

fun DoubleMatrix2D.toPortionsBatch(): PortionsBatch {
    val portfolios = ArrayList<Portions>(n1)
    (0 until n1).forEach { b ->
        val portfolio = ArrayList<Double>(n2)
        (0 until n2).forEach { c ->
            portfolio.add(this[b, c])
        }
    }
    return portfolios
}

fun Portions.toMatrix(): DoubleMatrix2D = listOf(this).toMatrix()
fun History.toMatrix(): DoubleMatrix4D = listOf(this).toMatrix()
fun DoubleMatrix2D.toPortions(): Portions = toPortionsBatch().first()