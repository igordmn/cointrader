package com.dmi.cointrader.main

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.cbor.CBOR.Companion.load
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun savedTradeConfig(): TradeConfig = load(Paths.get("data/tradeConfig").toFile().readBytes())
fun saveTradeConfig(config: TradeConfig) = Paths.get("data/tradeConfig").toFile().writeBytes(dump(config))

@Serializable
data class TradeConfig(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "NANO", "TRX", "ETC", "LTC", "XRP", "DGD", "VEN", "NEO", "ICX", "ADA", "BCPT", "XVG", "XLM", "EOS", "HSR", "LSK", "BCC",
                "MTL", "NEBL", "OMG", "XMR", "GVT", "WTC", "IOTA", "INS", "IOST", "ARN", "BRD", "STRAT", "GXS", "OST"
        ),
        val historyCount: Int = 160,
        val startTime: Instant = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3")),
        val period: Duration = Duration.ofMinutes(5)
)

data class Config(
        val mainCoin: String = "BTC",
        val altCoins: List<String> = listOf(
                "USDT", "ETH", "NANO", "TRX", "ETC", "LTC", "XRP", "DGD", "VEN", "NEO", "ICX", "ADA", "BCPT", "XVG", "XLM", "EOS", "HSR", "LSK", "BCC",
                "MTL", "NEBL", "OMG", "XMR", "GVT", "WTC", "IOTA", "INS", "IOST", "ARN", "BRD", "STRAT", "GXS", "OST"
        ),
        val initialCoins: Map<String, BigDecimal> = mapOf(
                "BTC" to BigDecimal("1.00")
        ),
        val historyCount: Int = 160,
        val fee: BigDecimal = BigDecimal("0.0018"),
        val backTestStartTime: Instant = Instant.now() - Duration.ofDays(5),

        val startTime: Instant = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3")),
        val period: Duration = Duration.ofMinutes(5),

        val trainStartTime: Instant = startTime,
        val trainEndTime: Instant = LocalDateTime.of(2018, 2, 18, 21, 50, 0).toInstant(ZoneOffset.of("+3")),
        val trainTest1Days: Int = 30,
        val trainTest2Days: Int = 7,
        val trainExcludeDays: Int = 7,
        val trainIncludeTestToTrain: Boolean = false,
        val trainGeometricBias: Double = 3e-05,

        val trainLogSteps: Int = 1000,
        val trainBatchSize: Int = 100
)