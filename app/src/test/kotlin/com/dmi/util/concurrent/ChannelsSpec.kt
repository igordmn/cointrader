package com.dmi.util.concurrent

import com.dmi.util.test.Spec
import com.dmi.util.test.channelOf
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList

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
                        channelOf<Int>()
                ).zip(5).toList() shouldBe emptyList<Int>()
            }
        }

        "chunkedBy" - {
            "simple" {
                channelOf(1, 1, 2, 2, 2, 3)
                        .chunkedBy { it }
                        .toList() shouldBe listOf(
                        Pair(1, listOf(1, 1)),
                        Pair(2, listOf(2, 2, 2)),
                        Pair(3, listOf(3))
                )
            }

            "empty" {
                channelOf<Int>()
                        .chunkedBy { it }
                        .toList() shouldBe emptyList<Int>()
            }
        }

        "withPrevious" - {
            "empty" {
                channelOf<Int>().withPrevious(1).toList() shouldBe emptyList<Int>()
            }

            "single" {
                channelOf(1).withPrevious(1).toList() shouldBe listOf(1 to null)
            }

            "multiple" {
                channelOf(1, 2, 3).withPrevious(1).toList() shouldBe listOf(
                        1 to null,
                        2 to 1,
                        3 to 2
                )
            }

            "with step 2" {
                channelOf(1, 2, 3, 4).withPrevious(2).toList() shouldBe listOf(
                        1 to null,
                        2 to null,
                        3 to 1,
                        4 to 2
                )
            }

            "zero previous" {
                channelOf(1, 2, 3, 4).withPrevious(0).toList() shouldBe listOf(
                        1 to 1,
                        2 to 2,
                        3 to 3,
                        4 to 4
                )
            }
        }
    }
}
