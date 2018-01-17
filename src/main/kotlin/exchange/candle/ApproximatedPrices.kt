package exchange.candle

import util.math.max
import util.math.min
import java.math.BigDecimal
import java.math.RoundingMode

interface ApproximatedPricesFactory {
    fun forCandle(candle: Candle): ApproximatedPrices
}

class LinearApproximatedPricesFactory(
        private val operationScale: Int
) : ApproximatedPricesFactory {
    override fun forCandle(candle: Candle): ApproximatedPrices = LinearApproximatedPrices(candle, operationScale)
}

interface ApproximatedPrices {
    fun cutCandle(t1: Double, t2: Double) = Candle(
            open = exactAt(t1),
            close = exactAt(t2),
            high = highBetween(t1, t2),
            low = lowBetween(t1, t2)
    )

    fun exactAt(t: Double): BigDecimal
    fun highBetween(t1: Double, t2: Double): BigDecimal
    fun lowBetween(t1: Double, t2: Double): BigDecimal
}

class LinearApproximatedPrices(
        candle: Candle,
        private val operationScale: Int
): ApproximatedPrices {
    private val x1 = 0.0
    private val x2 = 1.0 / 3
    private val x3 = 2.0 / 3
    private val x4 = 1.0
    private val y1: BigDecimal = candle.open
    private val y2: BigDecimal
    private val y3: BigDecimal
    private val y4: BigDecimal = candle.close

    init {
        if ((candle.open - candle.high).abs() <= (candle.open - candle.low).abs()) {
            y2 = candle.high
            y3 = candle.low
        } else {
            y2 = candle.low
            y3 = candle.high
        }
    }

    override fun exactAt(t: Double): BigDecimal {
        require(t in 0.0..1.0)
        return when {
            t <= x2 -> line1at(t)
            t <= x3 -> line2at(t)
            else -> line3at(t)
        }
    }

    override fun highBetween(t1: Double, t2: Double): BigDecimal {
        require(t1 in 0.0..1.0)
        require(t2 in 0.0..1.0)
        require(t1 <= t2)

        return when {
            t1 <= x2 && t2 <= x2 -> max(line1at(t1) ,line1at(t2))
            t1 <= x2 && t2 <= x3 -> max(line1at(t1), scale(y2), line2at(t2))
            t1 <= x2 && t2 <= x4 -> max(line1at(t1), scale(y2), scale(y3), line3at(t2))
            t1 <= x3 && t2 <= x3 -> max(line2at(t1) ,line2at(t2))
            t1 <= x3 && t2 <= x4 -> max(line2at(t1), scale(y3), line3at(t2))
            else -> max(line3at(t1), line3at(t2))
        }
    }

    override fun lowBetween(t1: Double, t2: Double): BigDecimal {
        require(t1 in 0.0..1.0)
        require(t2 in 0.0..1.0)
        require(t1 <= t2)

        return when {
            t1 <= x2 && t2 <= x2 -> min(line1at(t1) ,line1at(t2))
            t1 <= x2 && t2 <= x3 -> min(line1at(t1), scale(y2), line2at(t2))
            t1 <= x2 && t2 <= x4 -> min(line1at(t1), scale(y2), scale(y3), line3at(t2))
            t1 <= x3 && t2 <= x3 -> min(line2at(t1) ,line2at(t2))
            t1 <= x3 && t2 <= x4 -> min(line2at(t1), scale(y3), line3at(t2))
            else -> min(line3at(t1), line3at(t2))
        }
    }

    private fun line1at(x: Double) = linearApproximate(x1, x2, y1, y2, x)
    private fun line2at(x: Double) = linearApproximate(x2, x3, y2, y3, x)
    private fun line3at(x: Double) = linearApproximate(x3, x4, y3, y4, x)

    private fun scale(value: BigDecimal) = value.setScale(operationScale, RoundingMode.HALF_UP)

    private fun linearApproximate(x1: Double, x2: Double, y1: BigDecimal, y2: BigDecimal, x: Double): BigDecimal {
        val result = y1 + ((y2 - y1) * BigDecimal(x - x1) localDivide BigDecimal(x2 - x1))
        return result.setScale(operationScale, RoundingMode.HALF_UP)
    }

    private infix fun BigDecimal.localDivide(other: BigDecimal) = divide(other, operationScale, RoundingMode.HALF_UP)
}