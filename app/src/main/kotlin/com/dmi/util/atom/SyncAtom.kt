package com.dmi.util.atom

interface SyncAtom<out T> : ReadAtom<T> {
    suspend fun sync()
}

suspend fun <T> syncAtom(original: Atom<T>): SyncAtom<T> {
    var cached = original()
    return object : SyncAtom<T> {
        suspend override fun invoke(): T = cached

        suspend override fun sync() {
            cached = original()
        }
    }
}