package dataOld

import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.math.roundToLong


private val exchange = "binance"

private val COINS = listOf(
        "USDT", "TRX", "ETH", "XRP", "VEN", "NEO",
        "EOS", "BCD", "ICX", "WTC", "ELF", "CND",
        "ADA", "XLM", "BCH", "XVG", "LTC", "HSR",
        "NEBL", "IOTA", "ETC", "QTUM", "POE", "BTG",
        "TNB", "ZRX", "LRC", "TNT", "LEND", "GTO",
        "OMG", "BRD", "SUB", "BTS", "WABI", "XMR",
        "OST", "AION", "ENJ", "STRAT", "ENG", "AMB",
        "LSK", "AST", "CDT", "MDA", "LINK", "DASH",
        "KNC", "MTL"
)

private const val REVERSED_COINS = "USDT"
private val ALT_NAMES = mapOf(
        "BCH" to "BCC"
)

private const val START_DATE = 1420243200L * 1000  // 03.01.2015
//private const val END_DATE = 1514937600L * 1000    // 03.01.2018
private val PERIOD_TYPE = "1m"
private val PERIOD_MS = 60 * 1000

fun main(args: Array<String>) {
    val api = binanceAPI()

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$final_name" else "${final_name}BTC"
    }

    fun chartDataItems(pair: String, startDate: Long, endDate: Long, periodType: String): List<Candlestick> {
        val all = ArrayList<Candlestick>()
        var it = startDate
        val end = endDate
        while (it <= end) {
            val res = runBlocking {
                api.getCandlestickBars(pair, periodType, 500, it, end)
            }
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

    fun fillCoinHistory(coin: String, endDate: Long) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS

        transaction {
            //            deleteHistories(exchange, coin)

            val startDateDB = execSQL("select max(date) as maxdate from History where exchange=\"${exchange}\" and coin=\"$coin\"") { rs ->
                rs.getString("maxdate")
            }?.toLong()

            var startDate = startDateDB?.times(1000) ?: START_DATE
            startDate += PERIOD_MS
            val items = chartDataItems(pair, startDate, endDate, PERIOD_TYPE)

            var lastDate = -1L
            for (item in items) {
                val date = (item.openTime.toDouble() / PERIOD_MS).roundToLong() * (PERIOD_MS / 1000)

                if (date == lastDate)
                    continue

                lastDate = date
                insertHistory(History(
                        exchange = exchange,
                        coin = coin,
                        openTime = date,
                        closeTime = date + (PERIOD_MS / 1000),
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

    val serverTime = runBlocking { api.serverTime() }.serverTime
    val endDate = (serverTime / PERIOD_MS) * PERIOD_MS

    for (coin in COINS) {
        fillCoinHistory(coin, endDate)
    }
}
