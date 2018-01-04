package data.download

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import data.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.math.roundToLong


private val exchange = "binance"

private val COINS = listOf(
        "USDT", "ETH", "XRP", "IOTA", "XVG", "BCH", "TRX", "LTC",
        "NEO", "ADA", "EOS", "QTUM", "ETC", "DASH", "HSR", "VEN",
        "BNB", "POWR", "POE", "MANA", "MCO", "QSP", "STRAT", "WTC",
        "OMG", "SNT", "BTS", "XMR", "LSK", "ZEC", "SALT", "REQ",
        "STORJ", "YOYO", "LINK", "CTR", "BTG", "ENG", "VIB",
        "MTL"
)

private const val REVERSED_COINS = "USDT"
private val ALT_NAMES = mapOf(
        "BCH" to "BCC"
)

private const val START_DATE = 1420243200L * 1000  // 03.01.2015
private const val END_DATE = 1514937600L * 1000    // 03.01.2018
private val PERIOD_TYPE =  CandlestickInterval.ONE_MINUTE
private val PERIOD_S =  60

fun main(args: Array<String>) {
    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$final_name" else "${final_name}BTC"
    }

    fun chartDataItems(pair: String, startDate: Long, endDate: Long, periodType: CandlestickInterval): List<Candlestick> {
        val factory = BinanceApiClientFactory.newInstance()
        val client = factory.newRestClient()

        val all = ArrayList<Candlestick>()

        var it = startDate
        val end = endDate
        while (it <= end) {
            val res = client.getCandlestickBars(pair, periodType, 500, it, end)
            if (res.size > 0) {
                all.addAll(res)

                val openTime = Date(res.first().openTime)
                val closeTime = Date(res.first().closeTime)
                println("$pair    $openTime    $closeTime")
                it = res.last().closeTime
            } else {
                break
            }
        }

        return all
    }

    fun fillCoinHistory(coin: String) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS
        val items = chartDataItems(pair, START_DATE, END_DATE, PERIOD_TYPE)

        transaction {
            deleteHistories(exchange, coin)
            var lastDate = -1L
            for (item in items) {
                val date = (item.openTime / 1000.0 / PERIOD_S).roundToLong() * PERIOD_S

                if (date == lastDate)
                    continue

                lastDate = date
                insertHistory(History(
                        exchange = exchange,
                        coin = coin,
                        date = date,
                        open = if (isReversed) BigDecimal.ONE divideMoney BigDecimal(item.open) else BigDecimal(item.open),
                        close = if (isReversed) BigDecimal.ONE divideMoney BigDecimal(item.close) else BigDecimal(item.close),
                        high = if (isReversed) BigDecimal.ONE divideMoney BigDecimal(item.low) else BigDecimal(item.high),
                        low = if (isReversed) BigDecimal.ONE divideMoney BigDecimal(item.high) else BigDecimal(item.low),
                        volume = if (isReversed) BigDecimal(item.volume) else BigDecimal(item.quoteAssetVolume)
                ))
            }
        }
    }

    connectCoinDatabase()

    for (coin in COINS) {
        fillCoinHistory(coin)
    }
}
