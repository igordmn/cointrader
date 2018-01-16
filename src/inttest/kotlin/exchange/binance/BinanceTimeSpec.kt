package exchange.binance

import com.binance.api.client.BinanceApiClientFactory
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import util.concurrent.delay
import util.test.between
import java.time.Duration

class BinanceTimeSpec : StringSpec({
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val time = BinanceTime(client)

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