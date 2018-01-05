package backtime

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.domain.market.OrderBookEntry
import io.reactivex.schedulers.Schedulers
import net.DoubleMatrix4D
import net.NNAgent
import net.PythonUtils
import java.util.concurrent.Executors

private val COINS = listOf(
        "USDT", "ETH", "XRP", "IOTA", "XVG", "BCH", "TRX", "LTC",
        "NEO", "ADA", "EOS", "QTUM", "ETC", "DASH", "HSR", "VEN",
        "BNB", "POWR", "POE", "MANA", "MCO", "QSP", "STRAT", "WTC",
        "OMG", "SNT", "BTS", "XMR", "LSK", "ZEC", "SALT", "REQ",
        "STORJ", "YOYO", "LINK", "CTR", "BTG", "ENG", "VIB",
        "MTL"
)

private val REVERSED_COINS = listOf("USDT")
private val ALT_NAMES = mapOf(
        "BCH" to "BCC"
)

private val coinNumber = 25
private val windowSize = 160
private val period = CandlestickInterval.FIVE_MINUTES
private val periodMs = 5L * 60 * 1000
private val fee = 0.001

private typealias CoinToCandles = List<List<Candlestick>>

private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

fun main(args: Array<String>) {
    scheduler.scheduleDirect {
        try {
            PythonUtils.startPython()
            main()
        } finally {
            PythonUtils.stopPython()
        }
    }

    while (true) {
        Thread.sleep(10000)
    }
}

private fun main() {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newRestClient()
    val coins = COINS.take(coinNumber)

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val finalName = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$finalName" else "${finalName}BTC"
    }

    fun loadAllCandles(endTime: Long): CoinToCandles {
        val coinToCandles = coins.map {
            client.getCandlestickBars(
                    pair(it),
                    period,
                    windowSize,
                    null,
                    endTime
            )
        }
        coinToCandles.forEach {
            require(it.last().closeTime == endTime)
        }
        return coinToCandles
    }

    fun indicatorByIndex(isReversed: Boolean, index: Int, candle: Candlestick): Double {
        return when (index) {
            0 -> if (isReversed) 1 / candle.close.toDouble() else candle.close.toDouble()
            1 -> if (isReversed) 1 / candle.high.toDouble() else candle.low.toDouble()
            2 -> if (isReversed) 1 / candle.low.toDouble() else candle.high.toDouble()
            else -> throw UnsupportedOperationException()
        }
    }

    fun candlesToMatrix(coinToCandles: CoinToCandles): DoubleMatrix4D {
        return DoubleMatrix4D(1, 3, coinNumber, windowSize) { _, i2, i3, i4 ->
            val coin = coins[i3]
            val isReversed = coin in REVERSED_COINS
            val candle = coinToCandles[i3][i4]
            indicatorByIndex(isReversed, i2, candle)
        }
    }



    val agent = NNAgent(fee, 3, coinNumber, windowSize, "D:/Development/Projects/coin_predict/train_package/netfile")

    val portfolio = DoubleArray(coinNumber + 1)
    portfolio[0] = 0.1

    fun coinPrice(index: Int, coinToCandles: CoinToCandles): Double {
        val coin = coins[index]
        val isReversed = coin in REVERSED_COINS
        return if (isReversed) 1 / coinToCandles[index].last().close.toDouble() else coinToCandles[index].last().close.toDouble()
    }

    fun rebalancePortfolioTo(buyIndex: Int, coinToCandles: CoinToCandles) {
        val currentIndex = portfolio.indexOf(portfolio.max()!!)
        if (currentIndex != buyIndex) {
            if (currentIndex != 0) {
                val currentPrice = coinPrice(currentIndex - 1, coinToCandles)
                portfolio[0] = portfolio[currentIndex] * currentPrice * (1 - fee)
                portfolio[currentIndex] = 0.0
            }

            val capital = portfolio[0] * (1 - fee)
            println("CAPITAL SWITCH $capital")

            if (buyIndex != 0) {
                val buyPrice = coinPrice(buyIndex - 1, coinToCandles)
                portfolio[buyIndex] = portfolio[0] / buyPrice * (1 - fee)
                portfolio[0] = 0.0
            }
        } else {
            if (currentIndex != 0) {
                val currentPrice = coinPrice(currentIndex - 1, coinToCandles)
                val capital = portfolio[currentIndex] * currentPrice * (1 - fee)
                println("CAPITAL $capital")
            } else {
                println("CAPITAL ${portfolio[0]}")
            }
        }
    }

    fun rebalancePortfolio(endTime: Long) {
        val coinToCandles = loadAllCandles(endTime)
        val history = candlesToMatrix(coinToCandles)
        val bestPortfolio = agent.bestPortfolio(history).data
        val buyIndex = bestPortfolio.indexOf(bestPortfolio.max()!!)

        rebalancePortfolioTo(buyIndex, coinToCandles)
    }

    val endTime = (client.serverTime / periodMs) * periodMs - 1
    var time = endTime - 24 * 60 * 60 * 1000

    while (time < endTime) {
        rebalancePortfolio(time)
        time += 5 * 60  * 1000
    }
}