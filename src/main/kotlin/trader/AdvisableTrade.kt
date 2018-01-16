package trader

import adviser.CoinPortions
import adviser.TradeAdviser
import exchange.*
import kotlinx.coroutines.experimental.async
import org.slf4j.Logger
import util.lang.round
import util.lang.roundValues
import util.lang.truncatedTo
import util.lang.zipValues
import util.math.portions
import util.math.sum
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

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

        val previousCandles = previousCandles(markets, time)
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

        // todo move from here
        val newCapitals = capitals(prices)
        val newPortions = capitals.portions(operationScale)
        val totalCapital = newCapitals.values.sum()
        listener.afterTrade(totalCapital, newCapitals, newPortions)
    }

    private suspend fun previousCandles(markets: Map<String, MainMarket>, time: Instant): CoinToCandles {
        return markets.mapValues {
            async {
                it.value.candlesBefore(time, historyCount, period)
            }
        }.mapValues {
            it.value.await()
        }.withMainCoin(historyCount)
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
        fun afterTrade(totalCapital: BigDecimal, capitals: Map<String, BigDecimal>, portions: CoinPortions) = Unit
    }

    class EmptyListener : Listener

    class LogListener(private val log: Logger) : Listener {
        override fun afterGetCandles(previousCandles: CoinToCandles) {
            val firstAndLastCandles = previousCandles.mapValues {
                Pair(it.value.first(), it.value.last())
            }.entries.joinToString("\n") {
                val coin = it.key
                val firstCandle = Candle(
                        it.value.first.open.round(6),
                        it.value.first.close.round(6),
                        it.value.first.high.round(6),
                        it.value.first.low.round(6)
                )
                val secondCandle = Candle(
                        it.value.second.open.round(6),
                        it.value.second.close.round(6),
                        it.value.second.high.round(6),
                        it.value.second.low.round(6)
                )
                "$coin   first $firstCandle   last $secondCandle"
            }
            log.debug("afterGetCandles\n$firstAndLastCandles\n")
        }

        override fun afterGetAmounts(amounts: Map<String, BigDecimal>) {
            val amountsR = amounts.roundValues(6)
            log.debug("afterGetAmounts    $amountsR")
        }

        override fun afterGetCapitals(capitals: Map<String, BigDecimal>, portions: CoinPortions) {
            val capitalsR = capitals.roundValues(6)
            val portionsR = portions.roundValues(2)
            log.debug("afterGetCapitals\ncapitals $capitalsR\nportions $portionsR")
        }

        override fun afterGetBestPortions(bestPortions: CoinPortions) {
            val bestPortionsR = bestPortions.roundValues(2)
            log.debug("afterGetBestPortions\nbestPortions=$bestPortionsR")
        }

        override fun afterBuyMainCoin(sellingCoin: String, mainAmount: BigDecimal) {
            val mainAmountR = mainAmount.round(6)
            log.debug("afterBuyMainCoin   sellingCoin $sellingCoin   mainAmount $mainAmountR")
        }

        override fun afterSellMainCoin(buyingCoin: String, mainAmount: BigDecimal) {
            val mainAmountR = mainAmount.round(6)
            log.debug("afterSellMainCoin   buyingCoin $buyingCoin   mainAmount $mainAmountR")
        }

        override fun afterTrade(totalCapital: BigDecimal, capitals: Map<String, BigDecimal>, portions: CoinPortions) {
            val totalCapitalR = totalCapital.round(6)
            val capitalsR = capitals.roundValues(6)
            val portionsR = portions.roundValues(2)
            log.info("afterTrade   totalCapital $totalCapitalR\ncapitals $capitalsR\nportions $portionsR")
        }
    }
}