package exchange.binance

class BinanceConstants {
    val btcMarkets = setOf(
            "TRX", "ETH", "NEO", "XRP", "BNB", "VEN", "EOS", "CND", "ICX",
            "BCD", "ADA", "XVG", "WTC", "BCH", "ELF", "XLM", "LTC", "VIBE",
            "INS", "NEBL", "POE", "APPC", "IOTA", "HSR", "ETC", "QTUM",
            "ARN", "GAS", "BTG", "TNB", "TRIG", "XMR", "LEND", "BTS",
            "SUB", "ZRX", "OMG", "QSP", "LRC", "GTO", "BRD", "WABI",
            "STRAT", "FUN", "CDT", "KNC", "REQ", "LSK", "AION", "ENG",
            "OST", "MANA", "LINK", "POWR", "BCPT", "FUEL", "ZEC", "BQX",
            "DASH", "ENJ", "CTR", "BAT", "SALT", "MCO", "AST"
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
}