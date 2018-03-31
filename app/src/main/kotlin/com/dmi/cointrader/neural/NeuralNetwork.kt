package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.io.ResourceContext
import com.dmi.util.lang.unsupported
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
    val dir = Paths.get("data/network")
    Files.createDirectories(dir)
    return NeuralNetwork.load(jep, dir, gpuMemoryFraction = 0.2).use()
}

fun ResourceContext.trainingNetwork(jep: Jep, config: TradeConfig): NeuralNetwork {
    return NeuralNetwork.init(jep, NeuralNetwork.Config(config.assets.alts.size, config.historyPeriods.count), gpuMemoryFraction = 0.5).use()
}

fun ResourceContext.networkTrainer(jep: Jep, net: NeuralNetwork, fee: Double): NeuralTrainer {
    return NeuralTrainer(jep, net, fee).use()
}

typealias Portions = List<Double>
typealias PortionsBatch = List<Portions>

fun Portions.withoutMainAsset(): Portions = drop(1)

private const val historyIndicatorNumber = 2

private fun Spread.historyIndicator(index: Int) = when (index) {
    0 -> ask
    1 -> bid
    else -> unsupported()
}

class NeuralNetwork private constructor(
        private val jep: Jep,
        val config: Config,
        gpuMemoryFraction: Double,
        savedFile: Path?
) : AutoCloseable {
    init {
        if (neuralNetworkCreated.getAndSet(true)) {
            unsupported("Two created neural networks doesn't support")
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

    fun bestPortfolio(currentPortfolio: Portions, history: NeuralHistory): Portions {
        return bestPortfolio(currentPortfolio.withoutMainAsset().toNumpy(), history.toNumpy()).toPortions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun bestPortfolio(currentPortfolio: NDDoubleArray, histories: NDDoubleArray): NDFloatArray {
        require(currentPortfolio.dimensions[1] == config.altAssetNumber)
        require(histories.dimensions[0] == currentPortfolio.dimensions[0])
        require(histories.dimensions[1] == config.altAssetNumber)
        require(histories.dimensions[2] == config.historySize)
        require(histories.dimensions[3] == historyIndicatorNumber)

        return jep.invoke("best_portfolio", currentPortfolio, histories) as NDArray<FloatArray>
    }

    fun save(directory: Path) {
        val fileStr = directory.resolve("net").toAbsolutePath().toString().replace("\\", "/")
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
) : AutoCloseable {
    init {
        if (neuralTrainerCreated.getAndSet(true)) {
            unsupported("Two created neural trainers doesn't support")
        }
        if (!neuralNetworkCreated.get()) {
            unsupported("Neural network should be created")
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

    fun train(currentPortfolio: PortionsBatch, history: TradedHistoryBatch): Result {
        val resultMatrix = train(
                currentPortfolio.map { it.withoutMainAsset() }.toNumpy(),
                history.map { it.history }.toNumpy(),
                history.map { it.tradeTimeSpreads }.toNumpy(Spread::ask),
                history.map { it.tradeTimeSpreads }.toNumpy(Spread::bid)
        )
        return Result(
                resultMatrix.newPortions.toPortionsBatch(),
                resultMatrix.geometricMeanProfit
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun train(currentPortfolio: NDDoubleArray, history: NDDoubleArray, asks: NDDoubleArray, bids: NDDoubleArray): ResultMatrix {
        require(currentPortfolio.dimensions[1] == net.config.altAssetNumber)
        require(history.dimensions[0] == currentPortfolio.dimensions[0])
        require(history.dimensions[1] == net.config.altAssetNumber)
        require(history.dimensions[2] == net.config.historySize)
        require(history.dimensions[3] == historyIndicatorNumber)
        require(asks.dimensions[0] == currentPortfolio.dimensions[0])
        require(asks.dimensions[1] == net.config.altAssetNumber)
        require(bids.dimensions[0] == currentPortfolio.dimensions[0])
        require(bids.dimensions[1] == net.config.altAssetNumber)

        val result = jep.invoke("train", currentPortfolio, history, asks, bids) as List<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double
        return ResultMatrix(newPortions, geometricMeanProfit)
    }

    override fun close() {
        jep.eval("del trainer")
        neuralTrainerCreated.set(false)
    }

    private data class ResultMatrix(val newPortions: NDFloatArray, val geometricMeanProfit: Double)
    data class Result(val newPortions: PortionsBatch, val geometricMeanProfit: Double)
}

@JvmName("toNumpy1")
private fun List<NeuralHistory>.toNumpy(): NDDoubleArray {
    val batchCount = size
    val historyCount = first().size
    val assetCount = first().first().size

    val data = DoubleArray(batchCount * assetCount * historyCount* historyIndicatorNumber)
    var k = 0
    for (b in this) {
        for (a in 0 until assetCount) {
            for (h in b) {
                for (i in 0 until historyIndicatorNumber) {
                    data[k++] = h[a].historyIndicator(i)
                }
            }
        }
    }

    return NDArray(data, batchCount, assetCount, historyCount, historyIndicatorNumber)
}

@JvmName("toNumpy2")
private fun List<List<Double>>.toNumpy(): NDDoubleArray = toNumpy({ it })

private fun <T> List<List<T>>.toNumpy(value: (T) -> Double): NDDoubleArray {
    val batchCount = size
    val assetCount = first().size

    val data = DoubleArray(batchCount * assetCount)
    var k = 0
    for (b in this)
        for (a in b)
            data[k++] = value(a)
    return NDArray(data, batchCount, assetCount)
}

private fun NDFloatArray.toPortionsBatch(): PortionsBatch {
    val n = dimensions[0]
    val m = dimensions[1]

    val portfolios = ArrayList<Portions>(n)
    var portfolio = ArrayList<Double>(m)

    for (x in data) {
        portfolio.add(x.toDouble())
        if (portfolio.size == m) {
            portfolios.add(portfolio)
            portfolio = ArrayList(m)
        }
    }

    return portfolios
}

@JvmName("toNumpy3")
private fun Portions.toNumpy(): NDDoubleArray = listOf(this).toNumpy()

@JvmName("toNumpy4")
private fun NeuralHistory.toNumpy(): NDDoubleArray = listOf(this).toNumpy()

private fun NDFloatArray.toPortions(): Portions = toPortionsBatch().first()