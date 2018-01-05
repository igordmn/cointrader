package data.slippage

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.domain.market.OrderBook
import com.binance.api.client.domain.market.OrderBookEntry
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
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

private val logFile = File("D:\\Development\\Projects\\cointrader\\log.log")

private typealias CoinToCandles = Map<String, List<Candlestick>>

fun main(args: Array<String>) {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newRestClient()
    val coins = COINS.take(altcoinsNumber)
    logFile.delete()

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$final_name" else "${final_name}BTC"
    }

    fun buySlippage(price: Double, amount: Double, orders: List<OrderBookEntry>): Double {
        val desiredPrice = price * amount
        var amountLeft = amount

        var actualPrice = 0.0
        for (order in orders) {
            val orderAmount = order.qty.toDouble()
            val orderPrice = order.price.toDouble()
            if (amountLeft > orderAmount) {
                actualPrice += orderAmount * orderPrice
                amountLeft -= orderAmount
            } else {
                actualPrice += amountLeft * orderPrice
                return actualPrice / desiredPrice
            }
        }

        return -1.0
    }

    fun sellSlippage(price: Double, amount: Double, orders: List<OrderBookEntry>): Double {
        val desiredPrice = price * amount
        var amountLeft = amount

        var actualPrice = 0.0
        for (order in orders) {
            val orderAmount = order.qty.toDouble()
            val orderPrice = order.price.toDouble()
            if (amountLeft > orderAmount) {
                actualPrice += orderAmount * orderPrice
                amountLeft -= orderAmount
            } else {
                actualPrice += amountLeft * orderPrice
                return desiredPrice / actualPrice
            }
        }

        return -1.0
    }

    fun slippage(asks: List<OrderBookEntry>, bids: List<OrderBookEntry>, price: Double, amount: Double): Pair<Double, Double> {
        val buySlippage = buySlippage(price, amount, asks)
        val sellSlippage = sellSlippage(price, amount, bids)
        return Pair(buySlippage, sellSlippage)
    }

    fun slippage(orderBook: OrderBook, coin: String, price: Double, amount: Double): Pair<Double, Double> {
        val coinAmount = if (coin in REVERSED_COINS) amount else amount / price
        val asks = orderBook.asks
        val bids = orderBook.bids
        return slippage(asks, bids, price, coinAmount)
    }

    fun loadAllCandles(endTime: Long): CoinToCandles {
        val coinToCandles = coins.associate {
            Pair(
                    it,
                    client.getCandlestickBars(
                            pair(it),
                            CandlestickInterval.ONE_MINUTE,
                            200, null, endTime
                    )
            )
        }
        coinToCandles.values.forEach {
            require(it.last().closeTime == endTime)
        }
        return coinToCandles
    }

    fun printSlippage(orderBook: OrderBook, coin: String, price: Double, amount: Double, timeDiff: Long) {
        val (buySlippage, sellSlippage) = slippage(orderBook, coin, price, amount)
//        println("Slippage $coin \t time $timeDiff \t amount $amount \t buy $buySlippage \t sell $sellSlippage")
        val text = "$coin \t $timeDiff \t $amount \t $buySlippage \t $sellSlippage"
        logFile.appendText(text + "\n")
        println(text)
    }

    fun tick() {
        val currentTime = client.serverTime
        val endTime = (currentTime / 60000) * 60000 - 1

        val coinToCandles = loadAllCandles(endTime)
        Thread.sleep(5000)
        coinToCandles.forEach { coin, candles ->
            val closePrice = coinToCandles[coin]!!.last().close.toDouble()
            val orderBook = client.getOrderBook(pair(coin), 100)
            val timeDiff = client.serverTime - endTime
            printSlippage(orderBook, coin, closePrice, 0.0625, timeDiff)
            printSlippage(orderBook, coin, closePrice, 0.125, timeDiff)
            printSlippage(orderBook, coin, closePrice, 0.25, timeDiff)
            printSlippage(orderBook, coin, closePrice, 0.5, timeDiff)
            printSlippage(orderBook, coin, closePrice, 1.0, timeDiff)
            printSlippage(orderBook, coin, closePrice, 2.0, timeDiff)
            printSlippage(orderBook, coin, closePrice, 4.0, timeDiff)
            printSlippage(orderBook, coin, closePrice, 8.0, timeDiff)
        }
    }

    sleepForNextMinute(client)

    Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0)
            .timeInterval()
            .observeOn(Schedulers.io())
            .subscribe {
                try {
                    tick()
                } catch (e: Exception) {
                    println(e.message)
                }
            }

    while (true) {
        Thread.sleep(10000)
    }
}

private fun sleepForNextMinute(client: BinanceApiRestClient) {
    var msForNextMinute = 60000 - client.serverTime % 60000
    if (msForNextMinute == 60000L) msForNextMinute = 0

    require(msForNextMinute < 60000)
    println("sleep $msForNextMinute")
    msForNextMinute += 10 // additional ms
    Thread.sleep(msForNextMinute)
}