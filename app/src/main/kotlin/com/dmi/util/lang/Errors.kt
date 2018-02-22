package com.dmi.util.lang

fun unsupportedOperation(msg: String? = null): Nothing {
    val exception: Throwable = if (msg != null) UnsupportedOperationException(msg) else UnsupportedOperationException()
    throw exception
}