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
                "USDT", "TRX", "ETH", "XRP", "VEN", "NEO",
                "EOS", "BCD", "ICX", "WTC", "ELF", "CND",
                "ADA", "XLM", "BCH", "XVG", "LTC", "HSR",
                "NEBL", "IOTA", "ETC", "QTUM", "POE", "BTG",
                "TNB", "ZRX", "LRC", "TNT", "LEND", "GTO",
                "OMG", "BRD", "SUB", "BTS", "WABI", "XMR",
                "OST", "AION", "ENJ", "STRAT", "ENG", "AMB",
                "LSK", "AST", "CDT", "MDA", "LINK", "DASH",
                "KNC", "MTL"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("3.00")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0015"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val backTestStartTime: Instant = Instant.now() - Duration.ofHours(16)
)