package read

import java.io.File



private val interval = 5 * 60L // 5 minutes
private val diff = 5 // +-5 seconds
private val minTradeCount = 12 * 24 // one continuous day

fun main(args: Array<String>) {
    val trades = ArrayList<ContinuousTrade>(10000)

    var trade = ArrayList<Double>(1000)
    var lastTime = -1L

    File("D:/1/bitstampUSD.csv/.bitstampUSD.csv").reader().useLines { sequence ->
        sequence.map {
            it.split(',')
        }.map {
            PriceMoment(it[0].toLong(), it[1].toDouble())
        }.forEach {
            if (lastTime == -1L)
                lastTime = it.time

            if (it.time - lastTime >= interval && it.time - lastTime < interval + diff) {
                trade.add(it.price)
                lastTime = it.time
            } else if (it.time - lastTime > interval) {
                trades.add(ContinuousTrade(trade))
                trade = ArrayList(1000)
                lastTime = it.time
            }
        }
    }
}


class PriceMoment(val time: Long, val price: Double)

class ContinuousTrade(val prices: List<Double>)