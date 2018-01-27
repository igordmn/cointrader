package main

import main.info.printTopCoins
import main.real.realTrade
import main.test.back.backTest
import main.test.forward.forwardTest

fun main(args: Array<String>) {
    val firstArg = if (args.isNotEmpty()) args[0] else null
    when (firstArg) {
        "backTest" -> backTest()
        "forwardTest" -> forwardTest()
        "realTrade" -> realTrade()
        "printTopCoins" -> printTopCoins()
    }
}