package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.archive.Spreads
import com.dmi.util.io.resourceContext
import com.dmi.util.test.Spec
import com.dmi.util.test.tempDirectory
import com.dmi.util.test.testFileSystem
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import java.nio.file.Files

class NeuralNetworkSpec: Spec({
    "test" {
        resourceContext {
            val altAssetNumber = 2
            val historySize = 3
            val batchSize = 4
            val dir = tempDirectory()

            fun portfolio() = listOf(1.0, 0.0)
            fun spread() = Spread(1.0, 1.0)
            fun spreads(): Spreads = listOf(spread(), spread())
            fun history() = listOf(spreads(), spreads(), spreads())
            fun tradedHistory() = TradedHistory(history(), spreads())

            var bestPortfolio1: Portions? = null

            val jep = jep()

            resourceContext {
                val network = NeuralNetwork.init(jep, NeuralNetwork.Config(altAssetNumber, historySize), gpuMemoryFraction = 0.1).use()
                val trainer = NeuralTrainer(jep, network, 0.01).use()

                val portfolio = listOf(portfolio(), portfolio(), portfolio(), portfolio())
                val history = listOf(tradedHistory(), tradedHistory(), tradedHistory(), tradedHistory())
                val (newPortions, geometricMeanProfit) = trainer.train(portfolio, history)

                newPortions.size shouldBe batchSize
                newPortions.forEach {
                    it.size shouldBe 1 + altAssetNumber
                }
                geometricMeanProfit should beGreaterThan(0.0)

                bestPortfolio1 = network.bestPortfolio(portfolio(), history())
                bestPortfolio1!!.size shouldBe 1 + altAssetNumber

                Files.createDirectories(dir)
                network.save(dir)
            }

            resourceContext {
                val network = NeuralNetwork.load(jep, dir, gpuMemoryFraction = 0.1).use()

                val bestPortfolio2 = network.bestPortfolio(portfolio(), history())
                bestPortfolio2.size shouldBe 1 + altAssetNumber
                bestPortfolio2 shouldBe bestPortfolio1
            }
        }
    }
})