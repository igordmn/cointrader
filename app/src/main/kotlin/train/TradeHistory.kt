package train

interface TradeHistory {
    val size: Int

    fun subHistory(start: Int, end: Int): TradeHistory


    
}