package com.dmi.cointrader.train

import com.dmi.cointrader.TradeAssets
import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TradePeriods
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.neural.jep
import com.dmi.util.lang.parseInstantRange
import com.dmi.util.lang.zoneOffset
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

suspend fun trainMulti() {
    val jep = jep()

    fun trainConfig() = TrainConfig(
            steps = 16000,
            repeats = 2,
            logSteps = 1000,
            scoresSkipSteps = 4000,
            breakSteps = 8000,
            breakProfit = 1.032
    )

    fun trainConfig2() = TrainConfig(
            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-24T01:45:00", zoneOffset("+3")),
            validationDays = 20.0,
            steps = 30000,
            repeats = 3,
            logSteps = 1000,
            scoresSkipSteps = 10000,
            breakSteps = 12000,
            breakProfit = 1.01
    )

    var num = 0

    with(object {
        suspend fun train(tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String) {
            try {
                println("   Num $num")
                train(jep, Paths.get("data/resultsMulti/$num"), tradeConfig, trainConfig, additionalParams)
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
        train(TradeConfig(historyPeriods = historyPeriods(160)), trainConfig(), "{}")
        train(TradeConfig(historyPeriods = historyPeriods(120)), trainConfig(), "{}")

        train(TradeConfig(assets = TradeAssets(main = "BTC", alts = listOf(
                "USDT", "ETH", "LTC", "NEO", "BCC", "MCO", "WTC", "QTUM", "OMG", "STRAT", "BQX", "IOTA", "LINK", "XVG", "SALT",
                "MTL", "SUB", "EOS", "SNT", "ETC", "ENG", "DNT", "DASH", "BTG", "TRX", "POWR", "XRP", "ENJ", "VEN", "RCN", "NULS",
                "XMR", "BCPT", "GVT", "POE", "LSK", "DGD", "ADA", "XLM", "WAVES", "ICX", "ELF", "AION", "NEBL", "EDO", "TRIG",
                "IOST", "NANO", "NCASH", "ZIL", "ONT", "STORM"
        ))), trainConfig2(), "{}")
        train(TradeConfig(assets = TradeAssets(main = "BTC", alts = listOf(
                "USDT", "ETH", "LTC", "NEO", "BCC", "QTUM", "OMG", "LINK", "MTL", "EOS", "ETC", "TRX", "XRP", "ENJ", "VEN",
                "XMR", "GVT", "DGD", "ADA", "XLM", "WAVES", "ICX", "AION", "NEBL", "IOST", "NANO", "NCASH", "STORM", "XVG"
        ))), trainConfig2(), "{}")
        train(TradeConfig(assets = TradeAssets(main = "BTC", alts = listOf(
                "USDT", "ETH", "LTC", "NEO", "BCC", "QTUM", "OMG", "LINK", "MTL", "EOS", "ETC", "TRX", "XRP", "ENJ", "VEN",
                "XMR", "GVT", "DGD", "ADA", "XLM", "WAVES", "ICX", "AION", "NEBL", "IOST", "NANO", "NCASH", "STORM"
        ))), trainConfig2(), "{}")
    }
}