package main.analyze.date20180127.slippage

import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.NormalizedMarketHistory
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import org.slf4j.LoggerFactory
import util.lang.truncatedTo
import util.math.sum
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) = runBlocking {
    // config for trained net
    val mainCoin = "BTC"
    val altCoins = listOf(
            "USDT", "ETH", "CND", "VEN", "TRX", "EOS", "XRP", "WTC", "TNT", "BNB",
            "ICX", "NEO", "XLM", "ELF", "LEND", "ADA", "LTC", "XVG", "IOTA",
            "HSR", "TNB", "BCH", "BCD", "CTR", "POE", "ETC", "QTUM", "MANA",
            "OMG", "BRD", "AION", "AMB", "SUB", "ZRX", "BTS", "STRAT", "WABI",
            "LINK", "XMR", "QSP", "LSK", "GTO", "ENG", "MCO", "POWR", "CDT",
            "KNC", "REQ", "OST", "ENJ", "DASH", "TRIG", "NEBL", "FUEL"
    )


    val tradeBuilder = TradeBuilder()

    val trades = realTradeLines()
            .map(::parseLine)
            .filter { it != Line.Unknown }
            .map(tradeBuilder::buildNext)
            .filterNotNull()

    val comissions = trades
            .map { it.capitalAfter.divide(it.capitalBefore, 30, RoundingMode.HALF_UP) }
            .toList()

    val meanCommission =  Math.sqrt(comissions.geoMean())

    println("fact commission $meanCommission")

    val api = binanceAPI(log = LoggerFactory.getLogger(BinanceAPI::class.java))
    val constants = BinanceConstants()
    PreloadedBinanceMarketHistories(constants, api, mainCoin, altCoins).use { preloadedHistories ->
        val serverTime = Instant.ofEpochMilli(api.serverTime().serverTime)
        preloadedHistories.preloadBefore(serverTime)

        val funs = object {
            suspend fun closePriceAndApproximatedPrice(coinWithBtc: String, time: Instant): Pair<BigDecimal, BigDecimal> {
                val coin = coinWithBtc.removeSuffix("BTC").removePrefix("BTC")
                val marketName = constants.marketName(mainCoin, coin) ?: constants.marketName(coin, mainCoin)
                val approximatedPricesFactory = LinearApproximatedPricesFactory(30)
                val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
                val history = NormalizedMarketHistory(preloadedHistories[marketName!!], normalizer, Duration.ofMinutes(1))
                val nextMinute = time.truncatedTo(Duration.ofMinutes(1)) + Duration.ofMinutes(1)
                val candles = history.candlesBefore(nextMinute).take(2).toList()
                val candle0 = candles[0]
                val candle1 = candles[1]
                val approximatedPrice = approximatedPricesFactory.forCandle(candle0.item).exactAt(Math.random())
                val closePrice = candle1.item.close
                return Pair(closePrice, approximatedPrice)
            }
        }

        val approximatedCommissions =  trades.asReceiveChannel().map {
            val (fromCoinClose, fromCoinApproximated) = funs.closePriceAndApproximatedPrice(it.fromCoin, it.time)
            val (toCoinClose, toCoinApproximated) = funs.closePriceAndApproximatedPrice(it.toCoin, it.time)
            val fromCommission = fromCoinApproximated / fromCoinClose
            val toCommission = toCoinClose / toCoinApproximated
            fromCommission * toCommission
        }.toList()

        val meanApproximatedCommission = Math.sqrt(approximatedCommissions.geoMean())
        println("approximated commission $meanApproximatedCommission")
    }
}

private fun List<BigDecimal>.geoMean(): Double {
    val total = reduce { acc, item -> acc * item }
    return Math.pow(total.toDouble(), 1.0 / size)
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