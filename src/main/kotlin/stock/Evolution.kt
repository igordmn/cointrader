package stock

import io.jenetics.*
import io.jenetics.engine.Codec
import io.jenetics.engine.Engine
import matrix.Matrix
import net.Network
import read.readCoinbaseByMin

private val trainCoeff = 0.95
fun main(args: Array<String>) {
    val allPrices = readCoinbaseByMin()
    val (trainPrices, testPrices) = split(allPrices, trainCoeff)
    val normalizedTrainPrices = normalizePrices(pricesToUpDown(trainPrices))
    val normalizedTestPrices = normalizePrices(pricesToUpDown(testPrices))
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

    fun fitness(net: Network) = testNet(net, normalizedTrainPrices, trainPrices)

    val codec = Codec.of(::initial, ::convert)

    val engine = Engine
            .builder(::fitness, codec)
            .alterers(
                    UniformCrossover(),
                    Mutator(0.55)
            )
            .build()

    var i = 1

    engine.stream().forEach {

        if (i % 20 == 0) {
            val genotype = it.bestPhenotype.genotype
            val net = convert(genotype)
            val testResult = testOnAllNet(net, normalizedTestPrices, testPrices)

            val trainBestRes = it.bestFitness
            println("$trainBestRes \t\t $testResult")
        } else {
            val trainBestRes = it.bestFitness
            println("$trainBestRes")
        }

        i++
    }
}

private fun split(list: List<Double>, coeff: Double): Pair<List<Double>, List<Double>> {
    val split = Math.round(list.size * coeff).toInt()
    val list1 = list.subList(0, split)
    val list2 = list.subList(split, list.size)
    return Pair(list1, list2)
}