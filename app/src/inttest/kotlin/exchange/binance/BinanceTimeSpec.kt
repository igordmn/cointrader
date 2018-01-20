package exchange.binance

import exchange.binance.api.binanceAPI
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import util.concurrent.delay
import util.test.between
import java.time.Duration

class BinanceTimeSpec : StringSpec({
    val api = binanceAPI()
    val time = BinanceTime(api)

    "current time diff after delay" {
        runBlocking {
            val delay = Duration.ofSeconds(1)
            val maxDiff = Duration.ofSeconds(3)

            val serverTime1 = time.current()
            delay(delay)
            val serverTime2 = time.current()

            val diff = Duration.between(serverTime1, serverTime2)
            println("delay: $delay   diff: $diff")

            diff.abs() shouldBe between(Duration.ZERO, maxDiff)
        }
    }
})