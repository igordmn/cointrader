package com.dmi.cointrader.train

import com.dmi.cointrader.TradeAssets
import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TradePeriods
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.trade.performTestTradesAllInFast
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.parseInstantRange
import com.dmi.util.lang.zoneOffset
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

suspend fun trainBatch() {
    val jep = jep()

    fun trainConfig() = TrainConfig(
//            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-23T00:00:00", zoneOffset("+3")),
//            validationDays = 20.0,
            steps = 13000,
            repeats = 2,
            logSteps = 1000,
            scoresSkipSteps = 4000,
            breakSteps = 7000,
            breakProfit = 1.032
    )

    var num = 0

    with(object {
        suspend fun train(tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String) {
            try {
                println("   Num $num")
                train(jep, Paths.get("data/resultsBatch/$num"), tradeConfig, trainConfig, additionalParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            num++
        }
    }) {
        fun historyPeriods(count: Int) = TradeConfig().historyPeriods.copy(count = count)
        fun historyPeriods2(minutes: Int) = TradeConfig().historyPeriods.copy(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt())
        fun tradePeriods(minutes: Int): TradePeriods = TradePeriods(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt(), delay = 1)

        train(TradeConfig(), trainConfig(), "{}")

//        train(TradeConfig(assets = TradeAssets(main = "BTC", alts = listOf(
//                "USDT", "ETH", "LTC", "NEO", "BCC", "QTUM", "OMG", "LINK", "MTL", "EOS", "ETC", "TRX", "XRP", "ENJ", "VEN",
//                "XMR", "GVT", "DGD", "ADA", "XLM", "WAVES", "ICX", "AION", "NEBL", "IOST", "NANO", "NCASH", "STORM", "XVG"
//        ))), TrainConfig(), "{}")
//
//        train(TradeConfig(assets = TradeAssets(main = "BTC", alts = listOf(
//                "USDT", "ETH", "LTC", "NEO", "BCC", "MCO", "WTC", "QTUM", "OMG", "STRAT", "BQX", "IOTA", "LINK", "XVG", "SALT",
//                "MTL", "SUB", "EOS", "SNT", "ETC", "ENG", "DNT", "DASH", "BTG", "TRX", "POWR", "XRP", "ENJ", "VEN", "RCN", "NULS",
//                "XMR", "BCPT", "GVT", "POE", "LSK", "DGD", "ADA", "XLM", "WAVES", "ICX", "ELF", "AION", "NEBL", "EDO", "TRIG",
//                "IOST", "NANO", "NCASH", "ZIL", "ONT", "STORM"
//        ))), TrainConfig(), "{}")
    }
}