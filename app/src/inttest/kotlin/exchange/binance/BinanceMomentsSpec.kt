package exchange.binance

import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.test.Spec
import com.dmi.util.test.instant
import exchange.binance.api.binanceAPI
import main.test.Config
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceMomentsSpec : Spec({
    val api = binanceAPI()
    val constants = BinanceConstants()
    val startTime = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3"))
    val currentTime = MemoryAtom(LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3")))
    val period = Duration.ofSeconds(10)
    val mainCoin = "BTC"
    val altCoins = listOf("ETH", "NEO", "LTC")

    val coinToTrades = coinToCachedBinanceTrades(mainCoin, altCoins, Paths.get("data/cache/binance"), constants, api, currentTime)
    val moments = cachedMoments(startTime, period, altCoins, Paths.get("data/cache/binance/moments"), coinToTrades, currentTime)
//    currentTime.sync()
    coinToTrades.forEach { it.sync() }
    moments.sync()
})