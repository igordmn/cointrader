package com.dmi.util.concurrent

import com.dmi.util.test.Spec
import com.dmi.util.test.channelOf
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking

class ChannelsSpec : Spec() {
    init {
        "chunked" - {
            "simple" {
                channelOf(1, 2, 3, 4, 5, 6, 7).chunked(2).toList() shouldBe listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6), listOf(7))
            }

            "min size" {
                channelOf(1, 2, 3).chunked(1).toList() shouldBe listOf(listOf(1), listOf(2), listOf(3))
            }
        }

        "zip" - {
            "same size" {
                listOf(
                        channelOf(1, 2, 3, 4, 5, 6, 7),
                        channelOf(10, 20, 30, 40, 50, 60, 70),
                        channelOf(100, 200, 300, 400, 500, 600, 700)
                ).zip(5).toList() shouldBe listOf(
                        listOf(1, 10, 100),
                        listOf(2, 20, 200),
                        listOf(3, 30, 300),
                        listOf(4, 40, 400),
                        listOf(5, 50, 500),
                        listOf(6, 60, 600),
                        listOf(7, 70, 700)
                )
            }

            "different size" {
                listOf(
                        channelOf(1, 2, 3, 4),
                        channelOf(10, 20, 30, 40, 50, 60, 70),
                        channelOf(100, 200, 300, 400, 500, 600)
                ).zip(5).toList() shouldBe listOf(
                        listOf(1, 10, 100),
                        listOf(2, 20, 200),
                        listOf(3, 30, 300),
                        listOf(4, 40, 400)
                )
            }

            "single list" {
                listOf(
                        channelOf(1, 2, 3, 4)
                ).zip(5).toList() shouldBe listOf(
                        listOf(1),
                        listOf(2),
                        listOf(3),
                        listOf(4)
                )
            }

            "empty channel" {
                listOf(
                        channelOf<Int>()
                ).zip(5).toList() shouldBe emptyList<Int>()
            }

            "empty channel with non-empty" {
                listOf(
                        channelOf(1, 2, 3, 4),
                        channelOf()
                ).zip(5).toList() shouldBe emptyList<Int>()
            }
        }

        "withPrevious" - {
            infix fun Int?.andNext(other: Int?) = CurrentAndPrevious(this, other)

            "empty" {
                channelOf<Int>().withPrevious(1).toList() shouldBe emptyList<Int>()
            }

            "single" {
                channelOf(1).withPrevious(1).toList() shouldBe listOf(1 andNext null)
            }

            "multiple" {
                channelOf(1, 2, 3).withPrevious(1).toList() shouldBe listOf(
                        1 andNext null,
                        2 andNext 1,
                        3 andNext 2
                )
            }

            "with step 2" {
                channelOf(1, 2, 3, 4).withPrevious(2).toList() shouldBe listOf(
                        1 andNext null,
                        2 andNext null,
                        3 andNext 1,
                        4 andNext 2
                )
            }

            "zero previous" {
                channelOf(1, 2, 3, 4).withPrevious(0).toList() shouldBe listOf(
                        1 andNext 1,
                        2 andNext 2,
                        3 andNext 3,
                        4 andNext 4
                )
            }
        }

        "windowed" - {
            "empty" {
                channelOf<Int>().windowed(1, 1).toList() shouldBe emptyList<List<Int>>()
                channelOf<Int>().windowed(1, 2).toList() shouldBe emptyList<List<Int>>()
                channelOf<Int>().windowed(2, 1).toList() shouldBe emptyList<List<Int>>()
            }

            "single" {
                channelOf(1).windowed(1, 1).toList() shouldBe listOf(listOf(1))
                channelOf(1).windowed(1, 2).toList() shouldBe listOf(listOf(1))
                channelOf(1).windowed(2, 1).toList() shouldBe emptyList<List<Int>>()
            }

            "multiple" {
                channelOf(1, 2, 3, 4, 5).windowed(1, 1).toList() shouldBe listOf(listOf(1), listOf(2), listOf(3), listOf(4), listOf(5))
                channelOf(1, 2, 3, 4, 5).windowed(1, 2).toList() shouldBe listOf(listOf(1), listOf(3), listOf(5))
                channelOf(1, 2, 3, 4, 5).windowed(2, 1).toList() shouldBe listOf(listOf(1, 2), listOf(2, 3), listOf(3, 4), listOf(4, 5))
                channelOf(1, 2, 3, 4, 5).windowed(2, 2).toList() shouldBe listOf(listOf(1, 2), listOf(3, 4))
                channelOf(1, 2, 3, 4, 5).windowed(3, 2).toList() shouldBe listOf(listOf(1, 2, 3), listOf(3, 4, 5))
                channelOf(1, 2, 3, 4, 5).windowed(4, 2).toList() shouldBe listOf(listOf(1, 2, 3, 4))
                channelOf(1, 2, 3, 4, 5, 6).windowed(5, 1).toList() shouldBe listOf(listOf(1, 2, 3, 4, 5), listOf(2, 3, 4, 5, 6))
                channelOf(1, 2, 3, 4, 5, 6).windowed(6, 1).toList() shouldBe listOf(listOf(1, 2, 3, 4, 5, 6))
            }

            "bug1" {
                (1805251..1810890).asReceiveChannel().windowed(2400, 30).toList().size shouldBe 109
            }
        }
    }
}
