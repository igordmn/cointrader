package com.dmi.util.io

class ResourceContext: AutoCloseable {
    private val resources = ArrayList<AutoCloseable>()

    fun <T: AutoCloseable> T.use(): T {
        resources.add(this)
        return this
    }

    override fun close() {
        resources.asReversed().forEach(AutoCloseable::close)
    }
}

inline fun <T> resourceContext(consume: ResourceContext.() -> T): T {
    return ResourceContext().use {
        consume(it)
    }
}