package exchange.binance

class BinanceConstants {
    val btcMarkets = setOf(
            "ETH", "LTC", "BNB", "NEO", "BCH", "GAS", "HSR", "MCO", "WTC", "LRC", "QTUM",
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

    val standardNameToBinance = mapOf(
            "BCH" to "BCC"
    )

    val binanceNameToStandard = mapOf(
            "BCC" to "BCH"
    )

    fun marketName(fromCoin: String, toCoin: String): String? {
        val fromCoinBinanceName = standardNameToBinance[fromCoin]?: fromCoin
        val toCoinBinanceName = standardNameToBinance[toCoin]?: toCoin
        return when {
            fromCoin == "BTC" && toCoin in btcMarkets -> "${toCoinBinanceName}BTC"
            toCoin == "BTC" && fromCoin in btcReversedMarkets -> "BTC$fromCoinBinanceName"
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