package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.archive.Periods
import com.dmi.util.io.readBytes
import com.dmi.util.io.writeBytes
import com.dmi.util.lang.ONE_MINUTE
import com.dmi.util.lang.parseInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.load
import kotlinx.serialization.cbor.CBOR.Companion.dump
import java.nio.file.Paths
import java.time.Duration
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun savedTradeConfig(): TradeConfig = load(Paths.get("data/tradeConfig").readBytes())
fun saveTradeConfig(config: TradeConfig) = Paths.get("data/tradeConfig").writeBytes(dump(config))

@Serializable
data class TradeAssets(val main: Asset, val alts: List<Asset>) {
    val all = listOf(main) + alts
}

@Serializable
data class TradeConfig(
        val assets: TradeAssets = TradeAssets(
                main = "BTC",
                alts = listOf(
                        "USDT", "ETH", "TRX", "NANO", "NCASH", "XRP", "DGD", "VEN", "NEO", "LTC", "ADA", "ETC", "ICX", "BCPT", "EOS", "WTC", "XLM", "XMR", "GVT",
                        "MTL", "BCH", "IOST", "IOTA", "OMG", "NEBL", "BCD", "AION", "XVG", "INS", "SUB", "LSK", "QTUM", "CND", "WAVES", "ELF", "DASH",
                        "STRAT", "GXS", "BQX", "BTG", "CTR"
                )
        ),
        val historySize: Int = 160,
        val periods: Periods = Periods(
                start = ISO_LOCAL_DATE_TIME.parseInstant("2017-07-01T00:00:00"),
                duration = Duration.ofSeconds(10)
        ),
        val tradePeriods: Int = 5 * periods.perMinute().toInt(),    // trade every FIVE minutes
        val historyPeriods: Int = 5 * periods.perMinute().toInt(),  // get historical prices every last FIVE minutes
        // start trade only after delay from start of trading period
        // it's needed because http requests to exchange take time
        // it's fixed because neural network is training with this delay
        val tradeDelay: Duration = Duration.ofSeconds(10)
)