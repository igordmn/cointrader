package com.dmi.cointrader.main

import com.dmi.cointrader.app.info.printTopCoins

fun main(args: Array<String>) {
    val firstArg = if (args.isNotEmpty()) args[0] else null
    when (firstArg) {
        "printTopCoins" -> printTopCoins()
    }
}