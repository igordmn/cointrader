package data.download

import com.github.kittinunf.fuel.Fuel
import data.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.math.roundToLong


private val exchange = "bitfinex"

private val COINS = listOf(
        "USD", "BCH", "ETH", "EUR", "IOTA", "XRP", "LTC", "EOS",
        "XMR", "DASH", "ETC", "ZEC", "NEO", "OMG", "BTG", "QTUM",
        "SAN", "ETP", "RRT", "DAT"
)

private val REVERSED_COINS = listOf("USDT", "EUR")
private val ALT_NAMES = mapOf(
        "DASH" to "DSH",
        "QTUM" to "QTM",
        "IOTA" to "IOT"
)

private const val START_DATE = 1420243200L * 1000  // 03.01.2015
//private const val END_DATE = 1514937600L * 1000    // 03.01.2018
private const val PERIOD_S = 60
private const val PERIOD_NAME = "1m"

fun main(args: Array<String>) {
    data class ChartDataItem(
            val date: Long,
            val open: BigDecimal,
            val high: BigDecimal,
            val low: BigDecimal,
            val close: BigDecimal,
            val volume: BigDecimal
    )

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "tBTC$final_name" else "t${final_name}BTC"
    }

    fun parseResult(str: String): List<ChartDataItem> {
        val items = ArrayList<ChartDataItem>()
        if (str == "[]")
            return items

        require(str.startsWith("[[") && str.endsWith("]]"))

        val strItems = str.drop(2).dropLast(2).split("],[")
        for (strItem in strItems) {
            val strItemValues = strItem.split(",")
            val date = strItemValues[0].toLong()
            val open = BigDecimal(strItemValues[1])
            val close = BigDecimal(strItemValues[2])
            val high = BigDecimal(strItemValues[3])
            val low = BigDecimal(strItemValues[4])
            val volume = BigDecimal(strItemValues[5])
            items.add(ChartDataItem(
                    date = date,
                    open = open,
                    close = close,
                    high = high,
                    low = low,
                    volume = volume
            ))
        }
        return items
    }

    fun chartDataItemsChunk(pair: String, endDate: Long): List<ChartDataItem> {
        Thread.sleep(3100)
        repeat(10) {
            val response = Fuel.get(
                    "https://api.bitfinex.com/v2/candles/trade:$PERIOD_NAME:$pair/hist?" +
                            "limit=1000&" +
                            "end=$endDate&"
            ).responseString().third
            val (success, error) = response
            if (error != null) {
                println(error)
                Thread.sleep(60000)
            } else {
                return parseResult(success!!)
            }
        }

        throw RuntimeException("timeout")
    }

    fun chartDataItems(pair: String, startDate: Long, endDate: Long): List<ChartDataItem> {
        val all = ArrayList<ChartDataItem>()
        var it = endDate
        while (it >= startDate) {
            val chunk = chartDataItemsChunk(pair, it)
            if (chunk.isEmpty()) {
                break
            }

            all.addAll(chunk)

            val from = Date(chunk.first().date)
            val to = Date(chunk.last().date)
            println("$pair    $from    $to")
            it = chunk.last().date - PERIOD_S * 1000
        }
        return all.filter { it.date >= startDate + PERIOD_S }
    }

    fun fillCoinHistory(coin: String, endDate: Long) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS

        transaction {
//            deleteHistories(exchange, coin)

            val startDateDB = execSQL("select max(date) as maxdate from History where exchange=\"$exchange\" and coin=\"$coin\"") { rs ->
                rs.getString("maxdate")
            }?.toLong()

            var startDate = startDateDB?.times(1000) ?: START_DATE
            startDate += PERIOD_S * 1000
            val items = chartDataItems(pair, startDate, endDate)

            var lastDate = -1L
            for (item in items) {
                val date = (item.date / 1000.0 / PERIOD_S).roundToLong() * PERIOD_S - PERIOD_S
                if (date == lastDate)
                    continue

                lastDate = date

                val averagePrice = (item.close + item.open) divideMoney BigDecimal(2)

                insertHistory(History(
                        exchange = exchange,
                        coin = coin,
                        openTime = date,
                        closeTime = date + PERIOD_S,
                        open = if (isReversed) BigDecimal.ONE divideMoney item.open else item.open,
                        close = if (isReversed) BigDecimal.ONE divideMoney item.close else item.close,
                        high = if (isReversed) BigDecimal.ONE divideMoney item.low else item.high,
                        low = if (isReversed) BigDecimal.ONE divideMoney item.high else item.low,
                        volume = if (isReversed) item.volume else item.volume * averagePrice
                ))
            }
        }
    }

    connectCoinDatabase()

    val endDate = (System.currentTimeMillis() / (PERIOD_S * 1000)) * PERIOD_S * 1000

    for (coin in COINS) {
        fillCoinHistory(coin, endDate)
    }
}
