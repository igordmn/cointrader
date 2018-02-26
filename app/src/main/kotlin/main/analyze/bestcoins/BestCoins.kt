package main.analyze.bestcoins

import java.math.BigDecimal
import java.nio.file.Paths

fun main(args: Array<String>) {
    val file = Paths.get("C:/Users/Igor/Downloads/TradeHistory.csv").toFile()
    val profits = file
            .readLines().asSequence()
            .drop(1)
            .map(::parseOrder)
            .fold(ArrayList<Order>()) { acc, order ->
                when {
                    acc.size == 0 -> acc.add(order)
                    else -> {
                        val last = acc.last()
                        when {
                            last.type == order.type && last.market == order.market -> {
                                acc.removeAt(acc.size - 1)
                                acc.add(Order(order.market, order.type, last.totalAmount + order.totalAmount))
                            }
                            else -> acc.add(order)
                        }
                    }
                }
                acc
            }
            .dropWhile { it.type == OrderType.BUY }
            .zipWithNext(::SellBuy)
            .map { Profit(it.sell.market, it.sell.totalAmount.toDouble() / it.buy.totalAmount.toDouble()) }

    println(profits.joinToString("\n"))
}

private fun parseOrder(it: String): Order {
    val values = it.split(",")
    val market = values[1]
    val type = enumValueOf<OrderType>(values[2])
    val totalAmount = BigDecimal(values[5])
    return Order(market, type, totalAmount)
}

private enum class OrderType { BUY, SELL }

private data class Order(val market: String, val type: OrderType, val totalAmount: BigDecimal)

private data class SellBuy(val sell: Order, val buy: Order) {
    init {
        require(sell.type == OrderType.SELL)
        require(buy.type == OrderType.BUY)
    }
}

private data class Profit(val market: String, val profit: Double)