package dataOld

import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal


private val exchange = "poloniex"

private val COINS = listOf(
        "XRP", "USDT", "ETH", "BCH", "LTC", "XLM", "XMR", "NXT",
        "XEM", "DASH", "DGB", "ETC", "DOGE", "EMC2", "SC", "LSK",
        "BTS", "ZEC", "STRAT", "FCT", "REP", "ARDR", "VTC",
        "BCN", "BURST", "MAID", "STEEM", "SYS", "POT", "NAV",
        "DCR", "LBC", "FLDC", "GAME"
)

private const val REVERSED_COINS = "USDT"
private val ALT_NAMES = mapOf(
        "XLM" to "STR"
)

private const val START_DATE = 1420243200L  // 03.01.2015
//private const val END_DATE = 1514937600L    // 03.01.2018
private const val PERIOD_S = 300   // 5 min

fun main(args: Array<String>) {
    data class ChartDataItem(
            val date: Long,
            val high: BigDecimal,
            val low: BigDecimal,
            val open: BigDecimal,
            val close: BigDecimal,
            val volume: BigDecimal,
            val quoteVolume: BigDecimal,
            val weightedAverage: BigDecimal
    )

    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "${final_name}_BTC" else "BTC_$final_name"
    }

    fun chartDataItems(pair: String, startDate: Long, endDate: Long): List<ChartDataItem> {
        return requestList(
                "https://poloniex.com/public?command=returnChartData&" +
                        "currencyPair=$pair&" +
                        "start=$startDate&" +
                        "end=$endDate&" +
                        "period=${PERIOD_S}"
        )
    }

    fun fillCoinHistory(coin: String, endDate: Long) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS
//        val items = chartDataItems(pair, START_DATE, END_DATE, PERIOD_S)

        transaction {
//            deleteHistories(exchange, coin)

            val startDateDB = execSQL("select max(date) as maxdate from History where exchange=\"${exchange}\" and coin=\"$coin\"") { rs ->
                rs.getString("maxdate")
            }?.toLong()

            var startDate = startDateDB ?: START_DATE
            startDate += PERIOD_S
            val items = chartDataItems(pair, startDate, endDate)

            for (item in items) {
                require(item.date % PERIOD_S == 0L)

                insertHistory(History(
                        exchange = exchange,
                        coin = coin,
                        openTime = item.date,
                        closeTime = item.date + PERIOD_S,
                        open = if (isReversed) BigDecimal.ONE divideMoney item.open else item.open,
                        close = if (isReversed) BigDecimal.ONE divideMoney item.close else item.close,
                        high = if (isReversed) BigDecimal.ONE divideMoney item.low else item.high,
                        low = if (isReversed) BigDecimal.ONE divideMoney item.high else item.low,
                        volume = if (isReversed) item.quoteVolume else item.volume
                ))
            }
        }
    }

    connectCoinDatabase()

    val endDate = (System.currentTimeMillis() / PERIOD_S) * PERIOD_S

    for (coin in COINS) {
        fillCoinHistory(coin, endDate)
    }
}
