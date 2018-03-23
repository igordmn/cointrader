package com.dmi.cointrader.app.neural

import com.dmi.cointrader.app.archive.*
import com.dmi.cointrader.app.trade.TradeConfig
import com.dmi.util.io.ResourceContext
import com.dmi.util.lang.unsupportedOperation
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
    return NeuralNetwork.init(jep, NeuralNetwork.Config(config.assets.all.size, config.historySize), gpuMemoryFraction = 0.5).use()
}

fun ResourceContext.networkTrainer(jep: Jep, net: NeuralNetwork, fee: Double): NeuralTrainer {
    return NeuralTrainer(jep, net, fee).use()
}

fun jep() = Jep(false, Paths.get("python/src").toAbsolutePath().toString()).apply {
    try {
        eval("import sys")
        eval("sys.argv=[''] ")
    } catch (e: Throwable) {
        close()
        throw e
    }
}

private fun Matrix2D.toJep(): NDArray<DoubleArray> = NDArray(data, n1, n2)
private fun Matrix4D.toJep(): NDArray<DoubleArray> = NDArray(data, n1, n2, n3, n4)

private fun NDArray<FloatArray>.toMatrix2D() = Matrix2D(dimensions[0], dimensions[1], data.map { it.toDouble() }.toDoubleArray())

typealias Portions = List<Double>
typealias PortionsBatch = List<Portions>

private const val historyIndicatorNumber = 2

private fun Spread.historyIndicator(index: Int) = when (index) {
    0 -> ask
    1 -> bid
    else -> unsupportedOperation()
}

class NeuralNetwork private constructor(
        private val jep: Jep,
        val config: Config,
        gpuMemoryFraction: Double,
        savedFile: Path?
): AutoCloseable {
    init {
        if (neuralNetworkCreated.getAndSet(true)) {
            unsupportedOperation("Two created neural networks doesn't support")
        }

        jep.eval("from cointrader.network import NeuralNetwork")
        jep.eval("network = None")
        jep.eval("""
                def create_network(alt_asset_number, history_size, gpu_memory_fraction, saved_file):
                    global network
                    network = NeuralNetwork(alt_asset_number, history_size, $historyIndicatorNumber, gpu_memory_fraction, saved_file)
            """.trimIndent())
        jep.eval("""
                def best_portfolio(current_portfolio, history):
                    return network.best_portfolio(current_portfolio, history)
            """.trimIndent())
        jep.invoke(
                "create_network",
                config.altAssetNumber, config.historySize,
                gpuMemoryFraction, savedFile?.toAbsolutePath()?.toString()
        )
    }

    fun bestPortfolio(currentPortfolio: Portions, history: History): Portions {
        return bestPortfolio(currentPortfolio.toMatrix(), history.toMatrix()).toPortions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun bestPortfolio(currentPortfolio: Matrix2D, histories: Matrix4D): Matrix2D {
        require(currentPortfolio.n2 == config.altAssetNumber)
        require(histories.n1 == currentPortfolio.n1)
        require(histories.n2 == historyIndicatorNumber)
        require(histories.n3 == config.altAssetNumber)
        require(histories.n4 == config.historySize)

        val result = jep.invoke("best_portfolio", currentPortfolio.toJep(), histories.toJep()) as NDArray<FloatArray>
        return result.toMatrix2D()
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
            val altAssetNumber: Int,
            val historySize: Int
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
        private val net: NeuralNetwork,
        fee: Double
): AutoCloseable {
    init {
        if (neuralTrainerCreated.getAndSet(true)) {
            unsupportedOperation("Two created neural trainers doesn't support")
        }
        if (!neuralNetworkCreated.get()) {
            unsupportedOperation("Neural network should be created")
        }

        jep.eval("from cointrader.network import NeuralTrainer")
        jep.eval("trainer = None")
        jep.eval("""
                def create_trainer():
                    global trainer
                    global network
                    trainer = NeuralTrainer(network, $fee)
            """.trimIndent())
        jep.eval("""
                def train(current_portfolio, history, asks, bids):
                    return trainer.train(current_portfolio, history, asks, bids)
            """.trimIndent())
        jep.invoke("create_trainer")
    }

    fun train(currentPortfolio: PortionsBatch, histories: HistoryBatch, spreads: SpreadsBatch): Result {
        val resultMatrix = train(
                currentPortfolio.toMatrix(), histories.toMatrix(),
                spreads.toMatrix(Spread::ask), spreads.toMatrix(Spread::bid)
        )
        return Result(
                resultMatrix.newPortions.toPortionsBatch(),
                resultMatrix.geometricMeanProfit
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun train(currentPortfolio: Matrix2D, histories: Matrix4D, asks: Matrix2D, bids: Matrix2D): ResultMatrix {
        require(currentPortfolio.n2 == net.config.altAssetNumber)
        require(histories.n1 == currentPortfolio.n1)
        require(histories.n2 == historyIndicatorNumber)
        require(histories.n3 == net.config.altAssetNumber)
        require(histories.n4 == net.config.historySize)
        require(asks.n1 == currentPortfolio.n1)
        require(asks.n2 == net.config.altAssetNumber)
        require(bids.n1 == currentPortfolio.n1)
        require(bids.n2 == net.config.altAssetNumber)

        val result = jep.invoke("train", currentPortfolio.toJep(), histories.toJep(), asks.toJep(), bids.toJep()) as Array<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double
        return ResultMatrix(newPortions.toMatrix2D(), geometricMeanProfit)
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
    val coinsSize = first().first().size
    fun value(b: Int, c: Int, h: Int, i: Int) = this[b][h][c].historyIndicator(i)
    return Matrix4D(batchSize, historySize, coinsSize, historyIndicatorNumber, ::value)
}

@JvmName("toMatrix2D")
fun List<List<Double>>.toMatrix(): Matrix2D = toMatrix({ it })

@JvmName("toMatrix2D")
fun <T> List<List<T>>.toMatrix(value: (T) -> Double): Matrix2D {
    val batchSize = size
    val portfolioSize = first().size
    fun value(b: Int, c: Int) = value(this[b][c])
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