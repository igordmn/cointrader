package com.dmi.cointrader.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Market(
        @Id var id: Long = 0,
        val name: String
)