package util.test

import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Result

fun <T : Comparable<T>> between(a: T, b: T): Matcher<T> = object : Matcher<T> {
    override fun test(value: T) = Result(value in a..b, "$value is between ($a, $b)")
}