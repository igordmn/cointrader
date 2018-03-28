package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.trade.TradeAssets
import com.dmi.util.lang.minutes
import com.dmi.util.test.Spec
import com.dmi.util.test.instant
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList

class ArchiveSpec : Spec({
    "ETH and NEO assets (reversed markets)" - {
        val fileSystem = Jimfs.newFileSystem(Configuration.unix())

        val newArchive = object {
            suspend operator fun invoke(currentPeriod: Period) = archive(
                    PeriodSpace(instant(1500005040000L), minutes(5)),
                    TradeAssets(main = "BTC", alts = listOf("ETH", "NEO")),
                    binanceExchangeForInfo(),
                    currentPeriod,
                    fileSystem,
                    reloadCount = 1,
                    tradeLoadChunk = 20
            )
        }

        val expectedSpreads = listOf(
                listOf(Spread(0.08640000, 0.08100000), Spread(0.00261000, 0.00245000)),  // 0 1500005040000
                listOf(Spread(0.08640000, 0.08100000), Spread(0.00261000, 0.00245000)),  // 1 1500005340000
                listOf(Spread(0.08640000, 0.08640000), Spread(0.00261000, 0.00257000)),  // 2 1500005640000
                listOf(Spread(0.08640000, 0.08562000), Spread(0.00261000, 0.00257000)),  // 3 1500005940000
                listOf(Spread(0.08600000, 0.08600000), Spread(0.00265000, 0.00257000)),  // 4 1500006240000
                listOf(Spread(0.08543200, 0.08543200), Spread(0.00258600, 0.00253800)),  // 5 1500006540000
                listOf(Spread(0.08600000, 0.08585100), Spread(0.00259000, 0.00258700)),  // 6 1500006840000
                listOf(Spread(0.08618000, 0.08618000), Spread(0.00259000, 0.00254200)),  // 7 1500007140000
                listOf(Spread(0.08636000, 0.08636000), Spread(0.00254200, 0.00254200)),  // 8 1500007440000
                listOf(Spread(0.08635000, 0.08631900), Spread(0.00254200, 0.00253800)),  // 9 1500007740000
                listOf(Spread(0.08638000, 0.08635000), Spread(0.00263900, 0.00254500))   // 10 1500008040000
        )

        "initial" {
            val archive = newArchive(currentPeriod = 10)
            archive.historyAt(0..10).toList() shouldBe expectedSpreads
            archive.historyAt(2..8).toList() shouldBe expectedSpreads.slice(2..8)
            archive.historyAt(0..0).toList() shouldBe expectedSpreads.slice(0..0)
            archive.historyAt(2..2).toList() shouldBe  expectedSpreads.slice(2..2)
            archive.historyAt(10..10).toList() shouldBe  expectedSpreads.slice(10..10)
        }

        "sync once" {
            val archive = newArchive(currentPeriod = 7)
            archive.historyAt(0..7).toList() shouldBe expectedSpreads.slice(0..7)
            archive.sync(currentPeriod = 9)
            archive.historyAt(0..9).toList() shouldBe expectedSpreads.slice(0..9)
            archive.historyAt(9..9).toList() shouldBe expectedSpreads.slice(9..9)
        }

        "sync twice" {
            val archive = newArchive(currentPeriod = 7)
            archive.sync(currentPeriod = 9)
            archive.sync(currentPeriod = 10)
            archive.historyAt(0..10).toList() shouldBe expectedSpreads.slice(0..10)
            archive.historyAt(10..10).toList() shouldBe expectedSpreads.slice(10..10)
        }

        "restored" {
            val archive1 = newArchive(currentPeriod = 3)
            archive1.historyAt(0..3).toList() shouldBe expectedSpreads.slice(0..3)
            archive1.sync(currentPeriod = 9)
            archive1.historyAt(0..9).toList() shouldBe expectedSpreads.slice(0..9)

            val archive2 = newArchive(currentPeriod = 1)
            archive2.historyAt(0..1).toList() shouldBe expectedSpreads.slice(0..1)

            val archive3 = newArchive(currentPeriod = 8)
            archive3.historyAt(0..8).toList() shouldBe expectedSpreads.slice(0..8)
        }

        /*
            Trades

            ETH
            1500004804757 0.08000000 0.04300000 maker-buyer
            1500004971477 0.08000000 0.18000000 maker-buyer
            1500004979827 0.08000000 0.12600000 maker-buyer
            1500005019095 0.08000000 0.21200000 maker-buyer
            Spread(0.08640000, 0.08100000)          // 0 1500005040000
            1500005045212 0.08000000 0.12500000 maker-buyer
            1500005083426 0.08000000 0.04000000 maker-buyer
            1500005122638 0.08000000 0.12600000 maker-buyer
            1500005129516 0.08000000 0.01500000 maker-buyer
            1500005155933 0.08000000 0.69000000 maker-buyer
            1500005202292 0.08000000 0.19000000 maker-buyer
            1500005216092 0.08000000 0.30800000 maker-buyer
            1500005243862 0.08000000 1.08800000 maker-buyer
            1500005261435 0.08000000 0.29000000 maker-buyer
            1500005265138 0.08000000 0.62000000 maker-buyer
            Spread(0.08640000, 0.08100000)          // 1 1500005340000
            1500005359150 0.08000100 0.02000000 maker-buyer
            1500005408149 0.08000100 0.12800000 maker-buyer
            1500005481123 0.08100000 0.16800000 maker-buyer
            1500005535712 0.08640000 3.00000000 maker-seller
            1500005542078 0.08600000 0.26300000 maker-buyer
            1500005579710 0.08640000 0.26800000 maker-seller
            1500005596924 0.08640000 0.26200000 maker-buyer
            1500005638948 0.08640000 0.15000000 maker-buyer
            Spread(0.08640000, 0.08640000)          // 2 1500005640000
            1500005641786 0.08640000 0.44000000 maker-buyer
            1500005746933 0.08528900 0.30000000 maker-buyer
            1500005758470 0.08528900 0.10000000 maker-buyer
            1500005839607 0.08528900 1.19000000 maker-buyer
            1500005842120 0.08528900 4.97400000 maker-buyer
            1500005866574 0.08546700 0.01500000 maker-buyer
            1500005885550 0.08547900 0.09200000 maker-buyer
            1500005896236 0.08547900 2.00000000 maker-buyer
            1500005906537 0.08559200 2.20100000 maker-buyer
            1500005906537 0.08547900 2.20800000 maker-buyer
            1500005906537 0.08546700 8.40100000 maker-buyer
            1500005915740 0.08562000 5.31900000 maker-buyer
            1500005920187 0.08562000 13.00000000 maker-buyer
            1500005920747 0.08562000 0.68100000 maker-buyer
            Spread(0.08640000, 0.08562000)          // 3 1500005940000
            1500005951185 0.08512900 0.20000000 maker-buyer
            1500005992033 0.08512800 12.75000000 maker-buyer
            1500006037352 0.08527400 0.27600000 maker-buyer
            1500006111021 0.08527400 0.34300000 maker-seller
            1500006111021 0.08600000 0.69700000 maker-seller
            1500006185120 0.08527400 0.11800000 maker-seller
            1500006185120 0.08600000 1.39300000 maker-seller
            1500006196289 0.08600000 0.02000000 maker-buyer
            Spread(0.08600000, 0.08600000)          // 4 1500006240000
            1500006245891 0.08600000 0.06300000 maker-buyer
            1500006261238 0.08600000 1.78000000 maker-buyer
            1500006264237 0.08600000 0.03300000 maker-buyer
            1500006285742 0.08600000 0.85300000 maker-buyer
            1500006345887 0.08525000 0.08000000 maker-buyer
            1500006383311 0.08526500 0.99000000 maker-buyer
            1500006410809 0.08543000 0.30000000 maker-buyer
            1500006417944 0.08543200 0.07300000 maker-buyer
            1500006456517 0.08543200 0.18700000 maker-seller
            Spread(0.08543200, 0.08543200)          // 5 1500006540000
            1500006544606 0.08568500 0.09100000 maker-buyer
            1500006582826 0.08581100 0.16000000 maker-buyer
            1500006595818 0.08581100 0.15400000 maker-buyer
            1500006605746 0.08581100 0.76100000 maker-buyer
            1500006654627 0.08585100 0.30000000 maker-buyer
            1500006674284 0.08585100 8.64000000 maker-buyer
            1500006727307 0.08600000 8.24200000 maker-seller
            Spread(0.08600000, 0.08585100)          // 6 1500006840000
            1500007039848 0.08618000 1.59700000 maker-buyer
            1500007042438 0.08618000 0.56600000 maker-buyer
            1500007072586 0.08618000 15.34700000 maker-buyer
            1500007122558 0.08618000 0.29600000 maker-buyer
            1500007124015 0.08618000 0.40400000 maker-buyer
            Spread(0.08618000, 0.08618000)          // 7 1500007140000
            1500007162359 0.08618000 0.02700000 maker-buyer
            1500007231711 0.08618000 0.10700000 maker-buyer
            1500007275258 0.08638000 4.12000000 maker-seller
            1500007356034 0.08634700 0.08700000 maker-buyer
            1500007356041 0.08634700 0.89100000 maker-buyer
            1500007356048 0.08631300 0.08700000 maker-buyer
            1500007356048 0.08630700 0.67100000 maker-buyer
            1500007356091 0.08630700 0.08700000 maker-buyer
            1500007356099 0.08630700 0.67100000 maker-seller
            1500007356099 0.08634700 0.08700000 maker-seller
            1500007357036 0.08634700 0.22000000 maker-buyer
            1500007364035 0.08634700 0.75800000 maker-seller
            1500007380789 0.08634700 0.22000000 maker-buyer
            1500007381034 0.08634700 0.75800000 maker-seller
            1500007383489 0.08634700 0.22000000 maker-buyer
            1500007383496 0.08633200 0.91800000 maker-buyer
            1500007384033 0.08634700 0.75800000 maker-seller
            1500007384049 0.08634700 0.22000000 maker-buyer
            1500007402481 0.08631400 0.05200000 maker-seller
            1500007402490 0.08634500 0.35200000 maker-seller
            1500007402505 0.08631400 0.22000000 maker-buyer
            1500007416048 0.08623400 0.52900000 maker-seller
            1500007416056 0.08624500 0.79100000 maker-seller
            1500007428036 0.08623400 0.22000000 maker-buyer
            1500007428052 0.08623400 0.03300000 maker-seller
            1500007428059 0.08623400 0.49600000 maker-seller
            1500007430590 0.08636000 0.42200000 maker-buyer
            Spread(0.08636000, 0.08636000)          // 8 1500007440000
            1500007443364 0.08631400 3.57800000 maker-seller
            1500007466642 0.08631400 0.01600000 maker-buyer
            1500007595407 0.08631400 0.02000000 maker-buyer
            1500007596037 0.08631400 0.55400000 maker-buyer
            1500007596084 0.08631400 0.55400000 maker-buyer
            1500007621496 0.08635000 2.52300000 maker-seller
            1500007673447 0.08633700 0.19700000 maker-buyer
            1500007673447 0.08631800 0.08300000 maker-buyer
            1500007704036 0.08631900 0.55400000 maker-buyer
            1500007704044 0.08631900 0.78500000 maker-buyer
            1500007734610 0.08635000 2.47700000 maker-seller
            Spread(0.08635000, 0.08631900)          // 9 1500007740000
            1500007746162 0.08635000 5.89000000 maker-buyer
            1500007856911 0.08635000 0.55400000 maker-buyer
            1500007856920 0.08635000 0.78500000 maker-buyer
            1500007856926 0.08635000 0.30600000 maker-buyer
            1500007856933 0.08635000 0.98400000 maker-buyer
            1500007860038 0.08635000 0.55400000 maker-buyer
            1500007860046 0.08635000 0.78500000 maker-buyer
            1500007860053 0.08635000 0.30600000 maker-buyer
            1500007860060 0.08635000 0.98400000 maker-buyer
            1500007860104 0.08635000 0.55400000 maker-buyer
            1500007860111 0.08635000 0.78500000 maker-buyer
            1500007860119 0.08635000 0.30600000 maker-buyer
            1500007860127 0.08635000 0.98400000 maker-buyer
            1500007872037 0.08635000 0.55400000 maker-buyer
            1500007872044 0.08635000 0.78500000 maker-buyer
            1500007872052 0.08635000 0.30600000 maker-buyer
            1500007872059 0.08635000 0.98400000 maker-buyer
            1500007888973 0.08635000 3.79700000 maker-buyer
            1500008002149 0.08638000 0.20000000 maker-seller
            Spread(0.08638000, 0.08635000)          // 10 1500008040000


            NEO
            1500004832141 0.00375000 20.00000000 maker-seller
            Spread(0.00261000, 0.00245000)          // 0 1500005040000
            Spread(0.00261000, 0.00245000)          // 1 1500005340000
            1500005396254 0.00261000 0.42000000 maker-seller
            1500005499534 0.00245000 0.42000000 maker-buyer
            1500005631074 0.00257000 1.50000000 maker-buyer
            Spread(0.00261000, 0.00257000)          // 2 1500005640000
            1500005732141 0.00257000 2.00000000 maker-buyer
            1500005920297 0.00257000 2.00000000 maker-buyer
            Spread(0.00261000, 0.00257000)          // 3 1500005940000
            1500005956506 0.00265000 1.93000000 maker-seller
            1500006105257 0.00257000 28.00000000 maker-buyer
            1500006182271 0.00257000 2.00000000 maker-buyer
            1500006232169 0.00257000 1.00000000 maker-buyer
            Spread(0.00265000, 0.00257000)          // 4 1500006240000
            1500006240593 0.00257000 3.50000000 maker-buyer
            1500006240593 0.00253800 72.50000000 maker-buyer
            1500006409445 0.00253800 5.00000000 maker-buyer
            1500006435471 0.00258600 712.63000000 maker-seller
            Spread(0.00258600, 0.00253800)          // 5 1500006540000
            1500006551862 0.00259000 10.00000000 maker-buyer
            1500006702277 0.00259000 50.00000000 maker-buyer
            1500006800045 0.00259000 495.80000000 maker-buyer
            1500006800045 0.00258700 551.20000000 maker-buyer
            Spread(0.00259000, 0.00258700)          // 6 1500006840000
            1500006848320 0.00258700 199.17000000 maker-buyer
            1500006848320 0.00257900 994.56000000 maker-buyer
            1500006866516 0.00256300 4.00000000 maker-buyer
            1500007058639 0.00256300 133.64000000 maker-buyer
            1500007079277 0.00255000 33.30000000 maker-buyer
            1500007079277 0.00254200 444.00000000 maker-buyer
            Spread(0.00259000, 0.00254200)          // 7 1500007140000
            1500007310116 0.00254200 7.00000000 maker-seller
            1500007334885 0.00254200 10.00000000 maker-seller
            Spread(0.00254200, 0.00254200)          // 8 1500007440000
            1500007468353 0.00254200 2.00000000 maker-seller
            1500007520701 0.00253800 575.00000000 maker-buyer
            Spread(0.00254200, 0.00253800)          // 9 1500007740000
            1500007745363 0.00263900 1.01000000 maker-seller
            1500007816664 0.00263800 10.00000000 maker-seller
            1500007816664 0.00263900 310.00000000 maker-seller
            1500007966328 0.00254500 12.00000000 maker-buyer
            Spread(0.00263900, 0.00254500)          // 10 1500008040000
        */
    }

    "USDT asset (normal market)" {

    }
})