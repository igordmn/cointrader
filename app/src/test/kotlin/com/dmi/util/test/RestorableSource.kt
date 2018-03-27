package com.dmi.util.test

import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.elementAt
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.initialValues(size: Int = Int.MAX_VALUE): List<VALUE> {
    return initial().take(size).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.restoredAfter(index: Int, size: Int = Int.MAX_VALUE): List<VALUE> {
    return restored(stateAt(index)).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.stateAt(index: Int) = initial().elementAt(index).state