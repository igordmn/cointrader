package exchange.binance.api

import com.binance.api.client.constant.BinanceApiConstants
import com.binance.api.client.security.AuthenticationInterceptor
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.Logger
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

fun binanceAPI(apiKey: String? = null, secret: String? = null, log: Logger? = null): BinanceAPI {
    val httpClient = OkHttpClient.Builder()

    if (log != null) {
        httpClient.addInterceptor(
                HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                    log.info(it)
                }).setLevel(HttpLoggingInterceptor.Level.BODY)
        )
    }

    if (apiKey != null && secret != null) {
        httpClient.addInterceptor(AuthenticationInterceptor(apiKey, secret))
    }

    val retrofit = Retrofit.Builder()
            .baseUrl(BinanceApiConstants.API_BASE_URL)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(httpClient.build()).build()

    return retrofit.create(BinanceAPI::class.java)
}