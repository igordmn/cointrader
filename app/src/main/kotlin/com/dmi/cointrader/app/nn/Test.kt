package com.dmi.cointrader.app.nn

import com.dmi.cointrader.app.moment.MomentConfig
import com.dmi.cointrader.app.moment.MomentSource
import com.dmi.cointrader.app.moment.momentArray
import com.dmi.cointrader.app.trade.BinanceTradeConfig
import com.dmi.cointrader.app.trade.BinanceTradeSource
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.binanceTradeArray
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class MarketInfo(val coin: String, val name: String, val isReversed: Boolean)

fun marketInfo(coin: String): MarketInfo {
    val mainCoin = "BTC"
    val constants = BinanceConstants()

    val name = constants.marketName(coin, mainCoin)
    val reversedName = constants.marketName(mainCoin, coin)

    return when {
        name != null -> MarketInfo(coin, name, false)
        reversedName != null -> MarketInfo(coin, reversedName, true)
        else -> throw UnsupportedOperationException()
    }
}

fun main(args: Array<String>) {
    runBlocking {
        val api = binanceAPI()
        val startTime = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3"))
        val period = Duration.ofMinutes(5)
        val coins: List<String> = listOf(
                "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX"
        )

        val momentConfig = MomentConfig(startTime, period, coins)
        val currentTime = Instant.ofEpochMilli(api.serverTime().serverTime)

        val coinToTrades = coins.map { coin ->
            val market = marketInfo(coin)
            val tradeConfig = BinanceTradeConfig(market.name)
            val binanceTradeSource = BinanceTradeSource(tradeConfig, currentTime)
            val binanceTrades = binanceTradeArray(Paths.get("D:/yy/trades/$market"))

            binanceTrades.syncWith(binanceTradeSource)

            if (market.isReversed) {
                binanceTrades.map(Trade::reverse)
            } else {
                binanceTrades
            }
        }

        val moments = momentArray(Paths.get("D:/yy/moments"), momentConfig)
        val momentsSource = MomentSource(momentConfig, currentTime, coinToTrades)
        moments.syncWith(momentsSource)
    }
}