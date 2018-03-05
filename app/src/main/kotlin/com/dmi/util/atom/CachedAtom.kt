package com.dmi.util.atom

suspend fun <T> cachedAtom(original: Atom<T>): Atom<T> {
    var cached = original()
    return object : Atom<T> {
        suspend override fun invoke(): T = cached

        suspend override fun set(value: T) {
            original.set(value)
            cached = value
        }
    }
}