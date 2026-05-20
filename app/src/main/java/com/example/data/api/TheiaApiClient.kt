package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * BLACKCLOUD yerel bağlantısı için yapılandırılmış HTTP istemcisi.
 * LLM yanıtlarının üretim süresi düşünülerek yüksek readTimeout değeri ayarlanmıştır.
 */
object TheiaApiClient {
    
    // Varsayılan Termux / localhost çekirdek portu
    private const val BASE_URL = "http://127.0.0.1:8765/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)  // LLM'in düşünmesi ve yanıt üretmesi için yeterli süre
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: TheiaApiService = retrofit.create(TheiaApiService::class.java)

    /**
     * Gerekirse BASE_URL'i dinamik olarak almak için yardımcı url.
     */
    fun getBaseUrl(): String = BASE_URL
}
