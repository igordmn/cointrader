package com.dmi.util.atom

interface Atom<T>: ReadAtom<T> {
    override suspend operator fun invoke(): T
    suspend fun set(value: T)
}

class MemoryAtom<T>(private var value: T): Atom<T> {
    override suspend operator fun invoke(): T = value

    override suspend fun set(value: T) {
        this.value = value
    }
}

interface ReadAtom<out T> {
    suspend operator fun invoke(): T
}