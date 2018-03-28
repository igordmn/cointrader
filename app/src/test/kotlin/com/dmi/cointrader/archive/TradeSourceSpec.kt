package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.util.test.Spec
import com.dmi.util.test.initialValues
import com.dmi.util.test.instant
import com.dmi.util.test.restoredAfter
import io.kotlintest.matchers.shouldBe

class TradeSourceSpec : Spec({
    val market = binanceExchangeForInfo().market("LTC", "BTC")!!

    "test1" {
        val source = TradeSource(market, instant(1500007920000L), chunkLoadCount = 3)
        source.initialValues() shouldBe listOf(
                Trade(instant(1500005394457L), 0.01900000, 20.00000000, isMakerBuyer = true),
                Trade(instant(1500005536859L), 0.01909900, 1.59000000, isMakerBuyer = true),
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true),
                Trade(instant(1500007884037L), 0.01922300, 3.87000000, isMakerBuyer = true)
        )
        source.restoredAfter(0) shouldBe listOf(
                Trade(instant(1500005536859L), 0.01909900, 1.59000000, isMakerBuyer = true),
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true),
                Trade(instant(1500007884037L), 0.01922300, 3.87000000, isMakerBuyer = true)
        )
        source.restoredAfter(1) shouldBe listOf(
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true),
                Trade(instant(1500007884037L), 0.01922300, 3.87000000, isMakerBuyer = true)
        )
    }

    "test2" {
        val source = TradeSource(market, instant(1500007691579L), chunkLoadCount = 3)
        source.initialValues() shouldBe listOf(
                Trade(instant(1500005394457L), 0.01900000, 20.00000000, isMakerBuyer = true),
                Trade(instant(1500005536859L), 0.01909900, 1.59000000, isMakerBuyer = true),
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true)
        )
        source.restoredAfter(0) shouldBe listOf(
                Trade(instant(1500005536859L), 0.01909900, 1.59000000, isMakerBuyer = true),
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true)
        )
        source.restoredAfter(1) shouldBe listOf(
                Trade(instant(1500006066191L), 0.01914800, 1.90000000, isMakerBuyer = true),
                Trade(instant(1500007121710L), 0.01909600, 13.75000000, isMakerBuyer = true),
                Trade(instant(1500007691579L), 0.01932900, 4.79000000, isMakerBuyer = true)
        )
    }
})