package com.dmi.util.atom

interface SyncAtom<out T> : ReadAtom<T> {
    suspend fun sync()
}

suspend fun <T> ReadAtom<T>.synchronizable(): SyncAtom<T> {
    var cached = this()
    return object : SyncAtom<T> {
        suspend override fun invoke(): T = cached

        suspend override fun sync() {
            cached = this@synchronizable()
        }
    }
}