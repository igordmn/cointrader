package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TradePeriods
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.neural.jep
import com.dmi.util.lang.parseInstantRange
import com.dmi.util.lang.zoneOffset
import java.nio.file.Paths
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

suspend fun trainMulti() {
    val jep = jep()

    fun trainConfig() = TrainConfig(
            range = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-29T00:50:00", zoneOffset("+3")),
            validationDays1 = 50.0,
            validationDays2 = 0.0,
            steps = 32000,
            repeats = 5,
            repeatsBreak = 2,
            repeatsBreakScore = 1.05,
            logSteps = 2000,
            scoresSkipSteps = 10000,
            breakSteps = 19999,
            breakProfit = 1.05
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

        train(TradeConfig(), trainConfig(), "{}")   // 0
    }
}