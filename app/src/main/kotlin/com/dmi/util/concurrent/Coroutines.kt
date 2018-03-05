package com.dmi.util.concurrent

import com.dmi.util.collection.zip
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Iterable<R> = map { value ->
    async {
        transform(value)
    }
}.map {
    it.await()
}

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

fun <T> ReceiveChannel<T>.insert(
        itemsFirst: (first: T) -> List<T>,
        itemsBetween: (previous: T, next: T) -> List<T>,
        itemsLast: (last: T) -> List<T>
): ReceiveChannel<T> = insertFirst(itemsFirst).insertBetween(itemsBetween).insertLast(itemsLast)

fun <T> ReceiveChannel<T>.insertFirst(items: (next: T) -> List<T>): ReceiveChannel<T> = produce {
    var isFirst = true

    consumeEach {
        if (isFirst) {
            items(it).forEach {
                send(it)
            }
            isFirst = false
        }

        send(it)
    }
}

fun <E> ReceiveChannel<E>.insertBetween(items: (E, E) -> List<E>): ReceiveChannel<E> = produce {
    class Item(var value: E)

    var previous: Item? = null

    consumeEach {
        if (previous == null) {
            previous = Item(it)
        } else {
            items(previous!!.value, it).forEach {
                send(it)
            }
            previous!!.value = it
        }
        send(it)
    }
}

fun <E> ReceiveChannel<E>.insertLast(itemsAfter: (E) -> List<E>): ReceiveChannel<E> = produce {
    class Item(var value: E)

    var last: Item? = null

    consumeEach {
        send(it)

        if (last == null) {
            last = Item(it)
        } else {
            last!!.value = it
        }
    }

    if (last != null) {
        itemsAfter(last!!.value).forEach {
            send(it)
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

fun <T> ReceiveChannel<T>.withPrevious(num: Int): ReceiveChannel<Pair<T, T?>> = produce {
    require(num >= 0)

    val allPrevious = LinkedList<T>()
    consumeEach {
        allPrevious.addLast(it)
        val previous = if (allPrevious.size > num) {
            allPrevious.removeFirst()
        } else {
            null
        }
        send(Pair(it, previous))
    }
}

fun <T> buildChannel(build: suspend () -> ReceiveChannel<T>): ReceiveChannel<T> = produce {
    build().consumeEach {
        send(it)
    }
}