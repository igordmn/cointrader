package com.dmi.util.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun rootLog(): Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)