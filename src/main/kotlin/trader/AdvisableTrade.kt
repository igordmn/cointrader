package trader

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
        val markets: Map<String, MainCoinMarket> = altCoins.associate { it to mainCoinMarket(it) }
        val previousCandles: CoinToCandles = markets.mapValues { it.value.candlesBefore(tradeTime) }.withMainCoin()
        val prices: Map<String, BigDecimal> = allCoins.associate { it to previousCandles[it]!!.last().closePrice }
        val capitals: Map<String, BigDecimal> = capitals(prices)
        val portions = capitals.portions(operationScale)

        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)

        val currentCoin = maxCoin(portions)
        val buyCoin = maxCoin(bestPortions)

        if (currentCoin != buyCoin) {
            val currentCapital = capitals[currentCoin]!!
            if (currentCoin != mainCoin) {
                val market = markets[currentCoin]!!
                val price = prices[currentCoin]!!
                market.sell(currentCapital, price)
            }

            if (buyCoin != mainCoin) {
                val market = markets[buyCoin]!!
                val price = prices[buyCoin]!!
                market.buy(currentCapital, price)
            }
        }
    }

    private fun mainCoinMarket(coin: String): MainCoinMarket {
        val market: Market? = markets.of(mainCoin, coin)
        val reversedMarket: Market? = markets.of(coin, mainCoin)
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
        return MainCoinMarket(finalMarket, isReversed)
    }

    private fun CoinToCandles.withMainCoin(): CoinToCandles = this + Pair(mainCoin, mainCoinCandles())

    private suspend fun capitals(prices: Map<String, BigDecimal>): Map<String, BigDecimal> {
        fun coinCapital(amount: BigDecimal, price: BigDecimal) = amount * price
        val amounts: Map<String, BigDecimal> = portfolio.amounts()
        return amounts.zipValues(prices, ::coinCapital)
    }

    private fun mainCoinCandles(): List<Candle> = List(historyCount) { mainCoinCandle() }

    private fun mainCoinCandle() = Candle(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    )

    private fun maxCoin(portions: Map<String, BigDecimal>): String = portions.maxBy { it.value }!!.key
    private fun startOfTradePeriod(time: Instant) = time.truncatedTo(period)

    private inner class MainCoinMarket(private val market: Market, private val isReversed: Boolean) {
        suspend fun candlesBefore(time: Instant): List<Candle> {
            fun Candle.reverse() = Candle(
                    closePrice = BigDecimal.ONE.divide(closePrice, operationScale),
                    openPrice = BigDecimal.ONE.divide(openPrice, operationScale),
                    highPrice = BigDecimal.ONE.divide(highPrice, operationScale),
                    lowPrice = BigDecimal.ONE.divide(lowPrice, operationScale)
            )

            val originalCandles = market.history.candlesBefore(time, historyCount, period)
            return if (isReversed) originalCandles else originalCandles.map(Candle::reverse)
        }

        suspend fun buy(mainCoinAmount: BigDecimal, altCoinPrice: BigDecimal) {
            if (isReversed) {
                market.orders.sell(mainCoinAmount)
            } else {
                market.orders.buy(mainCoinAmount * altCoinPrice)
            }
        }

        suspend fun sell(mainCoinAmount: BigDecimal, altCoinPrice: BigDecimal) {
            if (isReversed) {
                market.orders.buy(mainCoinAmount)
            } else {
                market.orders.sell(mainCoinAmount * altCoinPrice)
            }
        }
    }
}