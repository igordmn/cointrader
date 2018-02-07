package withorders

import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import java.nio.file.Paths

fun main(args: Array<String>) {
    runBlocking {
        val period = 60
        val from = Paths.get("../.coinbaseUSD.csv")
        val to = Paths.get("../gdaxBTCUSD.csv")

        val firstTrade = from.toFile().useLines {
            toTrade(parseLine(it.first()), period)
        }

        val trades: ReceiveChannel<Trade> = produce {
            from.toFile().useLines { sequence ->
                sequence.map(::parseLine).map { toTrade(it, period) }.forEach {
                    send(it)
                }
                close()
            }
        }
        val tradesWithSkipped: ReceiveChannel<Trade> = produce {
            var lastTrade = firstTrade
            trades.consumeEach { trade ->
                if (trade != firstTrade) {
                    require(trade.periodIndex >= lastTrade.periodIndex)
                    if (trade.periodIndex > lastTrade.periodIndex + 1) {
                        for (i in lastTrade.periodIndex + 1 until trade.periodIndex) {
                            send(Trade(i, lastTrade.price))
                        }
                    }
                    send(trade)
                    lastTrade = trade
                } else {
                    send(firstTrade)
                }
            }
            close()
        }

        val groupedTrades = produce {
            var trades = ArrayList<Trade>()
            tradesWithSkipped.consumeEach { trade ->
                require(trades.size == 0 || trades.last().periodIndex == trade.periodIndex || trades.last().periodIndex == trade.periodIndex - 1 )
                if (trades.size > 0 && trades.last().periodIndex != trade.periodIndex) {
                    send(trades)
                    trades = ArrayList()
                }
                trades.add(trade)
            }
            close()
        }

        val candleLines: ReceiveChannel<String> = groupedTrades.map { trades: List<Trade> ->
            val open = trades.first().price.setScale(2)
            val close = trades.last().price.setScale(2)
            val high = trades.map { it.price.setScale(2) }.max()!!
            val low = trades.map { it.price.setScale(2) }.min()!!
            require(high >= low)
            require(high >= close)
            require(low <= close)
            "$open,$close,$high,$low"
        }

        to.toFile().bufferedWriter().use { writer ->
            writer.write("open,close,high,low\n")
            candleLines.consumeEach {
                writer.write(it + "\n")
            }
        }
    }
}

private fun parseLine(str: String): Line {
    val values = str.split(",")
    return Line(values[0].toInt(), BigDecimal(values[1]), BigDecimal(values[2]))
}

private fun toTrade(line: Line, periodSeconds: Int) = Trade(line.time / periodSeconds, line.price)

private class Line(val time: Int, val price: BigDecimal, val amount: BigDecimal)
private class Trade(val periodIndex: Int, val price: BigDecimal)