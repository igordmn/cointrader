package com.dmi.util.lang

fun unsupported(msg: String? = null): Nothing {
    val exception: Throwable = if (msg != null) UnsupportedOperationException(msg) else UnsupportedOperationException()
    throw exception
}