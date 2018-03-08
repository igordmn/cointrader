package old.exchange.binance.api

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.*
import com.binance.api.client.domain.event.ListenKey
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.api.client.domain.general.ServerTime
import com.binance.api.client.domain.market.*
import com.google.common.util.concurrent.RateLimiter
import old.exchange.binance.api.model.NewOrderResponse
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlin.math.ceil

class BinanceAPI(
        private val service: BinanceAPIService,
        private val maxRequestsPerSecond: Int
) {
    private val thread = newSingleThreadContext("binanceApi")
    private val rateLimiter = RateLimiter.create(maxRequestsPerSecond.toDouble())

    suspend fun serverTime(): ServerTime = perform {
        service.serverTime
    }
    
    suspend fun exchangeInfo(): ExchangeInfo = perform {
        service.exchangeInfo
    }
    
    suspend fun allPrices(): List<TickerPrice> = perform {
        service.allPrices
    }
    
    suspend fun allBookTickers(): List<BookTicker> = perform {
        service.allBookTickers
    }

    suspend fun ping() = perform {
        service.ping()
    }

    suspend fun getOrderBook(symbol: String, limit: Int?): OrderBook = perform {
        service.getOrderBook(symbol, limit)
    }

    suspend fun getAggTrades(symbol: String, fromId: String, limit: Int?, startTime: Long?, endTime: Long?): List<AggTrade> = perform {
        service.getAggTrades(symbol, fromId, limit, startTime, endTime)
    }

    suspend fun getCandlestickBars(symbol: String, interval: String, limit: Int?, startTime: Long?, endTime: Long?): List<Candlestick> = perform {
        service.getCandlestickBars(symbol, interval, limit, startTime, endTime)
    }

    suspend fun get24HrPriceStatistics(symbol: String): TickerStatistics = perform {
        service.get24HrPriceStatistics(symbol)
    }

    suspend fun newOrder(symbol: String, side: OrderSide, type: OrderType, timeInForce: TimeInForce?, quantity: String, price: String?, stopPrice: String?, icebergQty: String?, recvWindow: Long?, timestamp: Long?): NewOrderResponse = perform {
        service.newOrder(symbol, side, type, timeInForce, quantity, price, stopPrice, icebergQty, "FULL", recvWindow, timestamp)
    }

    suspend fun newOrderTest(symbol: String, side: OrderSide, type: OrderType, timeInForce: TimeInForce?, quantity: String, price: String, stopPrice: String, icebergQty: String, recvWindow: Long?, timestamp: Long?) = perform {
        service.newOrderTest(symbol, side, type, timeInForce, quantity, price, stopPrice, icebergQty, recvWindow, timestamp)
    }

    suspend fun getOrderStatus(symbol: String, orderId: Long?, origClientOrderId: String, recvWindow: Long?, timestamp: Long?): Order = perform {
        service.getOrderStatus(symbol, orderId, origClientOrderId, recvWindow, timestamp)
    }

    suspend fun cancelOrder(symbol: String, orderId: Long?, origClientOrderId: String, newClientOrderId: String, recvWindow: Long?, timestamp: Long?) = perform {
        service.cancelOrder(symbol, orderId, origClientOrderId, newClientOrderId, recvWindow, timestamp)
    }

    suspend fun getOpenOrders(symbol: String, recvWindow: Long?, timestamp: Long?): List<Order> = perform {
        service.getOpenOrders(symbol, recvWindow, timestamp)
    }

    suspend fun getAllOrders(symbol: String, orderId: Long?, limit: Int?, recvWindow: Long?, timestamp: Long?): List<Order> = perform {
        service.getAllOrders(symbol, orderId, limit, recvWindow, timestamp)
    }

    suspend fun getAccount(recvWindow: Long?, timestamp: Long?): Account = perform {
        service.getAccount(recvWindow, timestamp)
    }

    suspend fun getMyTrades(symbol: String, limit: Int?, fromId: Long?, recvWindow: Long?, timestamp: Long?): List<Trade> = perform {
        service.getMyTrades(symbol, limit, fromId, recvWindow, timestamp)
    }

    suspend fun withdraw(asset: String, address: String, amount: String, name: String, recvWindow: Long?, timestamp: Long?) = perform {
        service.withdraw(asset, address, amount, name, recvWindow, timestamp)
    }

    suspend fun getDepositHistory(asset: String, recvWindow: Long?, timestamp: Long?): DepositHistory = perform {
        service.getDepositHistory(asset, recvWindow, timestamp)
    }

    suspend fun getWithdrawHistory(asset: String, recvWindow: Long?, timestamp: Long?): WithdrawHistory = perform {
        service.getWithdrawHistory(asset, recvWindow, timestamp)
    }

    suspend fun getDepositAddress(asset: String, recvWindow: Long?, timestamp: Long?): DepositAddress = perform {
        service.getDepositAddress(asset, recvWindow, timestamp)
    }

    suspend fun startUserDataStream(): ListenKey = perform {
        service.startUserDataStream()
    }

    suspend fun keepAliveUserDataStream(listenKey: String) = perform {
        service.keepAliveUserDataStream(listenKey)
    }

    suspend fun closeAliveUserDataStream(listenKey: String) = perform {
        service.closeAliveUserDataStream(listenKey)
    }

    private suspend fun <T> perform(action: suspend () -> Deferred<T>): T {
        return async(thread) {
            rateLimiter.acquire()
            action().await()
        }.await()
    }
}