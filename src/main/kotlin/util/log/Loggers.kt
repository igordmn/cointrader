package util.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun logger(cls: KClass<*>): Logger = logger(cls.qualifiedName.toString())
fun logger(name: String): Logger = LoggerFactory.getLogger(name)