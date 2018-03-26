package com.dmi.cointrader.train

import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.parseInstantRange
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

data class TrainConfig(
        val fee: Double = 0.0005,
        val range: InstantRange = ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-03-19T20:20:00"),

        val testDays: Double = 14.0,         //  days for test every log step, train includes these days
        val validationDays: Double = 14.0,   //  days for check overfitting, train doesn't include these days

        val logSteps: Int = 1000,
        val batchSize: Int = 109
)