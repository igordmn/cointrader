package dataOld

import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.math.roundToLong


private val exchange = "binance"

private val COINS = setOf(
        "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "ICX", "EOS",
        "ELF", "WTC", "CND", "ADA", "XLM", "XVG", "HSR", "LTC",
        "BCH", "ETC", "IOTA", "POE", "BTG", "QTUM", "TNT", "LSK",
        "GAS", "VIB", "ZRX", "OMG", "LEND", "BRD", "GTO", "BTS",
        "SUB", "XMR", "AION", "LRC", "STRAT", "MDA", "ENJ", "QSP",
        "WABI", "KNC", "CMT", "REQ", "AST", "MTL", "DASH", "ZEC",
        "WINGS",
        "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "EOS", "WTC", "ADA",
        "XVG", "HSR", "LTC", "BCH", "ETC", "IOTA", "POE", "BTG",
        "TNT", "QTUM", "LSK", "GAS", "VIB", "ZRX", "OMG", "BTS",
        "SUB", "XMR", "LRC", "STRAT", "MDA", "ENJ", "KNC", "REQ",
        "AST", "ENG", "ZEC", "DGD", "ADX", "BQX", "SALT"
)

private const val REVERSED_COINS = "USDT"
private val ALT_NAMES = mapOf(
        "BCH" to "BCC"
)

private const val START_DATE = 1420243200L * 1000  // 03.01.2015
private val PERIOD_TYPE = "1m"
private val PERIOD_MS = 60 * 1000

fun main(args: Array<String>) {
    val api = binanceAPI()

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$final_name" else "${final_name}BTC"
    }

    fun chartDataItems(pair: String, startDate: Long, endDate: Long): List<Candlestick> {
        val all = ArrayList<Candlestick>()
        var it = endDate
        while (it >= startDate) {
            val res = runBlocking {
                api.getCandlestickBars(pair, PERIOD_TYPE, 500, null, it)
            }
            if (res.isNotEmpty()) {
                all.addAll(res.filter { it.openTime >= startDate }.reversed())

                val openTime = Date(res.first().openTime)
                val closeTime = Date(res.last().closeTime)
                println("$pair    $openTime    $closeTime")
                it = res.first().openTime - 1
            } else {
                break
            }
        }

        return all.reversed()
    }

    fun fillCoinHistory(coin: String, endDate: Long) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS

        transaction {
            val startDateDB = execSQL("select max(date) as maxdate from History where exchange=\"${exchange}\" and coin=\"$coin\"") { rs ->
                rs.getString("maxdate")
            }?.toLong()

            var startDate = startDateDB?.times(1000) ?: START_DATE
            startDate += PERIOD_MS
            val items = chartDataItems(pair, startDate, endDate)

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
