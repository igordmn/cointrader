package test

import com.github.jnidzwetzki.bitfinex.v2.entity.Timeframe
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCandlestickSymbol
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCurrencyPair
import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker



fun main(args: Array<String>) {
    val bitfinexApiBroker = BitfinexApiBroker()
    bitfinexApiBroker.connect()

    for (pair in BitfinexCurrencyPair.values()) {
        if (pair.currency1 != "BTC" && pair.currency2 != "BTC")
            continue

        val symbol = BitfinexCandlestickSymbol(pair, Timeframe.MINUTES_1)


        val quoteManager = bitfinexApiBroker.quoteManager
        quoteManager.registerCandlestickCallback(symbol) { symbol, tick ->
            System.out.println("" + System.currentTimeMillis() +  "   " + symbol + "    " + tick)
        }
        quoteManager.subscribeCandles(symbol)
    }


//    quoteManager.removeCandlestickCallback(symbol, callback)
//    quoteManager.unsubscribeCandles(symbol)
}