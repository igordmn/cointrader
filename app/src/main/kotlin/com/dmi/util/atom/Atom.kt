package com.dmi.util.atom

interface Atom<T>: ReadAtom<T> {
    override suspend operator fun invoke(): T
    suspend fun set(value: T)
}

interface ReadAtom<out T> {
    suspend operator fun invoke(): T
}