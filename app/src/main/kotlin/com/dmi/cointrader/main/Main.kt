package com.dmi.cointrader.main

import com.dmi.cointrader.app.info.printTopCoins

fun main(args: Array<String>) {
    when (args[0]) {
        "printTopCoins" -> printTopCoins()
    }
}