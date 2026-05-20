package com.example.data.api

import com.example.data.model.ChatRequest
import com.example.data.model.ChatResponse
import com.example.data.model.Project
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * PROJECT BLACKCLOUD (Theia-Soul/Manisa) yerel FastAPI sunucusu için Retrofit servis arayüzü.
 */
interface TheiaApiService {
    
    /**
     * Çekirdeğin canlılık (liveness) durumunu kontrol etmek için ping isteği.
     */
    @GET("ping")
    suspend fun ping(): Response<Void>

    /**
     * Aktif bilişsel projelerin listesini getirir.
     */
    @GET("projects")
    suspend fun getProjects(): List<Project>

    /**
     * Seçili proje bağlamında sohbet sorusunu gönderir.
     */
    @POST("chat")
    suspend fun sendChat(@Body request: ChatRequest): ChatResponse
}
