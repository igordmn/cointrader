package com.dmi.util.concurrent

import com.dmi.util.collection.zip
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Iterable<R> = map { value ->
    async {
        transform(value)
    }
}.map {
    it.await()
}

fun <T> ReceiveChannel<List<T>>.flatten(): ReceiveChannel<T> = flatMap { it.asReceiveChannel() }

fun <T> ReceiveChannel<T>.chunked(size: Int): ReceiveChannel<List<T>> = produce {
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
    do {
        val indexToCandles = map {
            it.take(bufferSize).toList()
        }
        val chunk: List<List<T>> = indexToCandles.zip()
        chunk.forEach {
            send(it)
        }
    } while (chunk.size == bufferSize)
}

fun <T> ReceiveChannel<T>.insert(
        itemsBefore: (next: T) -> List<T>,
        itemsBetween: (previous: T, next: T) -> List<T>,
        itemsAfter: (previous: T) -> List<T>
): ReceiveChannel<T> = insertBefore(itemsBefore).insertBetween(itemsBetween).insertAfter(itemsAfter)

fun <T> ReceiveChannel<T>.insertBefore(items: (next: T) -> List<T>): ReceiveChannel<T> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun <E> ReceiveChannel<E>.insertBetween(items: (E, E) -> List<E>): ReceiveChannel<E> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun <E> ReceiveChannel<E>.insertAfter(itemsAfter: (E) -> List<E>): ReceiveChannel<E> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun <T, M, C> ReceiveChannel<T>.chunkedBy(marker: (T) -> M, fold: (M, List<T>) -> C): ReceiveChannel<C> {
    return chunkedBy(marker).map { (key, value) ->
        fold(key, value)
    }
}

fun <T, M> ReceiveChannel<T>.chunkedBy(marker: (T) -> M): ReceiveChannel<Pair<M, List<T>>> = produce {
    class Billet(val mark: M, firstItem: T) {
        val items = arrayListOf(firstItem)

        suspend fun addOrSend(mark: M, item: T): Billet = if (this.mark != mark) {
            send()
            Billet(mark, item)
        } else {
            items.add(item)
            this
        }

        suspend fun send() = send(Pair(mark, items))
    }

    var billet: Billet? = null

    consumeEach {
        val mark = marker(it)
        billet = billet?.addOrSend(mark, it) ?: Billet(mark, it)
    }

    billet?.send()
}

fun <T, R> ReceiveChannel<T>.map(transform: (T) -> R): ReceiveChannel<R> = produce {
    consumeEach {
        send(transform(it))
    }
}