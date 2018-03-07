package main.analyze.date20180127.slippage

import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.binance.market.makeBinanceCacheDB
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.NormalizedMarketHistory
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import com.dmi.util.lang.truncatedTo
import com.dmi.util.math.sum
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) = runBlocking {
    // config for trained net
    val mainCoin = "BTC"
    val applyFee = 0.0005

    val tradeBuilder = TradeBuilder()

    val trades = realTradeLines()
            .map(::parseLine)
            .filter { it != Line.Unknown }
            .map(tradeBuilder::buildNext)
            .filterNotNull()

    val costs = trades
            .map {
                val cost = it.capitalAfter.divide(it.capitalBefore, 30, RoundingMode.HALF_UP)
                if (it.toCoin == mainCoin || it.fromCoin == mainCoin) {
                    cost.toDouble() * (1 - applyFee)
                } else {
                    Math.sqrt(cost.toDouble()) * (1 - applyFee)
                }
            }
            .toList()

    val fee = 1 - costs.geoMean()
    val count = costs.size

    println("fact fee $fee. count $count")
}

private fun List<Double>.geoMean(): Double {
    val total = reduce { acc, item -> acc * item }
    return Math.pow(total, 1.0 / size)
}

private fun parseLine(line: String): Line = when {
    line.endsWith("trader.AdvisableTrade real afterGetCapitals") -> Line.AfterGetCapitals(parseTime(line))
    line.contains("trader.AdvisableTrade real afterTrade") -> Line.AfterTrade(parseTime(line))
    line.contains("e.binance.market.BinanceMarketBroker Sell") -> parseSell(line)
    line.contains("e.binance.market.BinanceMarketBroker Buy") -> parseBuy(line)
    line.startsWith("capitals {") -> parseCapitals(line)
    else -> Line.Unknown
}

private fun parseTime(line: String): Instant {
    val words = line.split(" ")
    val timeStr = words[0] + " " + words[1]
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    return LocalDateTime.parse(timeStr, formatter).toInstant(ZoneOffset.of("+3"))
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

data class Trade(val time: Instant, val fromCoin: String, val toCoin: String, val capitalBefore: BigDecimal, val capitalAfter: BigDecimal)

class TradeBuilder {
    private var state: State = State.BeforeTrade

    fun buildNext(line: Line): Trade? {
        val state = state

        when (line) {
            is Line.AfterGetCapitals -> {
                this.state = State.AfterGetCapitals(line.time)
            }
            is Line.Capitals -> when (state) {
                is State.AfterGetCapitals -> {
                    this.state = State.CapitalsBeforeTrade(state.time, capital(line.map))
                }
                is State.AfterTrade -> {
                    val result = Trade(state.time, state.fromCoin, state.toCoin, state.capitalBefore, capital(line.map))
                    this.state = State.BeforeTrade
                    return result
                }
            }
            is Line.Sell -> if (state is State.CapitalsBeforeTrade) {
                this.state = State.Sell(state.time, state.capitalBefore, line.coin)
            } else {
                this.state = State.BeforeTrade
            }

            is Line.Buy -> if (state is State.Sell) {
                this.state = State.Buy(state.time, state.capitalBefore, state.fromCoin, line.coin)
            } else {
                this.state = State.BeforeTrade
            }

            is Line.AfterTrade -> if (state is State.Buy) {
                this.state = State.AfterTrade(state.time, state.capitalBefore, state.fromCoin, state.toCoin)
            } else {
                this.state = State.BeforeTrade
            }
        }

        return null
    }

    private fun capital(capitals: Map<String, BigDecimal>) = capitals.values.sum()

    private sealed class State {
        object BeforeTrade : State()
        data class AfterGetCapitals(val time: Instant) : State()
        data class CapitalsBeforeTrade(val time: Instant, val capitalBefore: BigDecimal) : State()
        data class Sell(val time: Instant, val capitalBefore: BigDecimal, val fromCoin: String) : State()
        data class Buy(val time: Instant, val capitalBefore: BigDecimal, val fromCoin: String, val toCoin: String) : State()
        data class AfterTrade(val time: Instant, val capitalBefore: BigDecimal, val fromCoin: String, val toCoin: String) : State()
    }
}