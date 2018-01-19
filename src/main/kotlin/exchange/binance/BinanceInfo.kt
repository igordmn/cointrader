package exchange.binance

class BinanceInfo {
    val btcMarkets = setOf(
            "ETH", "XRP", "IOTA", "XVG", "BCH", "TRX", "LTC",
            "NEO", "ADA", "EOS", "QTUM", "ETC", "DASH", "HSR", "VEN",
            "BNB", "POWR", "POE", "MANA", "MCO", "QSP", "STRAT", "WTC",
            "OMG", "SNT", "BTS", "XMR", "LSK", "ZEC", "SALT", "REQ",
            "STORJ", "YOYO", "LINK", "CTR", "BTG", "ENG", "VIB",
            "MTL", "TRX", "ETH", "NEO", "XRP", "BCD", "ADA", "ICX", "CND", "XVG", "XLM", "WTC",
            "NEBL", "INS", "HSR", "POE", "GAS", "ETC", "QTUM", "TRIG",
            "TNB", "LEND", "QSP", "XMR", "BTS", "OMG", "STRAT", "LSK",
            "FUN", "MANA", "FUEL", "AION", "ENJ", "MCO", "CDT"
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