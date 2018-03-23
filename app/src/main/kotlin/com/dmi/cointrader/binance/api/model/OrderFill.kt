package com.dmi.cointrader.binance.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class OrderFill {
    /**
     * Price.
     */
    var price: String? = null

    /**
     * Quantity.
     */
    var qty: String? = null

    /**
     * Commission.
     */
    var commission: String? = null

    /**
     * Asset on which commission is taken
     */
    var commissionAsset: String? = null

    override fun toString(): String {
        return "OrderFill(price=$price, qty=$qty, commission=$commission, commissionAsset=$commissionAsset)"
    }
}