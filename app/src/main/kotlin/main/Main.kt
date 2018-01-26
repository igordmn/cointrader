package main

import exchange.binance.market.binanceCachePath
import main.info.printTopCoins
import main.real.realTrade
import main.test.back.backTest
import main.test.forward.forwardTest
import java.nio.file.Files

fun main(args: Array<String>) {
    println(System.getenv("PATH"))
    Files.createDirectories(binanceCachePath)

    val firstArg = if (args.isNotEmpty()) args[0] else null

    when (firstArg) {
        "backTest" -> backTest()
        "forwardTest" -> forwardTest()
        "realTrade" -> realTrade()
        "printTopCoins" -> printTopCoins()
    }
}