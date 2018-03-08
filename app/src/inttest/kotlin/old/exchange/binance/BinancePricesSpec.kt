package old.exchange.binance

import old.exchange.binance.api.binanceAPI
import old.exchange.binance.market.BinanceMarketPrice
import io.kotlintest.matchers.beLessThan
import io.kotlintest.matchers.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.concurrent.delay
import com.dmi.util.math.max
import java.math.BigDecimal
import java.time.Duration

class BinancePricesSpec : StringSpec({
    val marketName = BinanceConstants().marketName("USDT", "BTC")!!
    val prices = BinanceMarketPrice(marketName, binanceAPI)

    "get current price multiple times" {
        fun diff(price1: BigDecimal, price2: BigDecimal): Double {
            val max = max(price1, price2).toDouble()
            val min = max(price1, price2).toDouble()
            return max / min
        }

        runBlocking {
            val delay = Duration.ofMillis(300)
            val maxDiff = 2.0

            val price1 = prices.current()
            delay(delay)
            val price2 = prices.current()
            delay(delay)
            val price3 = prices.current()

            println("delay: $delay   price1: $price1   price2: $price2   price3: $price3")

            diff(price1, price2) should beLessThan(maxDiff)
            diff(price2, price3) should beLessThan(maxDiff)
        }
    }
})