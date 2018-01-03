package data.download

import data.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal


private val exchange = "poloniex"

private val COINS = listOf(
        "XRP", "USDT", "ETH", "BCH", "LTC", "XLM", "XMR", "NXT",
        "XEM", "DASH", "DGB", "ETC", "DOGE", "EMC2", "SC", "LSK",
        "BTS", "ZEC", "STRAT", "FCT", "REP", "ARDR", "OMG", "VTC",
        "BCN", "BURST", "GNT", "MAID", "STEEM", "SYS", "ZRX", "CVC",
        "POT", "NAV", "DCR", "LBC", "STORJ", "FLDC", "GAME", "GNO"
)

private const val REVERSED_COINS = "USDT"
private val ALT_NAMES = mapOf(
        "XLM" to "STR"
)

private const val START_DATE = 1420243200L  // 03.01.2015
private const val END_DATE = 1514937600L    // 03.01.2018
private const val PERIOD = 300   // 5 min

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

    fun chartDataItems(pair: String, startDate: Long, endDate: Long, period: Int): List<ChartDataItem> {
        return requestList(
                "https://poloniex.com/public?command=returnChartData&" +
                        "currencyPair=$pair&" +
                        "start=$startDate&" +
                        "end=$endDate&" +
                        "period=$period"
        )
    }

    fun fillCoinHistory(coin: String) {
        println(coin)
        val pair = pair(coin)
        val isReversed = coin in REVERSED_COINS
        val items = chartDataItems(pair, START_DATE, END_DATE, PERIOD)

        transaction {
            deleteHistories(exchange, coin)
            for (item in items) {
                require(item.date % PERIOD == 0L)

                insertHistory(History(
                        exchange = exchange,
                        coin = coin,
                        date = item.date,
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

    for (coin in COINS) {
        fillCoinHistory(coin)
    }
}
