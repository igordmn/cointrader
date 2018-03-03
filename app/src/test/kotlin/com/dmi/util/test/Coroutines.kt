package com.dmi.util.test

import io.kotlintest.TestCase
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.runBlocking

fun <T> channelOf(vararg values: T): ReceiveChannel<T> = values.toList().asReceiveChannel()