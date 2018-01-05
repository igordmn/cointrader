package realtime

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.domain.market.OrderBook
import com.binance.api.client.domain.market.OrderBookEntry
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.DoubleMatrix4D
import net.NNAgent
import net.PythonUtils
import java.io.File
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

private val bitcoins = 0.1
private val altcoinsNumber = 25
private val windowSize = 160
private val period = CandlestickInterval.FIVE_MINUTES
private val periodMs = 5L * 60 * 1000
private val fee = 0.001

private typealias CoinToCandles = List<List<Candlestick>>

fun main(args: Array<String>) {
    try {
        PythonUtils.startPython()
        main()
    } finally {
        PythonUtils.stopPython()
    }
}

private fun main() {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newRestClient()
    val coins = COINS.take(altcoinsNumber)

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
        return DoubleMatrix4D(1, 3, altcoinsNumber, windowSize) { _, i2, i3, i4 ->
            val coin = coins[i3]
            val isReversed = coin in REVERSED_COINS
            val candle = coinToCandles[i3][i4]
            indicatorByIndex(isReversed, i2, candle)
        }
    }


    val currentTime = client.serverTime
    val endTime = (currentTime / periodMs) * periodMs - 1

    val agent = NNAgent(fee, 3, altcoinsNumber, windowSize, "D:/Development/Projects/coin_predict/train_package/netfile")
    val history = candlesToMatrix(loadAllCandles(endTime))
    val portfolio = agent.bestPortfolio(history)
    var g= 0
    g++


//    fun tick() {
//        val currentTime = client.serverTime
//        val endTime = (currentTime / periodMs) * periodMs - 1
//
//        val coinToCandles = loadAllCandles(endTime)
//
//        coinToCandles.forEach { coin, candles ->
//            val closePrice = coinToCandles[coin]!!.last().close.toDouble()
//            val orderBook = client.getOrderBook(pair(coin), 100)
//            val timeDiff = client.serverTime - endTime
//            printSlippage(orderBook, coin, closePrice, 0.0625, timeDiff)
//        }
//    }
//
//    sleepForNextMinute(client)
//
//    Observable.interval(1, TimeUnit.MINUTES)
//            .startWith(0)
//            .timeInterval()
//            .observeOn(Schedulers.io())
//            .subscribe {
//                try {
//                    tick()
//                } catch (e: Exception) {
//                    println(e.message)
//                }
//            }
//
//    while (true) {
//        Thread.sleep(10000)
//    }
}

private fun sleepForNextMinute(client: BinanceApiRestClient) {
    var msForNextMinute = periodMs - client.serverTime % periodMs
    if (msForNextMinute == periodMs) msForNextMinute = 0

    require(msForNextMinute < periodMs)
    println("sleep $msForNextMinute")
    msForNextMinute += 10 // additional ms
    Thread.sleep(msForNextMinute)
}