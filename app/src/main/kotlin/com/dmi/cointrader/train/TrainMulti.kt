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
            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-24T01:45:00", zoneOffset("+3")),
            validationDays = 60.0,
            steps = 16000,
            repeats = 2,
            logSteps = 1000,
            scoresSkipSteps = 4000,
            breakSteps = 8000,
            breakProfit = 1.032
    )

    fun trainConfig2() = TrainConfig(
            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-24T01:45:00", zoneOffset("+3")),
            validationDays = 60.0,
            steps = 16000,
            repeats = 2,
            logSteps = 1000,
            scoresSkipSteps = 8000,
            breakSteps = 12000,
            breakProfit = 1.02
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


    }
}