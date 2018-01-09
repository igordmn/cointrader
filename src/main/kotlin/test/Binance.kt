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

    var candlesticksCache: MutableMap<String, Candlestick>? = null

    /**
     * Initializes the candlestick cache by using the REST API.
     */
    fun initializeCandlestickCache(symbol: String, interval: CandlestickInterval) {
        val factory = BinanceApiClientFactory.newInstance()
        val client = factory.newRestClient()
        val candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval)

        candlesticksCache = TreeMap()
        for (candlestickBar in candlestickBars) {
            candlesticksCache!![symbol + candlestickBar.openTime] = candlestickBar
        }
    }

    /**
     * Begins streaming of depth events.
     */
    fun startCandlestickEventStreaming(symbol: String, interval: CandlestickInterval) {
        val factory = BinanceApiClientFactory.newInstance()
        val client = factory.newWebSocketClient()

        client.onCandlestickEvent(symbol.toLowerCase(), interval) { response ->
            val openTime = response.openTime
            var updateCandlestick: Candlestick? = candlesticksCache!![symbol + openTime]
            if (updateCandlestick == null) {
                // new candlestick
                updateCandlestick = Candlestick()
            }
            // update candlestick with the stream data
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

            // Store the updated candlestick in the cache
            candlesticksCache!![symbol + openTime] = updateCandlestick

            if (response.eventTime >= response.closeTime) {
                println("" + System.currentTimeMillis() + "   " + symbol + "   "+ updateCandlestick)
            }
        }
    }

    COINS.take(30).forEach {
        val pair = pair(it)
        initializeCandlestickCache(pair, CandlestickInterval.ONE_MINUTE)
        startCandlestickEventStreaming(pair, CandlestickInterval.ONE_MINUTE)
    }
}