package old.exchange.binance

import com.dmi.cointrader.app.binance.BinanceConstants
import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.history.Moment
import com.dmi.cointrader.app.history.moment.cachedMoments
import com.dmi.cointrader.app.history.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.dmi.cointrader.app.binance.api.binanceAPI
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class BinanceMomentsSpec : Spec({
    "first moments" - {
        val api = binanceAPI()
        val constants = BinanceConstants()
        val startTime = LocalDateTime.of(2017, 7, 14, 7, 4, 0).toInstant(ZoneOffset.of("+3"))
        val currentTime = MemoryAtom(LocalDateTime.of(2017, 7, 14, 7, 40, 0).toInstant(ZoneOffset.of("+3")))
        val period = Duration.ofMinutes(5)
        val mainCoin = "BTC"
        val altCoins = listOf("ETH", "NEO", "LTC")

        val fs = Jimfs.newFileSystem(Configuration.unix())
        val tradesPath = fs.getPath("old/data/cache/binance/trades/")
        val momentsPath = fs.getPath("old/data/cache/binance/moments")
        Files.createDirectories(tradesPath)
        Files.createDirectories(momentsPath.parent)
        val coinToTrades = coinToCachedBinanceTrades(mainCoin, altCoins, tradesPath, constants, api, currentTime, chunkLoadCount = 20)
        val moments = cachedMoments(startTime, period, altCoins, momentsPath, coinToTrades, currentTime, reloadCount = 1)

        val syncs = object {
            suspend fun first() {
                coinToTrades.forEach { it.sync() }
                moments.sync()

                moments.toList().apply {
                    size shouldBe 8
                    this[0] shouldBe Moment(listOf(   // 1500005340000
                            Candle(0.08000000, 0.08000000, 0.08000000),
                            Candle(0.00375000, 0.00375000, 0.00375000),
                            Candle(0.01900000, 0.01900000, 0.01900000)
                    ))
                    this[1] shouldBe Moment(listOf(  // 1500005640000
                            Candle(0.08640000, 0.08640000, 0.08000100),
                            Candle(0.00257000, 0.00261000, 0.00245000),
                            Candle(0.01909900, 0.01909900, 0.01900000)
                    ))
                    this[2] shouldBe Moment(listOf(  // 1500005940000
                            Candle(0.08562000, 0.08640000, 0.08528900),
                            Candle(0.00257000, 0.00257000, 0.00257000),
                            Candle(0.01909900, 0.01909900, 0.01909900)
                    ))
                    this[3] shouldBe Moment(listOf(  // 1500006240000
                            Candle(0.08600000, 0.08600000, 0.08512800),
                            Candle(0.00257000, 0.00265000, 0.00257000),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[4] shouldBe Moment(listOf(  // 1500006540000
                            Candle(0.08543200, 0.08600000, 0.08525000),
                            Candle(0.00258600, 0.00258600, 0.00253800),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[5] shouldBe Moment(listOf(  // 1500006840000
                            Candle(0.08600000, 0.08600000, 0.08568500),
                            Candle(0.00258700, 0.00259000, 0.00258700),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[6] shouldBe Moment(listOf(   // 1500007140000
                            Candle(0.08618000, 0.08618000, 0.08618000),
                            Candle(0.00254200, 0.00258700, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                    this[7] shouldBe Moment(listOf(   //-1500007200000
                            Candle(0.08618000, 0.08618000, 0.08618000),
                            Candle(0.00254200, 0.00254200, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                }
            }

            suspend fun second() {
                currentTime.set(LocalDateTime.of(2017, 7, 14, 7, 52, 0).toInstant(ZoneOffset.of("+3")))
                coinToTrades.forEach { it.sync() }
                moments.sync()

                moments.toList().apply {
                    size shouldBe 10
                    this[0] shouldBe Moment(listOf(   // 1500005340000
                            Candle(0.08000000, 0.08000000, 0.08000000),
                            Candle(0.00375000, 0.00375000, 0.00375000),
                            Candle(0.01900000, 0.01900000, 0.01900000)
                    ))
                    this[1] shouldBe Moment(listOf(  // 1500005640000
                            Candle(0.08640000, 0.08640000, 0.08000100),
                            Candle(0.00257000, 0.00261000, 0.00245000),
                            Candle(0.01909900, 0.01909900, 0.01900000)
                    ))
                    this[2] shouldBe Moment(listOf(  // 1500005940000
                            Candle(0.08562000, 0.08640000, 0.08528900),
                            Candle(0.00257000, 0.00257000, 0.00257000),
                            Candle(0.01909900, 0.01909900, 0.01909900)
                    ))
                    this[3] shouldBe Moment(listOf(  // 1500006240000
                            Candle(0.08600000, 0.08600000, 0.08512800),
                            Candle(0.00257000, 0.00265000, 0.00257000),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[4] shouldBe Moment(listOf(  // 1500006540000
                            Candle(0.08543200, 0.08600000, 0.08525000),
                            Candle(0.00258600, 0.00258600, 0.00253800),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[5] shouldBe Moment(listOf(  // 1500006840000
                            Candle(0.08600000, 0.08600000, 0.08568500),
                            Candle(0.00258700, 0.00259000, 0.00258700),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[6] shouldBe Moment(listOf(   // 1500007140000
                            Candle(0.08618000, 0.08618000, 0.08618000),
                            Candle(0.00254200, 0.00258700, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                    this[7] shouldBe Moment(listOf(   // 1500007440000
                            Candle(0.08636000, 0.08638000, 0.08618000),
                            Candle(0.00254200, 0.00254200, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                    this[8] shouldBe Moment(listOf(  // 1500007740000
                            Candle(0.08635000, 0.08635000, 0.08631400),
                            Candle(0.00253800, 0.00254200, 0.00253800),
                            Candle(0.01932900, 0.01932900, 0.01932900)
                    ))
                    this[9] shouldBe Moment(listOf(  //-1500007920000
                            Candle(0.08635000, 0.08635000, 0.08635000),
                            Candle(0.00263900, 0.00263900, 0.00263800),
                            Candle(0.01922300, 0.01922300, 0.01922300)
                    ))
                }
            }

            suspend fun third() {
                currentTime.set(LocalDateTime.of(2017, 7, 14, 7, 55, 0).toInstant(ZoneOffset.of("+3")))
                coinToTrades.forEach { it.sync() }
                moments.sync()

                moments.toList().apply {
                    size shouldBe 11
                    this[0] shouldBe Moment(listOf(   // 1500005340000
                            Candle(0.08000000, 0.08000000, 0.08000000),
                            Candle(0.00375000, 0.00375000, 0.00375000),
                            Candle(0.01900000, 0.01900000, 0.01900000)
                    ))
                    this[1] shouldBe Moment(listOf(  // 1500005640000
                            Candle(0.08640000, 0.08640000, 0.08000100),
                            Candle(0.00257000, 0.00261000, 0.00245000),
                            Candle(0.01909900, 0.01909900, 0.01900000)
                    ))
                    this[2] shouldBe Moment(listOf(  // 1500005940000
                            Candle(0.08562000, 0.08640000, 0.08528900),
                            Candle(0.00257000, 0.00257000, 0.00257000),
                            Candle(0.01909900, 0.01909900, 0.01909900)
                    ))
                    this[3] shouldBe Moment(listOf(  // 1500006240000
                            Candle(0.08600000, 0.08600000, 0.08512800),
                            Candle(0.00257000, 0.00265000, 0.00257000),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[4] shouldBe Moment(listOf(  // 1500006540000
                            Candle(0.08543200, 0.08600000, 0.08525000),
                            Candle(0.00258600, 0.00258600, 0.00253800),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[5] shouldBe Moment(listOf(  // 1500006840000
                            Candle(0.08600000, 0.08600000, 0.08568500),
                            Candle(0.00258700, 0.00259000, 0.00258700),
                            Candle(0.01914800, 0.01914800, 0.01914800)
                    ))
                    this[6] shouldBe Moment(listOf(   // 1500007140000
                            Candle(0.08618000, 0.08618000, 0.08618000),
                            Candle(0.00254200, 0.00258700, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                    this[7] shouldBe Moment(listOf(   // 1500007440000
                            Candle(0.08636000, 0.08638000, 0.08618000),
                            Candle(0.00254200, 0.00254200, 0.00254200),
                            Candle(0.01909600, 0.01909600, 0.01909600)
                    ))
                    this[8] shouldBe Moment(listOf(  // 1500007740000
                            Candle(0.08635000, 0.08635000, 0.08631400),
                            Candle(0.00253800, 0.00254200, 0.00253800),
                            Candle(0.01932900, 0.01932900, 0.01932900)
                    ))
                    this[9] shouldBe Moment(listOf(  // 1500008040000
                            Candle(0.08638000, 0.08638000, 0.08635000),
                            Candle(0.00254500, 0.00263900, 0.00254500),
                            Candle(0.01922300, 0.01922300, 0.01922300)

                    ))
                    this[10] shouldBe Moment(listOf(  //-1500008100000
                            Candle(0.08638000, 0.08638000, 0.08635000),
                            Candle(0.00254500, 0.00254500, 0.00254500),
                            Candle(0.01922300, 0.01922300, 0.01922300)
                    ))
                }
            }
        }

        "third sync only" {
            syncs.third()
        }

        "first, second and third" {
            syncs.first()
            syncs.second()
            syncs.third()
        }

        "first than third" {
            syncs.first()
            syncs.third()
        }

        "second than third" {
            syncs.second()
            syncs.third()
        }
    }
})


/*

    Times:

    1500005040000
    1500005340000
    1500005640000
    1500005940000
    1500006240000
    1500006540000
    1500006840000
    1500007140000
    1500007440000
    1500007740000
    1500008040000

    Current times:

    1500005040000
    1500007200000
    1500007920000
    1500008100000


    Trades:

    ETH
    1500004804757 0.08000000 0.04300000
    1500004971477 0.08000000 0.18000000
    1500004979827 0.08000000 0.12600000
    1500005019095 0.08000000 0.21200000
    Candle(0.08000000, 0.08000000, 0.08000000)  // 1500005040000
    1500005045212 0.08000000 0.12500000
    1500005083426 0.08000000 0.04000000
    1500005122638 0.08000000 0.12600000
    1500005129516 0.08000000 0.01500000
    1500005155933 0.08000000 0.69000000
    1500005202292 0.08000000 0.19000000
    1500005216092 0.08000000 0.30800000
    1500005243862 0.08000000 1.08800000
    1500005261435 0.08000000 0.29000000
    1500005265138 0.08000000 0.62000000
    Candle(0.08000000, 0.08000000, 0.08000000)  // 1500005340000
    1500005359150 0.08000100 0.02000000
    1500005408149 0.08000100 0.12800000
    1500005481123 0.08100000 0.16800000
    1500005535712 0.08640000 3.00000000
    1500005542078 0.08600000 0.26300000
    1500005579710 0.08640000 0.26800000
    1500005596924 0.08640000 0.26200000
    1500005638948 0.08640000 0.15000000
    Candle(0.08640000, 0.08640000, 0.08000100)  // 1500005640000
    1500005641786 0.08640000 0.44000000
    1500005746933 0.08528900 0.30000000
    1500005758470 0.08528900 0.10000000
    1500005839607 0.08528900 1.19000000
    1500005842120 0.08528900 4.97400000
    1500005866574 0.08546700 0.01500000
    1500005885550 0.08547900 0.09200000
    1500005896236 0.08547900 2.00000000
    1500005906537 0.08546700 8.40100000
    1500005906537 0.08547900 2.20800000
    1500005906537 0.08559200 2.20100000
    1500005915740 0.08562000 5.31900000
    1500005920187 0.08562000 13.00000000
    1500005920747 0.08562000 0.68100000
    Candle(0.08562000, 0.08640000, 0.08528900)  // 1500005940000
    1500005951185 0.08512900 0.20000000
    1500005992033 0.08512800 12.75000000
    1500006037352 0.08527400 0.27600000
    1500006111021 0.08527400 0.34300000
    1500006111021 0.08600000 0.69700000
    1500006185120 0.08527400 0.11800000
    1500006185120 0.08600000 1.39300000
    1500006196289 0.08600000 0.02000000
    Candle(0.08600000, 0.08600000, 0.08512800)  // 1500006240000
    1500006245891 0.08600000 0.06300000
    1500006261238 0.08600000 1.78000000
    1500006264237 0.08600000 0.03300000
    1500006285742 0.08600000 0.85300000
    1500006345887 0.08525000 0.08000000
    1500006383311 0.08526500 0.99000000
    1500006410809 0.08543000 0.30000000
    1500006417944 0.08543200 0.07300000
    1500006456517 0.08543200 0.18700000
    Candle(0.08543200, 0.08600000, 0.08525000)  // 1500006540000
    1500006544606 0.08568500 0.09100000
    1500006582826 0.08581100 0.16000000
    1500006595818 0.08581100 0.15400000
    1500006605746 0.08581100 0.76100000
    1500006654627 0.08585100 0.30000000
    1500006674284 0.08585100 8.64000000
    1500006727307 0.08600000 8.24200000
    Candle(0.08600000, 0.08600000, 0.08568500)  // 1500006840000
    1500007039848 0.08618000 1.59700000
    1500007042438 0.08618000 0.56600000
    1500007072586 0.08618000 15.34700000
    1500007122558 0.08618000 0.29600000
    1500007124015 0.08618000 0.40400000
    Candle(0.08618000, 0.08618000, 0.08618000)  // 1500007140000
    1500007162359 0.08618000 0.02700000
    Candle(0.08618000, 0.08618000, 0.08618000)  //-1500007200000
    1500007231711 0.08618000 0.10700000
    1500007275258 0.08638000 4.12000000
    1500007356034 0.08634700 0.08700000
    1500007356041 0.08634700 0.89100000
    1500007356048 0.08630700 0.67100000
    1500007356048 0.08631300 0.08700000
    1500007356091 0.08630700 0.08700000
    1500007356099 0.08630700 0.67100000
    1500007356099 0.08634700 0.08700000
    1500007357036 0.08634700 0.22000000
    1500007364035 0.08634700 0.75800000
    1500007380789 0.08634700 0.22000000
    1500007381034 0.08634700 0.75800000
    1500007383489 0.08634700 0.22000000
    1500007383496 0.08633200 0.91800000
    1500007384033 0.08634700 0.75800000
    1500007384049 0.08634700 0.22000000
    1500007402481 0.08631400 0.05200000
    1500007402490 0.08634500 0.35200000
    1500007402505 0.08631400 0.22000000
    1500007416048 0.08623400 0.52900000
    1500007416056 0.08624500 0.79100000
    1500007428036 0.08623400 0.22000000
    1500007428052 0.08623400 0.03300000
    1500007428059 0.08623400 0.49600000
    1500007430590 0.08636000 0.42200000
    Candle(0.08636000, 08638000, 0.08618000)  // 1500007440000
    1500007443364 0.08631400 3.57800000
    1500007466642 0.08631400 0.01600000
    1500007595407 0.08631400 0.02000000
    1500007596037 0.08631400 0.55400000
    1500007596084 0.08631400 0.55400000
    1500007621496 0.08635000 2.52300000
    1500007673447 0.08631800 0.08300000
    1500007673447 0.08633700 0.19700000
    1500007704036 0.08631900 0.55400000
    1500007704044 0.08631900 0.78500000
    1500007734610 0.08635000 2.47700000
    Candle(0.08635000, 0.08635000, 0.08631400)  // 1500007740000
    1500007746162 0.08635000 5.89000000
    1500007856911 0.08635000 0.55400000
    1500007856920 0.08635000 0.78500000
    1500007856926 0.08635000 0.30600000
    1500007856933 0.08635000 0.98400000
    1500007860038 0.08635000 0.55400000
    1500007860046 0.08635000 0.78500000
    1500007860053 0.08635000 0.30600000
    1500007860060 0.08635000 0.98400000
    1500007860104 0.08635000 0.55400000
    1500007860111 0.08635000 0.78500000
    1500007860119 0.08635000 0.30600000
    1500007860127 0.08635000 0.98400000
    1500007872037 0.08635000 0.55400000
    1500007872044 0.08635000 0.78500000
    1500007872052 0.08635000 0.30600000
    1500007872059 0.08635000 0.98400000
    1500007888973 0.08635000 3.79700000
    Candle(0.08635000, 0.08635000, 0.08635000)  //-1500007920000
    1500008002149 0.08638000 0.20000000
    Candle(0.08638000, 0.08638000, 0.08635000)  // 1500008040000
    1500008040036 0.08635000 0.55400000
    1500008040044 0.08635000 0.78500000
    1500008040051 0.08635000 0.30600000
    1500008040058 0.08635000 0.98400000
    1500008040100 0.08635000 0.55400000
    1500008040107 0.08635000 0.78500000
    1500008040114 0.08635000 0.30600000
    1500008040122 0.08635000 0.98400000
    1500008052032 0.08635000 0.55400000
    1500008052040 0.08635000 0.78500000
    1500008052048 0.08635000 0.30600000
    1500008052055 0.08635000 0.98400000
    1500008078383 0.08638000 0.09000000
    Candle(0.08638000, 0.08638000, 0.08635000)  //-1500008100000

    NEO
    1500004832141 0.00375000 20.00000000
    Candle(0.00375000, 0.00375000, 0.00375000)  // 1500005040000
    Candle(0.00375000, 0.00375000, 0.00375000)  // 1500005340000
    1500005396254 0.00261000 0.42000000
    1500005499534 0.00245000 0.42000000
    1500005631074 0.00257000 1.50000000
    Candle(0.00257000, 0.00261000, 0.00245000)  // 1500005640000
    1500005732141 0.00257000 2.00000000
    1500005920297 0.00257000 2.00000000
    Candle(0.00257000, 0.00257000, 0.00257000)  // 1500005940000
    1500005956506 0.00265000 1.93000000
    1500006105257 0.00257000 28.00000000
    1500006182271 0.00257000 2.00000000
    1500006232169 0.00257000 1.00000000
    Candle(0.00257000, 0.00265000, 0.00257000)  // 1500006240000
    1500006240593 0.00253800 72.50000000
    1500006240593 0.00257000 3.50000000
    1500006409445 0.00253800 5.00000000
    1500006435471 0.00258600 712.63000000
    Candle(0.00258600, 0.00258600, 0.00253800)  // 1500006540000
    1500006551862 0.00259000 10.00000000
    1500006702277 0.00259000 50.00000000
    1500006800045 0.00259000 495.80000000
    1500006800045 0.00258700 551.20000000
    Candle(0.00258700, 0.00259000, 0.00258700)  // 1500006840000
    1500006848320 0.00257900 994.56000000
    1500006848320 0.00258700 199.17000000
    1500006866516 0.00256300 4.00000000
    1500007058639 0.00256300 133.64000000
    1500007079277 0.00255000 33.30000000
    1500007079277 0.00254200 444.00000000
    Candle(0.00254200, 0.00258700, 0.00254200)  // 1500007140000
    Candle(0.00254200, 0.00254200, 0.00254200)  //-1500007200000
    1500007310116 0.00254200 7.00000000
    1500007334885 0.00254200 10.00000000
    Candle(0.00254200, 0.00254200, 0.00254200)  // 1500007440000
    1500007468353 0.00254200 2.00000000
    1500007520701 0.00253800 575.00000000
    Candle(0.00253800, 0.00254200, 0.00253800)  // 1500007740000
    1500007745363 0.00263900 1.01000000
    1500007816664 0.00263800 10.00000000
    1500007816664 0.00263900 310.00000000
    Candle(0.00263900, 0.00263900, 0.00263800)  //-1500007920000
    1500007966328 0.00254500 12.00000000
    Candle(0.00254500, 0.00263900, 0.00254500)  // 1500008040000
    Candle(0.00254500, 0.00254500, 0.00254500)  //-1500008100000

    LTC
    Candle(0.01900000, 0.01900000, 0.01900000)  // 1500005040000
    Candle(0.01900000, 0.01900000, 0.01900000)  // 1500005340000
    1500005394457 0.01900000 20.00000000
    1500005536859 0.01909900 1.59000000
    Candle(0.01909900, 0.01909900, 0.01900000)  // 1500005640000
    Candle(0.01909900, 0.01909900, 0.01909900)  // 1500005940000
    1500006066191 0.01914800 1.90000000
    Candle(0.01914800, 0.01914800, 0.01914800)  // 1500006240000
    Candle(0.01914800, 0.01914800, 0.01914800)  // 1500006540000
    Candle(0.01914800, 0.01914800, 0.01914800)  // 1500006840000
    1500007121710 0.01909600 13.75000000
    Candle(0.01909600, 0.01909600, 0.01909600)  // 1500007140000
    Candle(0.01909600, 0.01909600, 0.01909600)  //-1500007200000
    Candle(0.01909600, 0.01909600, 0.01909600)  // 1500007440000
    1500007691579 0.01932900 4.79000000
    Candle(0.01932900, 0.01932900, 0.01932900)  // 1500007740000
    1500007884037 0.01922300 3.87000000
    Candle(0.01922300, 0.01922300, 0.01922300)  //-1500007920000
    Candle(0.01922300, 0.01922300, 0.01922300)  // 1500008040000
    Candle(0.01922300, 0.01922300, 0.01922300)  //-1500008100000
*/