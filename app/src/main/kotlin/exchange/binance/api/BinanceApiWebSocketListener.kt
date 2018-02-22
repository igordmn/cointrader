package exchange.binance.api

import com.binance.api.client.BinanceApiCallback
import com.binance.api.client.exception.BinanceApiException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

import java.io.IOException

/**
 * Binance API WebSocket listener.
 */
class BinanceApiWebSocketListener<T>(
        private val callback: (T) -> Unit,
        private val eventClass: Class<T>
) : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket?, text: String?) {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        try {
            val event = mapper.readValue(text, eventClass)
            callback(event)
        } catch (e: IOException) {
            throw BinanceApiException(e)
        }
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        throw BinanceApiException(t)
    }
}