package exchange.binance

class BinanceInfo {
    val btcMarkets = listOf(
            "ETH", "XRP", "IOTA", "XVG", "BCH", "TRX", "LTC",
            "NEO", "ADA", "EOS", "QTUM", "ETC", "DASH", "HSR", "VEN",
            "BNB", "POWR", "POE", "MANA", "MCO", "QSP", "STRAT", "WTC",
            "OMG", "SNT", "BTS", "XMR", "LSK", "ZEC", "SALT", "REQ",
            "STORJ", "YOYO", "LINK", "CTR", "BTG", "ENG", "VIB",
            "MTL"
    )

    val btcReversedMarkets = listOf("USDT")

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
            fromCoin == "BTC" && toCoin in btcMarkets -> "{$toCoinBinanceName}BTC"
            toCoin == "BTC" && fromCoin in btcReversedMarkets -> "BTC$fromCoinBinanceName"
            else -> null
        }
    }
}