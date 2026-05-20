package com.example.data.repository

import com.example.data.api.TheiaApiClient
import com.example.data.model.ChatRequest
import com.example.data.model.ChatResponse
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BLACKCLOUD Çekirdek veri katmanı ile UI arasındaki köprü.
 * Ağ işlemlerinin arka planda (Dispatchers.IO) güvenle yürütülmesini sağlar.
 */
class TheiaRepository {
    
    private val api = TheiaApiClient.service

    /**
     * Yerel backend sunucusunun çalışıp çalışmadığını pingler.
     */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.ping()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Termux/localhost çekirdeğindeki aktif projeleri getirir.
     */
    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        api.getProjects()
    }

    /**
     * Belirtilen proje bağlamında yeni bir sohbet mesajı gönderir ve yanıt alır.
     */
    suspend fun sendChat(projectId: String, message: String): ChatResponse = withContext(Dispatchers.IO) {
        api.sendChat(ChatRequest(projectId, message))
    }
}
