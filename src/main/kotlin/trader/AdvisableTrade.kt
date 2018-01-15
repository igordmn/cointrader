package trader

import adviser.CoinPortions
import adviser.TradeAdviser
import exchange.*
import util.lang.truncatedTo
import util.lang.zipValues
import util.math.portions
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class AdvisableTrade(
        private val mainCoin: String,
        private val altCoins: List<String>,
        private val period: Duration,
        private val historyCount: Int,
        private val adviser: TradeAdviser,
        private val time: ExchangeTime,
        private val markets: Markets,
        private val portfolio: Portfolio,
        private val operationScale: Int
) : Trade {
    override suspend fun perform() {
        val currentTime = time.current()
        val tradeTime = startOfTradePeriod(currentTime)
        val allCoins = listOf(mainCoin) + altCoins

        val markets: Map<String, MainMarket> = altCoins.associate { it to findMarket(mainCoin, it) }
        val previousCandles: CoinToCandles = markets.mapValues { it.value.candlesBefore(tradeTime, historyCount, period) }.withMainCoin(historyCount)
        val prices: Map<String, BigDecimal> = allCoins.associate { it to previousCandles[it]!!.last().closePrice }
        val brokers = markets.mapValues { it.value.broker(prices[it.key]!!) }

        val capitals: Map<String, BigDecimal> = capitals(prices)
        val portions: CoinPortions = capitals.portions(operationScale)

        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)

        rebalance(portions, capitals, bestPortions, brokers)
    }

    private suspend fun rebalance(
            portions: CoinPortions,
            capitals: Map<String, BigDecimal>,
            bestPortions: CoinPortions,
            brokers: Map<String, MarketBroker>
    ) {
        val currentCoin = maxCoin(portions)
        val buyCoin = maxCoin(bestPortions)

        if (currentCoin != buyCoin) {
            val amount = capitals[currentCoin]!!
            if (currentCoin != mainCoin) {
                val broker = brokers[currentCoin]!!
                broker.buy(amount)
            }

            if (buyCoin != mainCoin) {
                val broker = brokers[buyCoin]!!
                broker.sell(amount)
            }
        }
    }

    private fun findMarket(fromCoin: String, toCoin: String): MainMarket {
        val market: Market? = markets.of(fromCoin, toCoin)
        val reversedMarket: Market? = markets.of(toCoin, fromCoin)

        require(market != null || reversedMarket != null) { "Market $fromCoin -> $toCoin doesn't exist" }

        val finalMarket: Market
        val isReversed: Boolean
        if (market != null) {
            finalMarket = market
            isReversed = false
        } else {
            finalMarket = reversedMarket!!
            isReversed = true
        }

        return MainMarket(finalMarket, isReversed, operationScale)
    }

    private fun CoinToCandles.withMainCoin(count: Int): CoinToCandles = this + Pair(mainCoin, mainCoinCandles(count))

    private fun mainCoinCandles(count: Int) = List(count) {
        Candle(
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE
        )
    }

    private suspend fun capitals(prices: Map<String, BigDecimal>): Map<String, BigDecimal> {
        fun coinCapital(amount: BigDecimal, price: BigDecimal) = amount * price
        val amounts: Map<String, BigDecimal> = portfolio.amounts()
        return amounts.zipValues(prices, ::coinCapital)
    }

    private fun maxCoin(portions: Map<String, BigDecimal>): String = portions.maxBy { it.value }!!.key
    private fun startOfTradePeriod(time: Instant) = time.truncatedTo(period)

    private class MainMarket(
            private val original: Market,
            private val isReversed: Boolean,
            private val operationScale: Int
    ) {
        suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
            return history().candlesBefore(time, count, period)
        }

        private fun history(): MarketHistory {
            return if (isReversed) ReversedMarketHistory(original.history, operationScale) else original.history
        }

        fun broker(price: BigDecimal): MarketBroker {
            return if (isReversed) ReversedMarketBroker(original.broker, price, operationScale) else original.broker
        }
    }
}