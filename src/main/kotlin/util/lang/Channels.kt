package util.lang

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce

suspend fun <T> ReceiveChannel<T>.zipWithNext(): ReceiveChannel<Pair<T, T>> = produce {
    val iterator = iterator()
    if (iterator.hasNext()) {
        var current = iterator.next()
        while (iterator.hasNext()) {
            val next = iterator.next()
            send(Pair(current, next))
            current = next
        }
    }
    close()
}