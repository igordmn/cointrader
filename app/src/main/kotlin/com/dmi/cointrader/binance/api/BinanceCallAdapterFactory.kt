package com.dmi.cointrader.binance.api

import com.binance.api.client.BinanceApiError
import com.binance.api.client.exception.BinanceApiException
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class BinanceCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
            returnType: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (Deferred::class.java != getRawType(returnType)) {
            return null
        }
        if (returnType !is ParameterizedType) {
           error("Deferred return type must be parameterized as Deferred<Foo> or Deferred<out Foo>")
        }
        val responseType = getParameterUpperBound(0, returnType)
        return BodyCallAdapter<Any>(responseType, retrofit)
    }

    private class BodyCallAdapter<T>(
            private val responseType: Type,
            private val retrofit: Retrofit
    ) : CallAdapter<T, Deferred<T>> {
        override fun responseType() = responseType

        override fun adapt(call: Call<T>): Deferred<T> {
            val deferred = CompletableDeferred<T>()

            deferred.invokeOnCompletion {
                if (deferred.isCancelled) {
                    call.cancel()
                }
            }

            call.enqueue(object : Callback<T> {
                override fun onFailure(call: Call<T>, t: Throwable) {
                    deferred.completeExceptionally(t)
                }

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful) {
                        deferred.complete(response.body()!!)
                    } else {
                        try {
                            val error = retrofit
                                    .responseBodyConverter<BinanceApiError>(BinanceApiError::class.java, arrayOfNulls<Annotation>(0))
                                    .convert(response.errorBody()) as BinanceApiError
                            deferred.completeExceptionally(BinanceApiException(error))
                        } catch (e: Exception) {
                            val message = response.errorBody().toString()
                            deferred.completeExceptionally(RuntimeException("Unknown error:\n$message"))
                        }
                    }
                }
            })

            return deferred
        }
    }
}