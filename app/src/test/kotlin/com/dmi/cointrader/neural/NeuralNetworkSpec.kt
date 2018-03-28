package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.archive.Spreads
import com.dmi.util.io.resourceContext
import com.dmi.util.test.Spec
import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe

class NeuralNetworkSpec: Spec({
    "train" {
        resourceContext {
            val jep = jep()
            val altAssetNumber = 2
            val historySize = 3
            val batchSize = 4
            val network = NeuralNetwork.init(jep, NeuralNetwork.Config(altAssetNumber, historySize), gpuMemoryFraction = 0.1)
            val trainer = NeuralTrainer(jep, network, 0.01)

            fun spread() = Spread(1.0, 1.0)
            fun spreads(): Spreads = listOf(spread(), spread())
            fun history() = TradedHistory(listOf(spreads(), spreads(), spreads()), spreads())

            val portfolio = listOf(listOf(1.0, 0.0), listOf(1.0, 0.0), listOf(1.0, 0.0), listOf(1.0, 0.0))
            val histories = listOf(history(), history(), history(), history())
            val (newPortions, geometricMeanProfit) = trainer.train(portfolio, histories)

            newPortions.size shouldBe batchSize
            newPortions.forEach {
                it.size shouldBe 1 + altAssetNumber
            }
            geometricMeanProfit should beGreaterThan(0.0)
        }
    }
})