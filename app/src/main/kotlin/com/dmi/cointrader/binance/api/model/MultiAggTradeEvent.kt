package com.dmi.cointrader.binance.api.model

import com.binance.api.client.domain.event.AggTradeEvent

data class MultiAggTradeEvent(var stream: String, var data: AggTradeEvent)