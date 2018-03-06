package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.*

data class Config(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "NANO", "TRX", "ETC", "LTC", "XRP", "DGD", "VEN", "NEO", "ICX", "ADA", "BCPT", "XVG", "XLM", "EOS", "HSR", "LSK", "BCH",
                "MTL", "NEBL", "OMG", "XMR", "GVT", "WTC", "IOTA", "INS", "IOST", "ARN", "BRD", "STRAT", "GXS", "OST"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("1.00")
        ),
        val historyCount: Int = 320,
        val fee: BigDecimal = BigDecimal("0.0018"),
        val learningRate: BigDecimal = BigDecimal("0.00056"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val backTestStartTime: Instant = Instant.now() - Duration.ofDays(5),

        val startTime: Instant = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3")),
        val period: Duration = Duration.ofMinutes(5),

        val trainEndTime: Instant = LocalDateTime.of(2018, 2, 18, 21, 50, 0).toInstant(ZoneOffset.of("+3")),
        val trainTestDays: Int = 14,

        val trainSteps: Int = 40000,
        val trainLogSteps: Int = 2000,
        val trainMaxNetworkMinSteps: Int = 2000
)