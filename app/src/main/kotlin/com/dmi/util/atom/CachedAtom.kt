package com.dmi.util.atom

suspend fun <T> Atom<T>.cached(): Atom<T> {
    var cached = this()
    return object : Atom<T> {
        suspend override fun invoke(): T = cached

        suspend override fun set(value: T) {
            this@cached.set(value)
            cached = value
        }
    }
}