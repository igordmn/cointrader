package com.dmi.cointrader.binance.api.model

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderStatus
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Response returned when placing a new order on the system.
 *
 * @see NewOrder for the request
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class NewOrderResponse {

    /**
     * Order symbol.
     */
    var symbol: String? = null

    /**
     * Order id.
     */
    var orderId: Long? = null

    /**
     * This will be either a generated one, or the newClientOrderId parameter
     * which was passed when creating the new order.
     */
    var clientOrderId: String? = null

    /**
     * Transact time for this order.
     */
    var transactTime: Long? = null

    var price: String? = null

    var origQty: String? = null

    var executedQty: String? = null

    var status: OrderStatus? = null

    var timeInForce: TimeInForce? = null

    var type: OrderType? = null

    var side: OrderSide? = null

    lateinit var fills: List<OrderFill>

    override fun toString(): String {
        return "NewOrderResponse(symbol=$symbol, orderId=$orderId, clientOrderId=$clientOrderId, transactTime=$transactTime, price=$price, origQty=$origQty, executedQty=$executedQty, status=$status, timeInForce=$timeInForce, type=$type, side=$side, fills=$fills)"
    }
}
