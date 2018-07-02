package com.dmi.cointrader

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.util.io.readBytes
import com.dmi.util.io.writeBytes
import com.dmi.util.lang.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.load
import kotlinx.serialization.cbor.CBOR.Companion.dump
import java.nio.file.Paths
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun savedTradeConfig(): TradeConfig = load(Paths.get("network/tradeConfig").readBytes())

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
                        "USDT", "ETH", "LTC", "NEO", "BCC", "WTC", "QTUM", "OMG", "ZRX", "STRAT", "BQX", "IOTA", "XVG", "MTL", "SUB", "EOS",
                        "SNT", "ETC", "ENG", "ZEC", "DASH", "BTG", "VIB", "TRX", "XRP", "VEN", "RCN", "NULS", "XMR", "BCPT", "ARN", "GVT",
                        "LSK", "ADA", "PPT", "CMT", "XLM", "WAVES", "GTO", "ICX", "ELF", "AION", "EDO", "WINGS", "LUN", "INS", "IOST",
                        "NANO", "BLZ", "AE", "NCASH", "ZIL", "ONT", "STORM", "XEM", "WAN", "QLC", "LOOM", "BCN"
                )
        ),
        val periodSpace: PeriodSpace = PeriodSpace(
                start = ISO_LOCAL_DATE_TIME.parseInstant("2017-07-01T00:00:00", zoneOffset("+3")),
                duration = seconds(10)
        ),
        val historyPeriods: HistoryPeriods = HistoryPeriods(count = 80, size = 15 * periodSpace.periodsPerMinute().toInt()),
        val tradePeriods: TradePeriods = TradePeriods(size = 15 * periodSpace.periodsPerMinute().toInt(), delay = 1),
        val preloadPeriods: Int = (1.5 * periodSpace.periodsPerMinute()).toInt(),
        val archiveReloadPeriods: Int = 6 * 10
)

data class TrainConfig(
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2018-01-01T00:00:00", "2018-07-02T17:32:00", zoneOffset("+3")),
        val testDays: Double = 50.0,
        val validationDays: Double = 0.0,

        val steps: Int = 30000,
        val repeats: Int = 1000,
        val repeatsBreak: Int = 1000,
        val repeatsBreakScore: Double = 1.03,
        val logSteps: Int = 500,
        val scoresSkipSteps: Int = 10000,
        val breakSteps: Int = 10000,
        val breakProfit: Double = 1.01,

        val fee: Double = 0.0010,
        val batchSize: Int = 30,
        val tradePeriodGeometricBias: Double = 5e-7
)