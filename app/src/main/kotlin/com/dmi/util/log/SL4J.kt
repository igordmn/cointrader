package com.dmi.util.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun rootLog(): Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)