package test

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import java.util.*

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


fun main(args: Array<String>) {
    fun pair(coin: String): String {
        val isReversed = coin in REVERSED_COINS
        val final_name = ALT_NAMES[coin] ?: coin
        return if (isReversed) "BTC$final_name" else "${final_name}BTC"
    }

    fun startCandlestickEventStreaming() {
        COINS.take(30).shuffled().forEach {
            val factory = BinanceApiClientFactory.newInstance()
            val client = factory.newWebSocketClient()
            val pair = pair(it)
            client.onCandlestickEvent(pair.toLowerCase(), CandlestickInterval.ONE_MINUTE) { response ->
                val updateCandlestick = Candlestick()
                updateCandlestick.openTime = response.openTime
                updateCandlestick.open = response.open
                updateCandlestick.low = response.low
                updateCandlestick.high = response.high
                updateCandlestick.close = response.close
                updateCandlestick.closeTime = response.closeTime
                updateCandlestick.volume = response.volume
                updateCandlestick.numberOfTrades = response.numberOfTrades
                updateCandlestick.quoteAssetVolume = response.quoteAssetVolume
                updateCandlestick.takerBuyQuoteAssetVolume = response.takerBuyQuoteAssetVolume
                updateCandlestick.takerBuyBaseAssetVolume = response.takerBuyQuoteAssetVolume

                if (response.eventTime >= response.closeTime) {
                    println("" + (response.eventTime - response.closeTime) + "   " + pair + "   " + updateCandlestick)
                }
            }
        }
    }

    startCandlestickEventStreaming()
}