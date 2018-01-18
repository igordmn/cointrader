package util.lang

fun unsupportedOperation(msg: String? = null): Nothing {
    val exception = if (msg != null) UnsupportedOperationException(msg) else UnsupportedOperationException()
    throw exception
}