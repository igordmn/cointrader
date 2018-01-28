package main.test

import adviser.AdviseIndicators
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class TestConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "TRX", "VEN", "CND", "ELF", "EOS",
                "WTC", "ICX", "XRP", "XLM", "NEO", "HSR", "ADA", "TNT",
                "XVG", "LTC", "TNB", "IOTA", "BCH", "LEND", "POE", "BRD",
                "OMG", "ZRX", "QTUM", "BTS", "AMB", "SUB", "ETC", "ENJ",
                "GTO", "LINK", "OST", "STRAT", "AION", "CDT", "LSK", "MDA",
                "WABI", "LRC", "AST", "XMR", "CTR", "BAT", "FUN", "ENG",
                "KNC", "DASH", "SALT"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("3.00")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0015"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val preloadStartTime: Instant = ZonedDateTime.of(2017, 6, 30, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant(),
        val backTestStartTime: Instant = Instant.now() - Duration.ofDays(10)
)