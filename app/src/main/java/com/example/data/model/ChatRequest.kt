package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * BLACKCLOUD sohbet isteği protokol modeli.
 */
@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "projectId") val projectId: String,
    @Json(name = "message") val message: String
)
