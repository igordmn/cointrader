package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TradePeriods
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.neural.jep
import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.days
import com.dmi.util.lang.parseInstantRange
import com.dmi.util.lang.zoneOffset
import java.nio.file.Paths
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

suspend fun trainMulti() {
    val jep = jep()
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
    }
}