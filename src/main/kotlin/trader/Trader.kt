package trader

import adviser.TradeAdviser
import exchange.*
import util.lang.truncatedTo
import util.math.Portions
import util.math.portions
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

private val PORTION_SCALE = 8   // number of digits after dot in portion

class Trader(
        private val mainCoin: String,
        private val altCoins: List<String>,
        private val period: Duration,
        private val historyCount: Int,
        private val adviser: TradeAdviser,
        private val exchange: Exchange
) {
    suspend fun trade() {
        val coinToMarket: Map<String, Market> = altCoins.associate(this::coinAndMarket)

        val currentTime = exchange.serverTime()

        val tradeTime = startOfTradePeriod(currentTime)

        val previousCandles: CoinToCandles = coinToMarket.mapValues { it.value.candlesBefore(tradeTime) }

        val portfolio = exchange.portfolio()
        val portions = portfolio.portions(previousCandles)
        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)
    }

    private fun coinAndMarket(coin: String) = coin to exchange.marketFor(mainCoin, coin)
    private suspend fun Market.candlesBefore(time: Instant) = history().candlesBefore(time, historyCount, period)
    private fun startOfTradePeriod(time: Instant) = time.truncatedTo(period)

    private suspend fun Portfolio.portions(coinToCandles: CoinToCandles): Portions {
        fun lastCoinPrice(coin: String) = coinToCandles[coin]!!.last().closePrice
        fun coinCapital(price: BigDecimal, amount: BigDecimal) = price * amount

        val coinPrices: List<BigDecimal> = altCoins.map(::lastCoinPrice)
        val coinAmounts: List<BigDecimal> = altCoins.map { amount(it) }
        val coinCapitals = coinPrices.zip(coinAmounts, ::coinCapital)
        return coinCapitals.portions(PORTION_SCALE)
    }

}