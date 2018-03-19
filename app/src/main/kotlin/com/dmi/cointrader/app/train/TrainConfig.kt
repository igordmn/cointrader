package com.dmi.cointrader.app.train

import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.parseInstantRange
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

data class TrainConfig(
        val fee: Double = 0.0020,
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-07T00:00:00", "2018-03-19T00:00:00"),

        val testDays: Int = 30,        //  days for test every log step, train includes these days
        val validationDays: Int = 7,   //  days for check overfitting, train doesn't include these days
        val geometricBias: Double = 3e-05,

        val logSteps: Int = 1000,
        val batchSize: Int = 100
)