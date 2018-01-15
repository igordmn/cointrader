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
        private val time: ExchangeTime,
        private val adviser: TradeAdviser,
        private val markets: Markets,
        private val portfolio: Portfolio,
        private val operationScale: Int
) : Trade {
    override suspend fun perform() {
        fun withMainMarket(coin: String) = coin to findMainMarket(coin)

        val markets = altCoins.associate(::withMainMarket)

        val tradeStart = time.current().truncatedTo(period)
        val previousCandles = markets.mapValues {
            it.value.candlesBefore(tradeStart, historyCount, period)
        }.withMainCoin(historyCount)

        val prices = previousCandles.mapValues {
            it.value.last().closePrice
        }
        val brokers = markets.mapValues {
            val price = prices[it.key]!!
            it.value.broker(price)
        }

        val capitals = capitals(prices)
        val portions = capitals.portions(operationScale)

        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)

        rebalance(capitals, portions, bestPortions, brokers)
    }

    private suspend fun rebalance(
            capitals: Map<String, BigDecimal>,
            portions: CoinPortions,
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

    private fun findMainMarket(coin: String): MainMarket {
        val market: Market? = markets.of(coin, mainCoin)
        val reversedMarket: Market? = markets.of(mainCoin, coin)

        require(market != null || reversedMarket != null) { "Market $mainCoin -> $coin doesn't exist" }

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
        fun coinCapital(price: BigDecimal, amount: BigDecimal) = amount * price
        val amounts = HashMap(portfolio.amounts())
        for (key in prices.keys) {
            if (!amounts.containsKey(key)) {
                amounts[key] = BigDecimal.ZERO
            }
        }
        return prices.zipValues(amounts, ::coinCapital)
    }

    private fun maxCoin(portions: Map<String, BigDecimal>): String = portions.maxBy { it.value }!!.key

    private class MainMarket(
            private val original: Market,
            private val isReversed: Boolean,
            private val operationScale: Int
    ) {
        suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
            return history().candlesBefore(time, count, period)
        }

        /**
         * History in main coin prices
         */
        private fun history(): MarketHistory {
            return if (isReversed) original.history else ReversedMarketHistory(original.history, operationScale)
        }

        /**
         * Amount of buy/sell main coins
         */
        fun broker(price: BigDecimal): MarketBroker {
            return if (isReversed) ReversedMarketBroker(original.broker, price, operationScale) else original.broker
        }
    }
}