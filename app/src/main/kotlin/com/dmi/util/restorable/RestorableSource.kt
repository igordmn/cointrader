package com.dmi.util.restorable

import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import com.dmi.util.concurrent.zip
import com.dmi.util.restorable.RestorableSource.Item
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.takeWhile

interface RestorableSource<STATE, out VALUE> {
    fun initial(): ReceiveChannel<Item<STATE, VALUE>>
    fun restored(state: STATE): ReceiveChannel<Item<STATE, VALUE>>

    @Serializable
    data class Item<out STATE, out VALUE>(val state: STATE, val value: VALUE)
}

fun <STATE : Any, VALUE> RestorableSource<STATE, VALUE>.initialOrRestored(state: STATE?) = state?.let(::restored) ?: initial()

fun <STATE, VALUE> List<RestorableSource<STATE, VALUE>>.zip() = object : RestorableSource<List<STATE>, List<VALUE>> {
    override fun initial() = this@zip
            .map { it.initial() }
            .zip()
            .map { it.toItem() }

    override fun restored(state: List<STATE>) = this@zip
            .zip(state, RestorableSource<STATE, VALUE>::restored)
            .zip()
            .map { it.toItem() }

    private fun List<Item<STATE, VALUE>>.toItem(): Item<List<STATE>, List<VALUE>> {
        return Item(
                map { it.state },
                map { it.value }
        )
    }
}

fun <STATE, T, R> RestorableSource<STATE, T>.map(transform: (T) -> R) = object : RestorableSource<STATE, R> {
    override fun initial() = this@map.initial().map(this::transform)
    override fun restored(state: STATE) = this@map.restored(state).map(this::transform)
    private fun transform(item: Item<STATE, T>) = Item(item.state, transform(item.value))
}

fun <STATE, T> RestorableSource<STATE, T>.takeWhile(predicate: suspend (T) -> Boolean) = object : RestorableSource<STATE, T> {
    override fun initial() = this@takeWhile.initial().takeWhile { predicate(it.value) }
    override fun restored(state: STATE) = this@takeWhile.restored(state).takeWhile { predicate(it.value) }
}

@Serializable
data class ScanState<out SOURCE_STATE, out R>(val source: SOURCE_STATE, val acc: R)

fun <STATE, T, R> RestorableSource<STATE, T>.scan(
        initial: R,
        operation: (T, acc: R) -> R
) = object : RestorableSource<ScanState<STATE, R>, R> {
    override fun initial() = this@scan.initial().scan(initial)
    override fun restored(state: ScanState<STATE, R>) = this@scan.restored(state.source).scan(state.acc)

    private fun ReceiveChannel<Item<STATE, T>>.scan(initial: R) = produce {
        var acc = initial
        consumeEach {
            acc = operation(it.value, acc)
            send(Item(ScanState(it.state, acc), acc))
        }
    }
}

fun <T> SuspendList<T>.asRestorableSource() = object : RestorableSource<Long, T> {
    override fun initial(): ReceiveChannel<Item<Long, T>> = restored(0)

    override fun restored(state: Long): ReceiveChannel<Item<Long, T>> = produce {
        var i = state
        this@asRestorableSource.channel(i).consumeEach {
            send(Item(i++, it))
        }
    }
}