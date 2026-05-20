package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * BLACKCLOUD sohbet yanıtı protokol modeli.
 */
@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "text") val text: String,
    @Json(name = "kkyp") val kkyp: KkypMetadata
)
