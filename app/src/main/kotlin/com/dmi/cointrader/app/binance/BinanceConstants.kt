package com.dmi.cointrader.app.binance

class BinanceConstants {
    val btcMarkets = setOf(
            "ETH", "LTC", "BNB", "NEO", "BCC", "GAS", "HSR", "MCO", "WTC", "LRC", "QTUM",
            "YOYO", "OMG", "ZRX", "STRAT", "SNGLS", "BQX", "KNC", "FUN", "SNM", "IOTA",
            "LINK", "XVG", "CTR", "SALT", "MDA", "MTL", "SUB", "EOS", "SNT", "ETC",
            "MTH", "ENG", "DNT", "ZEC", "BNT", "AST", "DASH", "OAX", "ICN",
            "BTG", "EVX", "REQ", "VIB", "TRX", "POWR", "ARK", "XRP", "MOD",
            "ENJ", "STORJ", "VEN", "KMD", "RCN", "NULS", "RDN", "XMR", "DLT",
            "AMB", "BAT", "BCPT", "ARN", "GVT", "CDT", "GXS", "POE", "QSP",
            "BTS", "XZC", "LSK", "TNT", "FUEL", "MANA", "BCD", "DGD", "ADX",
            "ADA", "PPT", "CMT", "XLM", "CND", "LEND", "WABI", "TNB", "WAVES",
            "GTO", "ICX", "OST", "ELF", "AION", "NEBL", "BRD", "EDO", "WINGS",
            "NAV", "LUN", "TRIG", "APPC", "VIBE", "RLC", "INS", "PIVX", "IOST",
            "NANO"
    )

    val btcReversedMarkets = setOf("USDT")

    fun marketName(fromAsset: String, toAsset: String): String? {
        return when {
            fromAsset == "BTC" && toAsset in btcMarkets -> "${toAsset}BTC"
            toAsset == "BTC" && fromAsset in btcReversedMarkets -> "BTC$fromAsset"
            else -> null
        }
    }

    fun marketInfo(mainCoin: String, coin: String): MarketInfo {
        val name = marketName(coin, mainCoin)
        val reversedName = marketName(mainCoin, coin)

        return when {
            name != null -> MarketInfo(coin, name, false)
            reversedName != null -> MarketInfo(coin, reversedName, true)
            else -> throw UnsupportedOperationException()
        }
    }
}

data class MarketInfo(val coin: String, val name: String, val isReversed: Boolean)