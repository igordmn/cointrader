package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class TestConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "TRX", "ETH", "NEO", "XRP", "BCD", "ADA",
                "ICX", "CND", "XVG", "XLM", "WTC", "NEBL", "HSR",
                "POE", "GAS", "ETC", "QTUM", "TRIG",  "TNB", "LEND",
                "QSP", "XMR", "BTS", "OMG", "STRAT", "LSK",
                "FUN", "MANA", "FUEL", "AION", "ENJ", "MCO", "CDT"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("0.003")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0005"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE,
        val startTime: Instant = Instant.now() - Duration.ofDays(9)
)