package com.juvis.diet.android.anewproject


import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.logisbelley.mobileapp.BuildConfig
import com.logisbelley.mobileapp.BuildConfig.URL_BASE
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.util.concurrent.TimeUnit


/**
 * Retrofit 생성자
 */
class RetrofitCreator {
    companion object {
        private const val CONNECT_TIMEOUT: Long = 10
        private const val READ_TIMEOUT: Long = 15
        private const val API_BASE_URL = URL_BASE

        /**
         * Retrofit 객체 생성
         */
        private fun defaultRetrofit(context: Context): Retrofit {
            val gson = GsonBuilder().setLenient().create()
            return Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(createOkHttpClient())
                .build()
        }

        /**
         * retrofit  인터페이스 생성
         */
        fun <T> create(service: Class<T>, context: Context): T {

            return defaultRetrofit(context).create(service)
        }


        private fun createOkHttpClient(): OkHttpClient {
            val interceptor = HttpLoggingInterceptor()
            if (BuildConfig.DEBUG) {
                interceptor.level = HttpLoggingInterceptor.Level.BODY
            } else {
                interceptor.level = HttpLoggingInterceptor.Level.NONE
            }
            return OkHttpClient.Builder().addInterceptor(interceptor).addNetworkInterceptor { chain ->
                    val original = chain.request()
                    try {
                            val newRequest = original.newBuilder()
//                                .addHeader("Content-Type", "application/json")
                                .build()
                            chain.proceed(newRequest)

                    } catch (e: ConnectException) {
                        chain.proceed(original)
                    } catch (e: SocketException) {
                        chain.proceed(original)
                    } catch (e: IOException) {
                        chain.proceed(original)
                    } catch (e: Exception) {
                        chain.proceed(original)
                    }
                }
                .readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build()

        }

    }
}