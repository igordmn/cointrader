package com.dmi.util.concurrent

import com.dmi.util.test.Spec
import com.dmi.util.test.channelOf
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList

class CoroutinesSpec : Spec() {
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

        "insertFirst" - {
            "simple" {
                channelOf(4, 5, 6).insertFirst {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe listOf(2, 3, 4, 5, 6)
            }

            "into single" {
                channelOf(1).insertFirst {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe listOf(-1, 0, 1)
            }

            "into empty" {
                channelOf<Int>().insertFirst {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe emptyList<Int>()
            }

            "insert empty" {
                channelOf(4, 5, 6).insertFirst {
                    emptyList()
                }.toList() shouldBe listOf(4, 5, 6)
            }

            "insert empty into empty" {
                channelOf<Int>().insertFirst {
                    emptyList()
                }.toList() shouldBe emptyList<Int>()
            }
        }

        "insertLast" - {
            "simple" {
                channelOf(4, 5, 6).insertLast {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe listOf(4, 5, 6, 4, 5)
            }

            "into single" {
                channelOf(1).insertLast {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe listOf(1, -1, 0)
            }

            "into empty" {
                channelOf<Int>().insertLast {
                    listOf(it - 2, it - 1)
                }.toList() shouldBe emptyList<Int>()
            }

            "insert empty" {
                channelOf(4, 5, 6).insertLast {
                    emptyList()
                }.toList() shouldBe listOf(4, 5, 6)
            }

            "insert empty into empty" {
                channelOf<Int>().insertLast {
                    emptyList()
                }.toList() shouldBe emptyList<Int>()
            }
        }

        "insertBetween" - {
            "simple" {
                channelOf(4, 9, 20).insertBetween { previous, next ->
                    listOf(previous + 1, next - 1)
                }.toList() shouldBe listOf(4, 5, 8, 9, 10, 19, 20)
            }

            "into single" {
                channelOf(2).insertBetween { previous, next ->
                    listOf(previous + 1, next - 1)
                }.toList() shouldBe listOf(2)
            }

            "into empty" {
                channelOf<Int>().insertBetween { previous, next ->
                    listOf(previous + 1, next - 1)
                }.toList() shouldBe emptyList<Int>()
            }

            "insert empty" {
                channelOf(4, 5, 6).insertBetween { previous, next ->
                    emptyList()
                }.toList() shouldBe listOf(4, 5, 6)
            }

            "insert empty into empty" {
                channelOf<Int>().insertBetween { previous, next ->
                    emptyList()
                }.toList() shouldBe emptyList<Int>()
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
    }
}
