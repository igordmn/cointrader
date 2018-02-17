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
                "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX", "VEN", "NEO", "ADA", "XLM", "HSR", "EOS",
                "BCH", "LSK", "POE", "PPT", "APPC", "MTL", "IOTA", "WTC", "ENG", "ZRX", "XVG", "OMG", "BRD",
                "ADX", "KNC", "DGD", "QTUM", "ZEC", "GXS", "XMR", "CND", "LEND", "STRAT", "VIBE", "BTG", "ELF",
                "FUN", "BTS", "AION", "DASH", "GAS"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("1.00")
        ),
        val period: Duration = Duration.ofMinutes(5),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0015"),
        val learningRate: BigDecimal = BigDecimal("0.00028"),
        val indicators: AdviseIndicators = AdviseIndicators.CLOSE_HIGH_LOW,
        val backTestStartTime: Instant = Instant.now() - Duration.ofHours(16)
)