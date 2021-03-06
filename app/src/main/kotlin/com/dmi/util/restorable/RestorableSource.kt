package com.dmi.util.restorable

import arrow.core.Option
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.map
import kotlinx.serialization.Serializable
import com.dmi.util.concurrent.zip
import com.dmi.util.restorable.RestorableSource.Item
import arrow.core.Option.Companion.pure
import arrow.core.Option.Companion.empty
import arrow.core.getOrElse
import kotlinx.coroutines.experimental.channels.*
import java.util.*
import kotlin.collections.ArrayList

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

@Serializable
data class ScanState<out SOURCE_STATE, out R>(val source: SOURCE_STATE, val acc: R)

fun <STATE, T, R> RestorableSource<STATE, T>.scan(
        initial: (first: T) -> R,
        operation: (T, acc: R) -> R
) = object : RestorableSource<ScanState<STATE, R>, R> {
    override fun initial() = this@scan.initial().scan(empty())
    override fun restored(state: ScanState<STATE, R>) = this@scan.restored(state.source).scan(pure(state.acc))

    private fun ReceiveChannel<Item<STATE, T>>.scan(initial: Option<R>) = produce {
        var acc = initial
        consumeEach { item ->
            val accValue = acc
                    .map { operation(item.value, it) }
                    .getOrElse { initial(item.value) }
            send(Item(ScanState(item.state, accValue), accValue))
            acc = pure(accValue)
        }
    }
}

fun <STATE, T, R> RestorableSource<STATE, T>.map(transform: (T) -> R) = object : RestorableSource<STATE, R> {
    override fun initial() = this@map.initial().map(this::transform)
    override fun restored(state: STATE) = this@map.restored(state).map(this::transform)
    private fun transform(item: Item<STATE, T>) = Item(item.state, transform(item.value))
}

fun <STATE, T> RestorableSource<STATE, T>.drop(count: Int) = object : RestorableSource<STATE, T> {
    override fun initial() = this@drop.initial().drop(count)
    override fun restored(state: STATE) = this@drop.restored(state)
}

fun <STATE, T> RestorableSource<STATE, T>.dropWhile(predicate: suspend (T) -> Boolean) = object : RestorableSource<STATE, T> {
    override fun initial() = this@dropWhile.initial().dropWhile { predicate(it.value) }
    override fun restored(state: STATE) = this@dropWhile.restored(state).dropWhile { predicate(it.value) }
}

fun <STATE, T> RestorableSource<STATE, T>.takeWhile(predicate: suspend (T) -> Boolean) = object : RestorableSource<STATE, T> {
    override fun initial() = this@takeWhile.initial().takeWhile { predicate(it.value) }
    override fun restored(state: STATE) = this@takeWhile.restored(state).takeWhile { predicate(it.value) }
}

fun <T> SuspendList<T>.asRestorableSource(bufferSize: Long = 100) = object : RestorableSource<Long, T> {
    override fun initial(): ReceiveChannel<Item<Long, T>> = restored(-1)

    override fun restored(state: Long): ReceiveChannel<Item<Long, T>> = produce {
        var i = state + 1
        channel(i until size(), bufferSize).consumeEach {
            send(Item(i++, it))
        }
    }
}

fun <T> List<T>.asRestorableSource() = object : RestorableSource<Int, T> {
    override fun initial(): ReceiveChannel<Item<Int, T>> = restored(-1)

    override fun restored(state: Int): ReceiveChannel<Item<Int, T>> = produce {
        for (i in state + 1 until this@asRestorableSource.size) {
            send(Item(i, this@asRestorableSource[i]))
        }
    }
}

@Serializable
data class WindowedState<out SOURCE_STATE, out VALUE>(val source: SOURCE_STATE, val lastItems: List<VALUE>)

typealias WindowedItem<SOURCE_STATE, VALUE> = Item<WindowedState<SOURCE_STATE, VALUE>, List<VALUE>>

fun <STATE, VALUE> RestorableSource<STATE, VALUE>.rightWindowed(size: Int) = object : RestorableSource<WindowedState<STATE, VALUE>, List<VALUE>> {
    override fun initial(): ReceiveChannel<WindowedItem<STATE, VALUE>> = produce {
        val items = LinkedList<VALUE>()
        this@rightWindowed.initial().consumeEach {
            items.addLast(it.value)
            if (items.size > size) {
                items.removeFirst()
            }
            val arrayItems = ArrayList(items)
            send(WindowedItem(WindowedState(it.state, arrayItems), arrayItems))
        }
    }

    override fun restored(state: WindowedState<STATE, VALUE>): ReceiveChannel<WindowedItem<STATE, VALUE>> = produce {
        val items = LinkedList(state.lastItems)
        this@rightWindowed.restored(state.source).consumeEach {
            items.addLast(it.value)
            if (items.size > size) {
                items.removeFirst()
            }
            val arrayItems = ArrayList(items)
            send(WindowedItem(WindowedState(it.state, arrayItems), arrayItems))
        }
    }
}