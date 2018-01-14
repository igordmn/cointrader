package main.test

import adviser.AdviseIndicators
import java.time.Duration
import java.time.Instant

object TestConfig {
    val mainCoin = "BTC"
    val altCoins = listOf("USDT", "ETH", "MCO", "NEO", "OMG", "QTUM", "STRAT", "XVG", "DASH", "ETC", "SNT", "TRX")
    val initialCoins = mapOf(
            "BTC" to "0.002"
    )
    val period = Duration.ofMinutes(30)
    val historyCount = 160
    val fee = 0.0005
    val slippage = 0.0015
    val indicators = AdviseIndicators.CLOSE
    val endTime = Instant.now() - Duration.ofDays(1)
    val startTime = endTime - Duration.ofDays(5)
}