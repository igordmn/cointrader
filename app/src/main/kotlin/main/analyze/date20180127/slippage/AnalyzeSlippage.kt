package main.analyze.date20180127.slippage

import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.math.sum
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) = runBlocking {
    // config for trained net
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
                if (it.sellMarket != null && it.buyMarket != null) {
                    Math.sqrt(cost.toDouble()) * (1 - applyFee)
                } else {
                    cost.toDouble() * (1 - applyFee)
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
    data class Sell(val time: Instant, val market: String) : Line()
    data class Buy(val time: Instant, val market: String) : Line()
    data class Capitals(val map: Map<String, BigDecimal>) : Line()
    object Unknown : Line()
}

data class Trade(val time: Instant, val sellMarket: String?, val buyMarket: String?, val capitalBefore: BigDecimal, val capitalAfter: BigDecimal) {
    init {
        require(sellMarket != null || buyMarket != null)
    }
}

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
                    val result = Trade(state.time, state.sellMarket, state.buyMarket, state.capitalBefore, capital(line.map))
                    this.state = State.BeforeTrade
                    return result
                }
            }
            is Line.Sell -> when (state) {
                is State.CapitalsBeforeTrade -> this.state = State.SellBuy(state.time, state.capitalBefore, line.market, null)
                is State.SellBuy -> this.state = State.SellBuy(state.time, state.capitalBefore, line.market, state.buyMarket)
                else -> this.state = State.BeforeTrade
            }

            is Line.Buy -> when (state) {
                is State.CapitalsBeforeTrade -> this.state = State.SellBuy(state.time, state.capitalBefore, null, line.market)
                is State.SellBuy -> this.state = State.SellBuy(state.time, state.capitalBefore, state.sellMarket, line.market)
                else -> this.state = State.BeforeTrade
            }

            is Line.AfterTrade -> if (state is State.SellBuy) {
                this.state = State.AfterTrade(state.time, state.capitalBefore, state.sellMarket, state.buyMarket)
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
        data class SellBuy(val time: Instant, val capitalBefore: BigDecimal, val sellMarket: String?, val buyMarket: String?) : State()
        data class AfterTrade(val time: Instant, val capitalBefore: BigDecimal, val sellMarket: String?, val buyMarket: String?) : State()
    }
}