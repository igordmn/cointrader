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
                Trade(instant(1500005394457L), 20.00000000, 0.01900000),
                Trade(instant(1500005536859L), 1.59000000, 0.01909900),
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900),
                Trade(instant(1500007884037L), 3.87000000, 0.01922300)
        )
        source.restoredAfter(0) shouldBe listOf(
                Trade(instant(1500005536859L), 1.59000000, 0.01909900),
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900),
                Trade(instant(1500007884037L), 3.87000000, 0.01922300)
        )
        source.restoredAfter(1) shouldBe listOf(
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900),
                Trade(instant(1500007884037L), 3.87000000, 0.01922300)
        )
    }

    "test2" {
        val source = TradeSource(market, instant(1500007691579L), chunkLoadCount = 3)
        source.initialValues() shouldBe listOf(
                Trade(instant(1500005394457L), 20.00000000, 0.01900000),
                Trade(instant(1500005536859L), 1.59000000, 0.01909900),
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900)
        )
        source.restoredAfter(0) shouldBe listOf(
                Trade(instant(1500005536859L), 1.59000000, 0.01909900),
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900)
        )
        source.restoredAfter(1) shouldBe listOf(
                Trade(instant(1500006066191L), 1.90000000, 0.01914800),
                Trade(instant(1500007121710L), 13.75000000, 0.01909600),
                Trade(instant(1500007691579L), 4.79000000, 0.01932900)
        )
    }
})