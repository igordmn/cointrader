package old.main.analyze.bestcoins

import com.dmi.util.math.geoMean
import old.main.analyze.date20180127.slippage.Line
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    val file = Paths.get("C:/Users/Igor/Downloads/TradeHistory.csv").toFile()
    val profits = file
            .readLines().asSequence()
            .drop(1)
            .map(::parseOrder)
            .map {
                if (it.market == "BTCUSDT") {
                    Order(it.time, it.market, it.type.reverse(), 1 / it.price, it.totalAmount * it.price)
                } else {
                    it
                }
            }
            .fold(ArrayList<Order>()) { acc, order ->
                when {
                    acc.size == 0 -> acc.add(order)
                    else -> {
                        val last = acc.last()
                        when {
                            last.type == order.type && last.market == order.market -> {
                                acc.removeAt(acc.size - 1)
                                acc.add(Order(last.time, order.market, order.type, order.price, last.totalAmount + order.totalAmount))
                            }
                            else -> acc.add(order)
                        }
                    }
                }
                acc
            }
            .fold(ArrayList<SellBuy>()) { acc, order ->
                when {
                    acc.size == 0 -> if (order.type == OrderType.SELL) {
                        acc.add(SellBuy(order, null))
                    }
                    else -> {
                        val last = acc.last()
                        when {
                            last.buy == null -> if (order.type == OrderType.BUY && order.market == last.sell!!.market) {
                                acc.removeAt(acc.size - 1)
                                acc.add(SellBuy(last.sell, order))
                            }
                            else -> if (order.type == OrderType.SELL) {
                                acc.add(SellBuy(order, null))
                            }
                        }
                    }
                }
                acc
            }
            .filter { it.sell != null && it.buy != null }
            .map { Profit(it.sell!!.market, it.sell.totalAmount / it.buy!!.totalAmount) }

    val profitsCombined = profits
            .groupBy { it.market }
            .mapValues { it.value.map { it.profit }.let(::geoMean) }
            .map { Profit(it.key, it.value) }
            .sortedBy { it.profit }

    println(profitsCombined.joinToString("\n"))
}

private fun OrderType.reverse(): OrderType = when(this) {
    OrderType.BUY -> OrderType.SELL
    OrderType.SELL -> OrderType.BUY
}

private fun parseOrder(it: String): Order {
    val values = it.split(",")
    val time = parseTime(values[0])
    val market = values[1]
    val type = enumValueOf<OrderType>(values[2])
    val price = values[3].toDouble()
    val totalAmount = values[5].toDouble()
    return Order(time, market, type, price, totalAmount)
}

private fun parseTime(line: String): Instant {
    val words = line.split(" ")
    val timeStr = words[0] + " " + words[1]
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return LocalDateTime.parse(timeStr, formatter).toInstant(ZoneOffset.of("+3"))
}

private enum class OrderType { BUY, SELL }

private data class Order(val time: Instant, val market: String, val type: OrderType, val price: Double, val totalAmount: Double)

private data class SellBuy(val sell: Order?, val buy: Order?) {
    init {
        require(sell == null || sell.type == OrderType.SELL)
        require(buy == null || buy.type == OrderType.BUY)
    }
}

private data class Profit(val market: String, val profit: Double)