package util.log

import java.util.logging.Logger
import kotlin.reflect.KClass

fun logger(cls: KClass<*>) = Logger.getLogger(cls.qualifiedName)