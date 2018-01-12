package trader

import adviser.TradeAdviser
import exchange.*
import util.lang.truncatedTo
import util.lang.zipValues
import util.math.portions
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

private const val PORTION_SCALE = 32  // number of digits after dot in portion

class Trader(
        private val mainCoin: String,
        private val altCoins: List<String>,
        private val period: Duration,
        private val historyCount: Int,
        private val adviser: TradeAdviser,
        private val exchange: Exchange
) {
    suspend fun trade() {
        fun coinCapital(amount: BigDecimal, price: BigDecimal) = amount * price

        val currentTime = exchange.currentTime()
        val tradeTime = startOfTradePeriod(currentTime)

        val altMarkets: Map<String, Market> = altCoins.associate(this::coinAndMarket)
        val previousCandles: CoinToCandles = altMarkets.mapValues { it.value.candlesBefore(tradeTime) }.withMainCoin()

        val portfolio = exchange.portfolio()
        val allCoins = listOf(mainCoin) + altCoins
        val amounts: Map<String, BigDecimal> = allCoins.associate { it to portfolio.amount(it) }
        val prices: Map<String, BigDecimal> = allCoins.associate { it to previousCandles[it]!!.last().closePrice }
        val capitals: Map<String, BigDecimal> = amounts.zipValues(prices, ::coinCapital)
        val portions = capitals.portions(PORTION_SCALE)

        val bestPortions = adviser.bestPortfolioPortions(portions, previousCandles)

        val currentCoin = maxCoin(portions)
        val buyCoin = maxCoin(bestPortions)

        if (currentCoin != buyCoin) {
            if (currentCoin != mainCoin) {
                val market = altMarkets[currentCoin]!!
                val amount = amounts[currentCoin]!!
                market.sell(amount)
            }

            if (buyCoin != mainCoin) {
                val market = altMarkets[currentCoin]!!
                val amount = amounts[currentCoin]!!
                market.buy(amount)
            }
        }
    }

    private fun CoinToCandles.withMainCoin(): CoinToCandles = this + Pair(mainCoin, mainCoinCandles())
    private fun mainCoinCandles(): List<Candle> = List(historyCount) { mainCoinCandle() }
    private fun mainCoinCandle() = Candle(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    )

    private fun maxCoin(portions: Map<String, BigDecimal>): String = portions.maxBy { it.value }!!.key
    private fun coinAndMarket(coin: String) = coin to exchange.marketFor(mainCoin, coin)
    private suspend fun Market.candlesBefore(time: Instant) = history().candlesBefore(time, historyCount, period)
    private fun startOfTradePeriod(time: Instant) = time.truncatedTo(period)
}