package main.analyze.date20180127.slippage

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    val lines = realTradeLines()
            .map(::parseLine)
            .filter { it != Line.Unknown }.take(10).toList()
    print(lines)
}

private fun parseLine(line: String): Line = when {
    line.endsWith("trader.AdvisableTrade real afterGetCapitals") -> Line.AfterGetCapitals(parseTime(line))
    line.contains("trader.AdvisableTrade real AfterTrade") -> Line.AfterTrade(parseTime(line))
    line.contains("e.binance.market.BinanceMarketBroker Sell") -> parseSell(line)
    line.contains("e.binance.market.BinanceMarketBroker Buy") -> parseBuy(line)
    line.startsWith("capitals {") -> parseCapitals(line)
    else -> Line.Unknown
}

private fun parseTime(line: String): Instant {
    val words = line.split(" ")
    val timeStr = words[0] + " " + words[1]
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    return LocalDateTime.parse(timeStr, formatter).toInstant(ZoneOffset.of("Europe/Moscow"))
}

private fun parseSell(line: String): Line {
    val time = parseTime(line)
    val coin = line.removeSuffix(":").split(" ").last()
    return Line.Sell(time, coin)
}

private fun parseBuy(line: String): Line {
    val time = parseTime(line)
    val coin = line.removeSuffix(":").split(" ").last()
    return Line.Buy(time, coin)
}

private fun parseCapitals(line: String) = Line.Capitals(
        line
                .removePrefix("capitals {")
                .removeSuffix("}")
                .split(", ")
                .map { entryStr ->
                    val (coin, priceStr) = entryStr.split("=")
                    Pair(coin, BigDecimal(priceStr))
                }.toMap()
)

sealed class Line {
    data class AfterGetCapitals(val time: Instant) : Line()
    data class AfterTrade(val time: Instant) : Line()
    data class Sell(val time: Instant, val coin: String) : Line()
    data class Buy(val time: Instant, val coin: String) : Line()
    data class Capitals(val map: Map<String, BigDecimal>) : Line()
    object Unknown : Line()
}