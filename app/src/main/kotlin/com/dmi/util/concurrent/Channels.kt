package com.dmi.util.concurrent

import com.dmi.util.collection.zip
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.experimental.CoroutineContext

fun <T> ReceiveChannel<List<T>>.flatten(): ReceiveChannel<T> = flatMap { it.asReceiveChannel() }

fun <T> ReceiveChannel<T>.chunked(size: Int): ReceiveChannel<List<T>> = produce {
    require(size > 0)

    var chunk = ArrayList<T>(size)
    consumeEach {
        if (chunk.size == size) {
            send(chunk)
            chunk = ArrayList(size)
        }
        chunk.add(it)
    }
    if (chunk.isNotEmpty()) {
        send(chunk)
    }
}

fun <T> List<ReceiveChannel<T>>.zip(bufferSize: Int = 100): ReceiveChannel<List<T>> = produce {
    try {
        do {
            val iterators = map {
                it.iterator()
            }
            val chunks = iterators.map {
                ArrayList<T>(bufferSize).apply {
                    while (size < bufferSize && it.hasNext()) {
                        add(it.next())
                    }
                }
            }
            val chunk: List<List<T>> = chunks.zip()
            chunk.forEach {
                send(it)
            }
        } while (chunk.size == bufferSize)
    } finally {
        forEach {
            it.cancel()
        }
    }
}

fun <T, M, C> ReceiveChannel<T>.chunkedBy(marker: (T) -> M, fold: (M, List<T>) -> C): ReceiveChannel<C> {
    return chunkedBy(marker).map { (key, value) ->
        fold(key, value)
    }
}

fun <T, M> ReceiveChannel<T>.chunkedBy(marker: (T) -> M): ReceiveChannel<Pair<M, List<T>>> = produce {
    class Billet(val mark: M, firstItem: T) {
        val items = arrayListOf(firstItem)
    }

    var billet: Billet? = null

    consumeEach { item ->
        val mark = marker(item)

        billet = billet?.let {
            if (it.mark != mark) {
                send(Pair(it.mark, it.items))
                Billet(mark, item)
            } else {
                it.items.add(item)
                it
            }
        } ?: Billet(mark, item)
    }

    billet?.let {
        send(Pair(it.mark, it.items))
    }
}

fun <T, R> ReceiveChannel<T>.map(transform: (T) -> R): ReceiveChannel<R> = produce {
    consumeEach {
        send(transform(it))
    }
}

fun <T> ReceiveChannel<T>.filter(predicate: (T) -> Boolean): ReceiveChannel<T> = produce {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

data class CurrentAndPrevious<out A, out B>(
        val current: A,
        val previous: B
)

fun <T> ReceiveChannel<T>.withPrevious(num: Int): ReceiveChannel<CurrentAndPrevious<T, T?>> = produce {
    require(num >= 0)

    val allPrevious = LinkedList<T>()
    consumeEach {
        allPrevious.addLast(it)
        val previous = if (allPrevious.size > num) {
            allPrevious.removeFirst()
        } else {
            null
        }
        send(CurrentAndPrevious(it, previous))
    }
}

fun <T> buildChannel(build: suspend () -> ReceiveChannel<T>): ReceiveChannel<T> = produce {
    build().consumeEach {
        send(it)
    }
}

data class Indexed<out INDEX, out VALUE>(val index: INDEX, val value: VALUE)
typealias LongIndexed<VALUE> = Indexed<Long, VALUE>

fun <E> ReceiveChannel<E>.withLongIndex(startIndex: Long = 0): ReceiveChannel<LongIndexed<E>> = produce {
    var index = startIndex
    consumeEach {
        send(LongIndexed(index++, it))
    }
}

fun <T> infiniteChannel(nextValue: suspend () -> T): ReceiveChannel<T> = produce {
    while (true) {
        send(nextValue())
    }
}

suspend fun <T> ReceiveChannel<T>.consumeTo(channel: SendChannel<T>) = consumeEach {
    channel.send(it)
}

fun <T> ReceiveChannel<T>.windowed(size: Int, step: Int): ReceiveChannel<List<T>> = produce {
    require(size > 0 && step > 0)
    val list = LinkedList<T>()
    var n = 1
    consumeEach {
        list.add(it)
        if (n >= size && n % step == 0) {
            send(ArrayList(list))
        }
        list.removeFirst()
        n++
    }
}