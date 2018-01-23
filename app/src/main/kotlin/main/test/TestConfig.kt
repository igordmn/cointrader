package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class TestConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "LTC", "TRX", "ETH", "NEO", "XRP", "CND", "ICX", "BCD", "ADA", "XVG",
                "WTC", "XLM", "NEBL", "POE", "HSR", "ETC", "QTUM", "GAS", "TNB",
                "TRIG", "XMR", "LEND", "BTS", "OMG", "QSP", "GTO", "STRAT", "FUN", "CDT",
                "REQ", "LSK", "AION", "MANA", "FUEL", "ZEC", "ENJ", "SALT",
                "MCO"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("0.00344362")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.001"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE,
        val startTime: Instant = Instant.now() - Duration.ofDays(10)
)