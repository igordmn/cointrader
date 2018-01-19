package exchange.binance.api

import com.binance.api.client.constant.BinanceApiConstants
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.*
import com.binance.api.client.domain.event.ListenKey
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.api.client.domain.general.ServerTime
import com.binance.api.client.domain.market.*
import exchange.binance.api.model.NewOrderResponse
import kotlinx.coroutines.experimental.Deferred
import retrofit2.http.*

interface BinanceAPIService {
    @get:GET("/api/v1/time")
    val serverTime: Deferred<ServerTime>

    @get:GET("/api/v1/exchangeInfo")
    val exchangeInfo: Deferred<ExchangeInfo>

    @get:GET("/api/v1/ticker/allPrices")
    val latestPrices: Deferred<List<TickerPrice>>

    @get:GET("/api/v1/ticker/allBookTickers")
    val bookTickers: Deferred<List<BookTicker>>
    // General endpoints

    @GET("/api/v1/ping")
    fun ping(): Deferred<Void>

    // Market data endpoints

    @GET("/api/v1/depth")
    fun getOrderBook(@Query("symbol") symbol: String, @Query("limit") limit: Int?): Deferred<OrderBook>

    @GET("/api/v1/aggTrades")
    fun getAggTrades(@Query("symbol") symbol: String, @Query("fromId") fromId: String, @Query("limit") limit: Int?,
                     @Query("startTime") startTime: Long?, @Query("endTime") endTime: Long?): Deferred<List<AggTrade>>

    @GET("/api/v1/klines")
    fun getCandlestickBars(@Query("symbol") symbol: String, @Query("interval") interval: String, @Query("limit") limit: Int?,
                           @Query("startTime") startTime: Long?, @Query("endTime") endTime: Long?): Deferred<List<Candlestick>>

    @GET("/api/v1/ticker/24hr")
    fun get24HrPriceStatistics(@Query("symbol") symbol: String): Deferred<TickerStatistics>

    // Account endpoints

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @POST("/api/v3/order")
    fun newOrder(@Query("symbol") symbol: String, @Query("side") side: OrderSide, @Query("type") type: OrderType,
                 @Query("timeInForce") timeInForce: TimeInForce?, @Query("quantity") quantity: String, @Query("price") price: String?,
                 @Query("stopPrice") stopPrice: String?, @Query("icebergQty") icebergQty: String?,
                 @Query("newOrderRespType") newOrderRespType: String?,
                 @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<NewOrderResponse>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @POST("/api/v3/order/test")
    fun newOrderTest(@Query("symbol") symbol: String, @Query("side") side: OrderSide, @Query("type") type: OrderType,
                     @Query("timeInForce") timeInForce: TimeInForce?, @Query("quantity") quantity: String, @Query("price") price: String,
                     @Query("stopPrice") stopPrice: String, @Query("icebergQty") icebergQty: String,
                     @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<Void>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/api/v3/order")
    fun getOrderStatus(@Query("symbol") symbol: String, @Query("orderId") orderId: Long?,
                       @Query("origClientOrderId") origClientOrderId: String, @Query("recvWindow") recvWindow: Long?,
                       @Query("timestamp") timestamp: Long?): Deferred<Order>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @DELETE("/api/v3/order")
    fun cancelOrder(@Query("symbol") symbol: String, @Query("orderId") orderId: Long?,
                    @Query("origClientOrderId") origClientOrderId: String, @Query("newClientOrderId") newClientOrderId: String,
                    @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<Void>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/api/v3/openOrders")
    fun getOpenOrders(@Query("symbol") symbol: String, @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<List<Order>>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/api/v3/allOrders")
    fun getAllOrders(@Query("symbol") symbol: String, @Query("orderId") orderId: Long?,
                     @Query("limit") limit: Int?, @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<List<Order>>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/api/v3/account")
    fun getAccount(@Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<Account>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/api/v3/myTrades")
    fun getMyTrades(@Query("symbol") symbol: String, @Query("limit") limit: Int?, @Query("fromId") fromId: Long?,
                    @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<List<Trade>>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @POST("/wapi/v3/withdraw.html")
    fun withdraw(@Query("asset") asset: String, @Query("address") address: String, @Query("amount") amount: String, @Query("name") name: String,
                 @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<Void>


    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/wapi/v3/depositHistory.html")
    fun getDepositHistory(@Query("asset") asset: String, @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<DepositHistory>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/wapi/v3/withdrawHistory.html")
    fun getWithdrawHistory(@Query("asset") asset: String, @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<WithdrawHistory>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_SIGNED_HEADER)
    @GET("/wapi/v3/depositAddress.html")
    fun getDepositAddress(@Query("asset") asset: String, @Query("recvWindow") recvWindow: Long?, @Query("timestamp") timestamp: Long?): Deferred<DepositAddress>

    // User stream endpoints

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_APIKEY_HEADER)
    @POST("/api/v1/userDataStream")
    fun startUserDataStream(): Deferred<ListenKey>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_APIKEY_HEADER)
    @PUT("/api/v1/userDataStream")
    fun keepAliveUserDataStream(@Query("listenKey") listenKey: String): Deferred<Void>

    @Headers(BinanceApiConstants.ENDPOINT_SECURITY_TYPE_APIKEY_HEADER)
    @DELETE("/api/v1/userDataStream")
    fun closeAliveUserDataStream(@Query("listenKey") listenKey: String): Deferred<Void>
}