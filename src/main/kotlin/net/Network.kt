package net

import matrix.Matrix
import matrix.addIdentityCol
import matrix.randomMatrix
import matrix.reLU

data class Network(
        val neurons: Neurons,
        val weights: Weights
) {
    init {
        require(weights.layer1.rows == neurons.input + 1) // 1 for bias weights
        require(weights.layer1.cols == neurons.layer1)

        require(weights.layer2.rows == neurons.layer1 + 1)
        require(weights.layer2.cols == neurons.layer2)

        require(weights.output.rows == neurons.layer2 + 1)
        require(weights.output.cols == neurons.output)
    }

    data class Neurons(
            val input: Int,
            val layer1: Int,
            val layer2: Int,
            val output: Int
    )

    data class Weights(
            val layer1: Matrix,
            val layer2: Matrix,
            val output: Matrix
    )
}

fun randomWeights(neurons: Network.Neurons) = Network.Weights(
        randomMatrix(neurons.input + 1, neurons.layer1),
        randomMatrix(neurons.layer1 + 1, neurons.layer2),
        randomMatrix(neurons.layer2 + 1, neurons.output)
)

fun output(net: Network, input: Matrix): Matrix {
    require(input.cols == net.neurons.input)

    val y1 = reLU(addIdentityCol(input) * net.weights.layer1) // addIdentityCol for bias
    val y2 = reLU(addIdentityCol(y1) * net.weights.layer2)
    return reLU(addIdentityCol(y2) * net.weights.output)
}