package exchange

interface Markets {
    fun of(fromCoin: String, toCoin: String): Market?
}