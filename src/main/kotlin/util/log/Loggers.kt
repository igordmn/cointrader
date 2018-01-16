package util.log

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun logger(cls: KClass<*>) = LoggerFactory.getLogger(cls.qualifiedName)