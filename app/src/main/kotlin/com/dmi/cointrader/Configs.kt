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

fun savedTradeConfig(): TradeConfig = load(Paths.get("data/network/tradeConfig").readBytes())

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
                        "USDT", "ETH", "LTC", "NEO", "BCC", "MCO", "WTC", "QTUM", "OMG", "ZRX", "STRAT", "BQX", "IOTA", "LINK", "XVG",
                        "SALT", "MTL", "SUB", "EOS", "SNT", "ETC", "ENG", "DNT", "AST", "DASH", "BTG", "VIB", "TRX", "POWR", "XRP", "ENJ",
                        "VEN", "RCN", "NULS", "XMR", "BCPT", "GVT", "POE", "LSK", "DGD", "ADA", "XLM", "WAVES", "ICX", "ELF", "AION",
                        "NEBL", "EDO", "TRIG", "IOST", "NANO", "NCASH", "POA", "ZIL", "ONT", "STORM", "XEM", "WAN", "QLC"
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
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-05-07T22:00:00", zoneOffset("+3")),
        val testDays: Double = 50.0,
        val validationDays: Double = 8.0,

        val steps: Int = 120000,
        val repeats: Int = 50,
        val repeatsBreak: Int = 20,
        val repeatsBreakScore: Double = 1.000,
        val logSteps: Int = 500,
        val scoresSkipSteps: Int = 10000,
        val breakSteps: Int = 10000,
        val breakProfit: Double = 1.045,

        val fee: Double = 0.0005,
        val batchSize: Int = 30,
        val tradePeriodGeometricBias: Double = 5e-7
)