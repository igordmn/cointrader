package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.util.io.readBytes
import com.dmi.util.io.writeBytes
import com.dmi.util.lang.parseInstant
import com.dmi.util.lang.seconds
import com.dmi.util.lang.zoneOffset
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.load
import kotlinx.serialization.cbor.CBOR.Companion.dump
import java.nio.file.Paths
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun savedTradeConfig(): TradeConfig = load(Paths.get("data/tradeConfig").readBytes())
fun saveTradeConfig(config: TradeConfig) = Paths.get("data/tradeConfig").writeBytes(dump(config))

@Serializable
data class TradeAssets(val main: Asset, val alts: List<Asset>) {
    val all = listOf(main) + alts
}

@Serializable
data class HistoryPeriods(val count: Int, val size: Int)

@Serializable
data class TradePeriods(
        val size: Int,
        // start trade only after delay from start of trading period
        // it's needed because http requests to exchange take time
        // it's fixed because neural network is training with this delay
        val delay: Int
)

@Serializable
data class TradeConfig(
        val assets: TradeAssets = TradeAssets(
                main = "BTC",
                alts = listOf(
                        "USDT", "TRX", "ETH", "ICX", "NCASH", "XRP", "EOS", "NANO", "NEO", "ONT", "ADA", "QTUM", "LTC", "DGD", "IOST", "XVG",
                        "VEN", "XLM", "BCC", "IOTA", "GVT", "MTL", "AION", "NEBL", "ETC", "XMR", "ENJ", "BCPT", "SUB", "DASH", "OMG", "WAVES",
                        "LINK", "ZIL", "WTC", "CTR", "ELF", "NULS", "BQX", "AST", "MCO", "STRAT", "ENG", "TRIG", "INS", "POWR", "TNT", "SNT",
                        "BTG", "EDO", "SALT", "LSK", "VIB", "POE", "RCN", "REQ"
                )
        ),
        val periodSpace: PeriodSpace = PeriodSpace(
                start = ISO_LOCAL_DATE_TIME.parseInstant("2017-07-01T00:00:00", zoneOffset("+3")),
                duration = seconds(10)
        ),
        val historyPeriods: HistoryPeriods = HistoryPeriods(count = 160, size = 5 * periodSpace.periodsPerMinute().toInt()),
        val tradePeriods: TradePeriods = TradePeriods(size = 1 * periodSpace.periodsPerMinute().toInt(), delay = 1),
        val archiveReloadPeriods: Int = 6 * 10
)