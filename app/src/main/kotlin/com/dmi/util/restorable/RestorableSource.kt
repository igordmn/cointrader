package com.dmi.util.restorable

import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import com.dmi.util.concurrent.zip
import com.dmi.util.restorable.RestorableSource.Item
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce

interface RestorableSource<STATE, out VALUE> {
    fun initial(): ReceiveChannel<Item<STATE, VALUE>>
    fun restored(state: STATE): ReceiveChannel<Item<STATE, VALUE>>

    @Serializable
    data class Item<out STATE, out VALUE>(val state: STATE, val value: VALUE)
}

fun <STATE : Any, VALUE> RestorableSource<STATE, VALUE>.initialOrRestored(state: STATE?) = state?.let(::restored) ?: initial()

fun <STATE : Any, VALUE> List<RestorableSource<STATE, VALUE>>.zip() = object : RestorableSource<List<STATE>, List<VALUE>> {
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

fun <STATE : Any, IN, OUT> RestorableSource<STATE, IN>.map(transform: (IN) -> OUT) = object : RestorableSource<STATE, OUT> {
    override fun initial() = this@map.initial().map(this::transform)
    override fun restored(state: STATE) = this@map.restored(state).map(this::transform)
    private fun transform(item: Item<STATE, IN>) = Item(item.state, transform(item.value))
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