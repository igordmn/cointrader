package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.candle.Periods
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

fun savedTradeConfig(): TradeConfig = CBOR.load(Paths.get("data/tradeConfig").toFile().readBytes())
fun saveTradeConfig(config: TradeConfig) = Paths.get("data/tradeConfig").toFile().writeBytes(CBOR.dump(config))

@Serializable
data class TradeAssets(val main: Asset, val alts: List<Asset>) {
    val all = listOf(main) + alts
}

@Serializable
data class TradeConfig(
        val assets: TradeAssets = TradeAssets(
                main = "BTC",
                alts = listOf(
                        "USDT", "ETH", "NANO", "TRX", "ETC", "LTC", "XRP", "DGD", "VEN", "NEO", "ICX", "ADA", "BCPT", "XVG", "XLM",
                        "EOS", "HSR", "LSK", "BCC", "MTL", "NEBL", "OMG", "XMR", "GVT", "WTC", "IOTA", "INS", "IOST", "ARN", "BRD",
                        "STRAT", "GXS", "OST"
                )
        ),
        val historyCount: Int = 160,
        val periods: Periods = Periods(
                start = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3")),
                duration = Duration.ofMinutes(5)
        )
)