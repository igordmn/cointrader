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
import java.util.logging.Logger

class AdvisableTrade(
        private val mainCoin: String,
        private val altCoins: List<String>,
        private val period: Duration,
        private val historyCount: Int,
        private val adviser: TradeAdviser,
        private val markets: Markets,
        private val portfolio: Portfolio,
        private val operationScale: Int,
        private val listener: Listener
) : Trade {
    override suspend fun perform(time: Instant) {
        require(time.truncatedTo(period) == time)

        fun withMainMarket(coin: String) = coin to findMainMarket(coin)

        val markets = altCoins.associate(::withMainMarket)

        val previousCandles = markets.mapValues {
            it.value.candlesBefore(time, historyCount, period)
        }.withMainCoin(historyCount)
        listener.afterGetCandles(previousCandles)

        val prices = previousCandles.mapValues {
            it.value.last().close
        }

        val brokers = markets.mapValues {
            val price = prices[it.key]!!
            it.value.broker(price)
        }

        val capitals = capitals(prices)
        val portions = capitals.portions(operationScale)
        listener.afterGetCapitals(capitals, portions)

        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)
        listener.afterGetBestPortions(bestPortions)

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
                listener.afterBuyMainCoin(currentCoin, amount)
            }

            if (buyCoin != mainCoin) {
                val broker = brokers[buyCoin]!!
                broker.sell(amount)
                listener.afterSellMainCoin(buyCoin, amount)
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
        listener.afterGetAmounts(amounts)

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

    interface Listener {
        fun afterGetCandles(previousCandles: CoinToCandles) = Unit
        fun afterGetAmounts(amounts: Map<String, BigDecimal>) = Unit
        fun afterGetCapitals(capitals: Map<String, BigDecimal>, portions: CoinPortions) = Unit
        fun afterGetBestPortions(bestPortions: CoinPortions) = Unit
        fun afterBuyMainCoin(sellingCoin: String, mainAmount: BigDecimal) = Unit
        fun afterSellMainCoin(buyingCoin: String, mainAmount: BigDecimal) = Unit
    }

    class EmptyListener: Listener

    class LogListener(private val log: Logger) : Listener {
        override fun afterGetCandles(previousCandles: CoinToCandles) {
            val firstAndLastCandles = previousCandles.mapValues {
                Pair(it.value.first(), it.value.last())
            }.entries.joinToString("\n") {
                "${it.key}   first ${it.value.first}   last ${it.value.second}"
            }
            log.info("afterGetCandles\n$firstAndLastCandles\n")
        }

        override fun afterGetAmounts(amounts: Map<String, BigDecimal>) {
            log.info("afterGetAmounts    $amounts")
        }

        override fun afterGetCapitals(capitals: Map<String, BigDecimal>, portions: CoinPortions) {
            log.info("afterGetCapitals\ncapitals $capitals\nportions $portions")
        }

        override fun afterGetBestPortions(bestPortions: CoinPortions) {
            log.info("afterGetBestPortions\nbestPortions=$bestPortions")
        }

        override fun afterBuyMainCoin(sellingCoin: String, mainAmount: BigDecimal) {
            log.info("afterBuyMainCoin   sellingCoin $sellingCoin   mainAmount $mainAmount")
        }

        override fun afterSellMainCoin(buyingCoin: String, mainAmount: BigDecimal) {
            log.info("afterSellMainCoin   buyingCoin $buyingCoin   mainAmount $mainAmount")
        }
    }
}