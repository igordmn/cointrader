package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class TestConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "CND", "VEN", "TRX", "EOS", "XRP", "WTC", "TNT", "BNB",
                "ICX", "NEO", "XLM", "ELF", "LEND", "ADA", "LTC", "XVG", "IOTA",
                "HSR", "TNB", "BCH", "BCD", "CTR", "POE", "ETC", "QTUM", "MANA",
                "OMG", "BRD", "AION", "AMB", "SUB", "ZRX", "BTS", "STRAT", "WABI",
                "LINK", "XMR", "QSP", "LSK", "GTO", "ENG", "MCO", "POWR", "CDT",
                "KNC", "REQ", "OST", "ENJ", "DASH"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("0.00332504"),
                "LTC" to BigDecimal("0.00000323"),
                "USDT" to BigDecimal("0.00583317")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0000"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val startTime: Instant = Instant.now() - Duration.ofDays(10)
)