package util.log

import java.io.PrintWriter
import java.io.StringWriter

fun Exception.stackTraceString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}