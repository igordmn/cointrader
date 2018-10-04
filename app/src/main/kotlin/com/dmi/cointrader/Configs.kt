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
                        "USDT", "ETH", "LTC", "NEO", "BCC", "WTC", "QTUM", "OMG", "ZRX", "BQX", "IOTA", "XVG", "EOS", "ETC", "ENG", "ZEC",
                        "DASH", "TRX", "XRP", "VEN", "XMR", "BAT", "ARN", "ADA", "XLM", "GTO", "ICX", "ELF", "NANO", "NCASH", "ZIL",
                        "ONT", "WAN", "CVC", "QKC", "NPXS", "KEY", "MFT"
                )
        ),
        val periodSpace: PeriodSpace = PeriodSpace(
                start = ISO_LOCAL_DATE_TIME.parseInstant("2017-07-01T00:00:00", zoneOffset("+3")),
                duration = seconds(10)
        ),
        val historyPeriods: HistoryPeriods = HistoryPeriods(count = 40, size = 30 * periodSpace.periodsPerMinute().toInt()),
        val tradePeriods: TradePeriods = TradePeriods(size = 30 * periodSpace.periodsPerMinute().toInt(), delay = 1),
        val preloadPeriods: Int = (1.5 * periodSpace.periodsPerMinute()).toInt(),
        val archiveReloadPeriods: Int = 6 * 10
)

data class TrainConfig(
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-09-01T00:00:00", "2018-10-04T16:32:00", zoneOffset("+3")),
        val testDays: Double = 90.0,
        val trainDaysExclude: ClosedRange<Double> = -90.0..-40.0,

        val steps: Int = 60000,
        val minBestStep: Int = 15000,
        val repeats: Int = 300,
        val repeatsBreak: Int = 100,
        val repeatsBreakScore: Double = 1.00,
        val logSteps: Int = 1000,
        val scoresSkipSteps: Int = 10000,
        val breakSteps: Int = 14000,
        val breakProfit: Double = 1.004,

        val fee: Double = 0.0015,
        val batchSize: Int = 90,
        val tradePeriodGeometricBias: Double = 1e-4
)