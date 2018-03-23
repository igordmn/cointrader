package com.dmi.cointrader.binance.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.dmi.cointrader.binance.api.model.MultiAggTradeEvent
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.*
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.TimeUnit

class BinanceSocketAPI : Closeable {
    private val client = OkHttpClient.Builder()
            .connectTimeout(900, TimeUnit.SECONDS)
            .writeTimeout(900, TimeUnit.SECONDS)
            .readTimeout(900, TimeUnit.SECONDS)
            .dispatcher(Dispatcher().apply {
                maxRequestsPerHost = 10
            })
            .build()

    // todo close on cancel
    fun aggTrades(symbols: List<String>): ReceiveChannel<MultiAggTradeEvent> {
        val channel = Channel<MultiAggTradeEvent>()

        val streams = symbols.joinToString("/") { "$it@aggTrade" }
        val request = Request.Builder().url("wss://stream.binance.com:9443/stream?streams=$streams").build()

        client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val mapper = ObjectMapper().registerModule(KotlinModule())
                    val event = mapper.readValue(text, MultiAggTradeEvent::class.java)
                    runBlocking {
                        channel.send(event)
                    }
                } catch (t: Throwable) {
                    channel.close(t)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                channel.close(t)
            }
        })

        return channel
    }

    @Throws(IOException::class)
    override fun close() {
        client.dispatcher().executorService().shutdown()
    }
}