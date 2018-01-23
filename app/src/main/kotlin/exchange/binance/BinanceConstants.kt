package exchange.binance

class BinanceConstants {
    val btcMarkets = setOf(
            "BCD", "POE", "NEO", "BTG", "FUN", "LRC", "TRX", "TRIG",
            "APPC", "XVG", "GTO", "BCH", "BQX", "REQ", "CDT", "QSP", "QTUM",
            "OST", "MDA", "ETC", "ADA", "XMR", "XRP", "VIBE", "DASH", "BTS",
            "KNC", "ENJ", "SUB", "WABI", "LTC", "ZRX", "POWR", "ENG", "ETH",
            "BNB", "AMB", "LSK", "BRD", "ICX", "OMG", "LINK", "ELF", "MCO",
            "HSR", "IOTA", "EOS", "STRAT", "AION", "TNB", "XLM", "MANA", "WTC",
            "VEN", "CTR", "CND", "LEND", "TNT"
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