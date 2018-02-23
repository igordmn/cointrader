package com.dmi.cointrader.app.dao

import com.dmi.cointrader.data.MyObjectBox
import java.io.File

fun boxStore() = MyObjectBox.builder().baseDirectory(File("data/objectbox")).build()