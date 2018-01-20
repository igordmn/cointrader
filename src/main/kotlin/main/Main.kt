package main

import main.info.printTopCoins
import main.real.realTrade
import main.test.back.backTest
import main.test.forward.forwardTest

fun main(args: Array<String>) {
    val firstArg = if (args.isNotEmpty()) args[0] else null

    when (firstArg) {
        "back_test" -> backTest()
        "forward_test" -> forwardTest()
        "real_trade" -> realTrade()
        "print_top_coins" -> printTopCoins()
    }
}