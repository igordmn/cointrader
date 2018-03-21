package com.dmi.util.atom

suspend fun <T> Atom<T>.cached(): Atom<T> {
    var cached = this()
    return object : Atom<T> {
        override suspend fun invoke(): T = cached

        override suspend fun set(value: T) {
            this@cached.set(value)
            cached = value
        }
    }
}