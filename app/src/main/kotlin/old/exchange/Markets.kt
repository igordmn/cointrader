package old.exchange

interface Markets {
    fun of(fromCoin: String, toCoin: String): OldMarket?
}