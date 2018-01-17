package util.test

import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Result
import java.math.BigDecimal

fun <T : Comparable<T>> between(a: T, b: T): Matcher<T> = object : Matcher<T> {
    override fun test(value: T) = Result(value in a..b, "$value is between ($a, $b)")
}

infix fun BigDecimal.plusOrMinus(tolerance: Double): ToleranceMatcher = ToleranceMatcher(this, tolerance)

class ToleranceMatcher(
        private val expected: BigDecimal,
        private val tolerance: Double
) : Matcher<BigDecimal> {
    override fun test(value: BigDecimal): Result {
        if (tolerance == 0.0)
            println("[WARN] When comparing doubles consider using tolerance, eg: a shouldBe b plusOrMinus c")
        val diff = (value - expected).abs()
        return Result(diff <= BigDecimal(tolerance), "$value should be equal to $expected")
    }

    infix fun plusOrMinus(tolerance: Double): ToleranceMatcher = ToleranceMatcher(expected, tolerance)
}