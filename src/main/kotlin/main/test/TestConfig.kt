package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class TestConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "MCO", "NEO", "OMG", "QTUM",
                "STRAT", "XVG", "DASH", "ETC", "SNT", "TRX", "LTC", "XRP", "IOTA", "BCH"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("0.002")
        ),
        val period: Duration = Duration.ofMinutes(30),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal(0.0005),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE,
        val startTime: Instant = Instant.now() - Duration.ofDays(10)
)