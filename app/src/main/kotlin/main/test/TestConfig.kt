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
                "USDT", "ETH", "TRX", "NEO", "VEN", "XRP", "ICX", "EOS",
                "ELF", "WTC", "CND", "ADA", "XLM", "XVG", "HSR", "LTC",
                "BCH", "ETC", "IOTA", "POE", "BTG", "QTUM", "TNT", "LSK",
                "GAS", "VIB", "ZRX", "OMG", "LEND", "BRD", "GTO", "BTS",
                "SUB", "XMR", "AION", "LRC", "STRAT", "MDA", "ENJ", "QSP",
                "WABI", "KNC", "CMT", "REQ", "AST", "MTL", "DASH", "ZEC",
                "WINGS"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("3.00")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0015"),
        val learningRate: BigDecimal = BigDecimal("0.00028"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val backTestStartTime: Instant = Instant.now() - Duration.ofHours(16)
)