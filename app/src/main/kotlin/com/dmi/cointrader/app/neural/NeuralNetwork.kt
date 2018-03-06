package com.dmi.cointrader.app.neural

import com.dmi.util.math.DoubleMatrix2D
import com.dmi.util.math.DoubleMatrix4D
import jep.Jep
import jep.NDArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.cbor.CBOR.Companion.load
import org.deeplearning4j.rl4j.network.NeuralNet
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

private val neuralNetworkCreated = AtomicBoolean(false)
private val neuralTrainerCreated = AtomicBoolean(false)

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
                def create_network(coin_count, history_count, indicator_count, gpu_memory_fraction, load_path):
                    global network
                    network = NeuralNetwork(coin_count, history_count, indicator_count, gpu_memory_fraction, load_path)
            """.trimIndent())
        jep.eval("""
                def best_portfolio(history, previous_w):
                    return network.best_portfolio(history, previous_w)
            """.trimIndent())
        jep.invoke("create_network", config.coinCount, config.historyCount, config.indicatorCount, gpuMemoryFraction, loadFile?.toAbsolutePath()?.toString())
    }

    @Suppress("UNCHECKED_CAST")
    fun bestPortfolio(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D = synchronized(this) {
        require(currentPortions.n2 == config.coinCount)
        require(history.n2 == config.indicatorCount)
        require(history.n3 == config.coinCount)
        require(history.n4 == config.historyCount)
        require(currentPortions.n1 == history.n1)

        val nphistory = NDArray(history.data, history.n1, history.n2, history.n3, history.n4)
        val npportions = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)

        val result = jep.invoke("best_portfolio", nphistory, npportions) as NDArray<FloatArray>

        val dataDouble = result.data.map { it.toDouble() }.toDoubleArray()
        return DoubleMatrix2D(result.dimensions[0], result.dimensions[1], dataDouble)
    }

    suspend fun save(directory: Path) {
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
            val coinCount: Int,
            val historyCount: Int,
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

    @Suppress("UNCHECKED_CAST")
    fun train(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D, priceIncs: DoubleMatrix2D): TrainResult {
        require(currentPortions.n2 == net.config.coinCount)
        require(history.n2 == net.config.indicatorCount)
        require(history.n3 == net.config.coinCount)
        require(history.n4 == net.config.historyCount)
        require(priceIncs.n2 == net.config.coinCount)
        require(priceIncs.n1 == currentPortions.n1)
        require(priceIncs.n1 == history.n1)

        val nphistory = NDArray(history.data, history.n1, history.n2, history.n3, history.n4)
        val npportions = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)
        val npPriceIncs = NDArray(priceIncs.data, priceIncs.n1, priceIncs.n2)

        val result = jep.invoke("train", nphistory, npportions, npPriceIncs) as Array<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double

        val portionsDataDouble = newPortions.data.map { it.toDouble() }.toDoubleArray()

        return TrainResult(
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

    data class TrainResult(val newPortions: DoubleMatrix2D, val geometricMeanProfit: Double)
}