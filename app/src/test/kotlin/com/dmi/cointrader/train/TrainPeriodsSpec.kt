package com.dmi.cointrader.train

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class TrainPeriodsSpec : Spec({
    "splitForTrain" {
        // 2 7 12 17 22 27
        (2L..30L step 5L).splitForTrain(periodsPerDay = 1.5, testDays = 7.4, validationDays = 3.4) shouldBe TrainPeriods(
                train = 2L..22L step 5L,
                test = 17L..22L step 5L,
                validation = 27L..27L step 5L
        )
    }
})