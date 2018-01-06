package realtime

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.domain.market.OrderBookEntry
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.DoubleMatrix4D
import net.NNAgent
import net.PythonUtils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun totalPrice(amount: Double, orders: List<OrderBookEntry>): Double {
        var amountLeft = amount

        var totalPrice = 0.0
        for (order in orders) {
            val orderAmount = order.qty.toDouble()
            val orderPrice = order.price.toDouble()
            if (amountLeft > orderAmount) {
                totalPrice += orderAmount * orderPrice
                amountLeft -= orderAmount
            } else {
                totalPrice += amountLeft * orderPrice
                return totalPrice
            }
        }

        throw RuntimeException()
    }

    fun totalAmount(bitcoins: Double, orders: List<OrderBookEntry>): Double {
        var totalAmount = 0.0
        var leftBitcoins = bitcoins

        for (order in orders) {
            val orderAmount = order.qty.toDouble()
            val orderPrice = order.price.toDouble()
            val orderBitcoins = orderAmount * orderPrice
            if (leftBitcoins > orderBitcoins) {
                totalAmount += orderAmount
                leftBitcoins -= orderBitcoins
            } else {
                totalAmount += orderAmount * leftBitcoins / orderBitcoins
                return totalAmount
            }
        }

        throw RuntimeException()
    }

    fun reverseBidsAsks(asks: List<OrderBookEntry>, bids: List<OrderBookEntry>): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        // 14000  1   -> 1/14000  1 * 14000
        // 14000  2   -> 1/14000  2 * 14000
        fun List<OrderBookEntry>.reversePrices() = map {
            val newEntry = OrderBookEntry()
            val dblPrice = it.price.toDouble()
            val dblQty = it.qty.toDouble()
            newEntry.price = (1 / dblPrice).toString()
            newEntry.qty = (dblQty * dblPrice).toString()
            newEntry
        }
        return Pair(bids.reversePrices(), asks.reversePrices())
    }

    fun loadAsksBids(coinIndex: Int): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        val coin = coins[coinIndex]
        val isReversed = coin in REVERSED_COINS
        val orderBook = client.getOrderBook(pair(coin), 50)
        return if (isReversed) reverseBidsAsks(orderBook.asks, orderBook.bids) else Pair(orderBook.asks, orderBook.bids)
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

    fun rebalancePortfolioTo(buyIndex: Int) {
        val currentIndex = portfolio.indexOf(portfolio.max()!!)
        if (currentIndex != buyIndex) {
            if (currentIndex != 0) {
                val (_, currentBids) = loadAsksBids(currentIndex - 1)
                portfolio[0] = totalPrice(portfolio[currentIndex], currentBids) * (1 - fee)
                portfolio[currentIndex] = 0.0
            }

            val capital = portfolio[0] * (1 - fee)
            println("CAPITAL SWITCH $capital")

            if (buyIndex != 0) {
                val (buyAsks, _) = loadAsksBids(buyIndex - 1)
                portfolio[buyIndex] = totalAmount(portfolio[0], buyAsks) * (1 - fee)
                portfolio[0] = 0.0
            }
        } else {
            if (currentIndex != 0) {
                val (_, currentBids) = loadAsksBids(currentIndex - 1)
                val capital = totalPrice(portfolio[currentIndex], currentBids) * (1 - fee)
                println("CAPITAL $capital")
            } else {
                println("CAPITAL ${portfolio[0]}")
            }
        }
    }

    fun rebalancePortfolio() {
        val endTime = (client.serverTime / periodMs) * periodMs - 1
        val coinToCandles = loadAllCandles(endTime)
        val history = candlesToMatrix(coinToCandles)
        val bestPortfolio = agent.bestPortfolio(history).data
        val buyIndex = bestPortfolio.indexOf(bestPortfolio.max()!!)

        val diff = client.serverTime - endTime
        println("Time diff $diff")
        rebalancePortfolioTo(buyIndex)
    }

    sleepForNextPeriod(client)

    Observable.interval(periodMs, TimeUnit.MILLISECONDS)
            .startWith(0)
            .timeInterval()
            .observeOn(scheduler)
            .subscribe {
                try {
                    rebalancePortfolio()
                } catch (e: Exception) {
                    println(e.message)
                }
            }
}

private fun sleepForNextPeriod(client: BinanceApiRestClient) {
    var msForNextPeriod = periodMs - client.serverTime % periodMs
    if (msForNextPeriod == periodMs) msForNextPeriod = 0

    require(msForNextPeriod < periodMs)
    println("sleep $msForNextPeriod")
    msForNextPeriod += 10 // additional ms
    Thread.sleep(msForNextPeriod)
}