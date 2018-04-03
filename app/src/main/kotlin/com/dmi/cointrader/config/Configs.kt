package com.dmi.cointrader.config

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
                        "USDT", "ETH", "LTC", "NEO", "BCC", "WTC", "QTUM", "OMG", "IOTA", "XVG", "MTL", "EOS", "ETC", "TRX", "XRP", "ENJ",
                        "VEN", "XMR", "BCPT", "GVT", "LSK", "DGD", "ADA", "XLM", "ICX", "NEBL", "INS", "IOST", "NANO", "NCASH", "ZIL"
                )
        ),
        val periodSpace: PeriodSpace = PeriodSpace(
                start = ISO_LOCAL_DATE_TIME.parseInstant("2017-07-01T00:00:00", zoneOffset("+3")),
                duration = seconds(10)
        ),
        val historyPeriods: HistoryPeriods = HistoryPeriods(count = 160, size = 5 * periodSpace.periodsPerMinute().toInt()),
        val tradePeriods: TradePeriods = TradePeriods(size = 5 * periodSpace.periodsPerMinute().toInt(), delay = 1),
        val archiveReloadPeriods: Int = 6 * 10
)

data class TrainConfig(
        val fee: Double = 0.0007,
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-02T15:50:00", zoneOffset("+3")),

        val testDays: Double = 1.0,        //  days for test every log step, train includes these days
        val validationDays: Double = 30.0,   //  days for check overfitting, train doesn't include these days

        val logSteps: Int = 1000,
        val logMovingAverageCount: Int = 10,
        val batchSize: Int = 109,
        val tradePeriodGeometricBias: Double = 5e-7
)