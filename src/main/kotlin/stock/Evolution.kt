package stock

import io.jenetics.*
import io.jenetics.engine.Codec
import io.jenetics.engine.Engine
import matrix.Matrix
import net.Network
import read.readCoinbaseByMin


fun main(args: Array<String>) {
    val prices = readCoinbaseByMin()
    val normalizedPrices = normalizePrices(pricesToUpDown(prices))
    val neurons = netNeurons()

    fun initial(): Genotype<DoubleGene> = Genotype.of(
            DoubleChromosome.of(-1.0, 1.0, (neurons.input + 1) * neurons.layer1),
            DoubleChromosome.of(-1.0, 1.0, (neurons.layer1 + 1) * neurons.layer2),
            DoubleChromosome.of(-1.0, 1.0, (neurons.layer2 + 1) * neurons.output)
    )

    fun convert(gt: Genotype<DoubleGene>): Network {
        val weights0 = (gt.getChromosome(0) as DoubleChromosome).toArray()
        val weights1 = (gt.getChromosome(1) as DoubleChromosome).toArray()
        val weights2 = (gt.getChromosome(2) as DoubleChromosome).toArray()
        return Network(
                neurons,
                Network.Weights(
                        Matrix(neurons.input + 1, neurons.layer1, weights0),
                        Matrix(neurons.layer1 + 1, neurons.layer2, weights1),
                        Matrix(neurons.layer2 + 1, neurons.output, weights2)
                )
        )
    }

    fun fitness(net: Network) = testNet(net, normalizedPrices, prices)

    val codec = Codec.of(::initial, ::convert)

    val engine = Engine
            .builder(::fitness, codec)
            .alterers(
                    Mutator(0.3),
                    UniformCrossover()
            )
            .build()

    engine.stream().forEach {
        println(it.bestFitness)
    }
}