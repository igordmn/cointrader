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
                        // 200
                        "USDT", "ETH", "LTC", "NEO", "BCC", "MCO", "WTC", "QTUM", "OMG", "STRAT", "BQX", "IOTA", "LINK", "XVG", "SALT",
                        "MTL", "SUB", "EOS", "ETC", "ENG", "DASH", "BTG", "TRX", "POWR", "XRP", "ENJ", "VEN", "RCN", "NULS", "XMR",
                        "BCPT", "GVT", "LSK", "DGD", "ADA", "XLM", "WAVES", "ICX", "ELF", "AION", "NEBL", "EDO", "TRIG", "IOST",
                        "NANO", "ZIL", "ONT", "STORM", "NCASH"

                        //600
                        /**
                         * "USDT", "ETH", "LTC", "NEO", "BCC", "QTUM", "OMG", "LINK", "MTL", "EOS", "ETC", "TRX", "XRP", "ENJ", "VEN", "XMR", "GVT", "DGD", "ADA", "XLM", "WAVES", "ICX", "AION", "NEBL", "IOST", "NANO", "NCASH", "STORM"
                         */
                        //1000
                        /**
                         * "USDT", "ETH", "NEO", "BCC", "QTUM", "MTL", "EOS", "TRX", "XRP", "GVT", "ADA", "XLM", "ICX", "NEBL", "NCASH"
                         */

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
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-20T20:10:00", zoneOffset("+3")),
        val testDays: Double = 30.0,
        val validationDays: Double = 90.0,

        val steps: Int = 60000,
        val repeats: Int = 30,
        val logSteps: Int = 1000,

        val fee: Double = 0.0007,
        val batchSize: Int = 60,
        val tradePeriodGeometricBias: Double = 5e-7
)