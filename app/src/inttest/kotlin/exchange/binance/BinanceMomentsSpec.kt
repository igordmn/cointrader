package exchange.binance

import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import exchange.binance.api.binanceAPI
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceMomentsSpec : Spec({
    val api = binanceAPI()
    val constants = BinanceConstants()
    val startTime = LocalDateTime.of(2017, 7, 14, 4, 0, 0).toInstant(ZoneOffset.of("+3"))
    val currentTime = MemoryAtom(LocalDateTime.of(2017, 7, 14, 4, 39, 0).toInstant(ZoneOffset.of("+3")))
    val period = Duration.ofMinutes(5)
    val mainCoin = "BTC"
    val altCoins = listOf("ETH", "NEO", "LTC")

    val fs = Jimfs.newFileSystem(Configuration.unix())
    val coinToTrades = coinToCachedBinanceTrades(mainCoin, altCoins, fs.getPath("data/cache/binance"), constants, api, currentTime, chunkLoadCount = 5)
    val moments = cachedMoments(startTime, period, altCoins, fs.getPath("data/cache/binance/moments"), coinToTrades, currentTime)

    coinToTrades.forEach { it.sync() }
    moments.sync()
    println("1")
    println(coinToTrades.map { it.toList() })
    println(moments.toList())

    currentTime.set(LocalDateTime.of(2017, 7, 14, 4, 52, 0).toInstant(ZoneOffset.of("+3")))
    coinToTrades.forEach { it.sync() }
    moments.sync()
    println("2")
    println(coinToTrades.map { it.toList() })
    println(moments.toList())

    currentTime.set(LocalDateTime.of(2017, 7, 14, 4, 55, 0).toInstant(ZoneOffset.of("+3")))
    coinToTrades.forEach { it.sync() }
    moments.sync()
    println("3")
    println(coinToTrades.map { it.toList() })
    println(moments.toList())
})