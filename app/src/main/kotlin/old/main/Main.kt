package old.main

import old.main.info.printTopCoins
import old.main.real.realTrade
import old.main.test.back.backTest
import old.main.test.forward.forwardTest

fun main(args: Array<String>) {
    val firstArg = if (args.isNotEmpty()) args[0] else null
    when (firstArg) {
        "backTest" -> backTest()
        "forwardTest" -> forwardTest()
        "realTrade" -> realTrade()
        "printTopCoins" -> printTopCoins()
    }
}