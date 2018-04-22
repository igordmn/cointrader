package com.dmi.cointrader.train

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
//            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-20T00:45:00", zoneOffset("+3")),
//            validationDays = 30.0,
            steps = 25000,
            repeats = 4,
            logSteps = 1000,
            scoresSkipSteps = 10000,
            breakSteps = 10000,
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
        train(TradeConfig(), trainConfig(), "{'kernel_size':7}")
        train(TradeConfig(), trainConfig(), "{'kernel_size':9}")
        train(TradeConfig(), trainConfig(), "{'kernel_size':2}")
        train(TradeConfig(), trainConfig(), "{'nb_filter':10}")
        train(TradeConfig(), trainConfig(), "{'nb_filter':20}")
        train(TradeConfig(), trainConfig(), "{'nb_filter':30}")
        train(TradeConfig(), trainConfig(), "{'filter_number':30}")
        train(TradeConfig(), trainConfig(), "{'filter_number':50}")
        train(TradeConfig(), trainConfig(), "{'activation':'prelu'}")
        train(TradeConfig(), trainConfig(), "{'activation':'elu'}")
        train(TradeConfig(), trainConfig(), "{'activation':'celu'}")
        train(TradeConfig(), trainConfig(), "{'activation':'selu'}")
    }
}