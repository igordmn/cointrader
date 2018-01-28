package util.concurrent

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce

fun <T> ReceiveChannel<T>.windowedWithPartial(size: Int): ReceiveChannel<List<T>> = produce {
    require(size > 0)

    var list = ArrayList<T>()

    consumeEach {
        list.add(it)
        if (list.size == size) {
            send(list)
            list = ArrayList()
        }
    }

    if (list.size > 0) {
        send(list)
    }
}