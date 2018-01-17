package exchange.binance.api

import com.binance.api.client.constant.BinanceApiConstants
import com.binance.api.client.security.AuthenticationInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.Logger
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

const val DEFAULT_RECEIVING_WINDOW = 6_000_000L

fun binanceAPI(apiKey: String? = null, secret: String? = null, log: Logger? = null): BinanceAPI {
    val httpClient = OkHttpClient.Builder()

    if (log != null) {
        httpClient.addInterceptor(
                HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                    log.trace(it)
                }).setLevel(HttpLoggingInterceptor.Level.BODY)
        )
    }

    if (apiKey != null && secret != null) {
        httpClient.addInterceptor(AuthenticationInterceptor(apiKey, secret))
    }

    val retrofit = Retrofit.Builder()
            .baseUrl(BinanceApiConstants.API_BASE_URL)
            .addCallAdapterFactory(BinanceCallAdapterFactory())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(httpClient.build()).build()

    return retrofit.create(BinanceAPI::class.java)
}