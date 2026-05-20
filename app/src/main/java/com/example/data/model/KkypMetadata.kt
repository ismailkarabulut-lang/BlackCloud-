package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Kaynak-Kilitli Yanıt Protokolü (KKYP) metadatası.
 * Model çıktısında doğrulama durumu ve zayıf bağlam uyarılarını içerir.
 */
@JsonClass(generateAdapter = true)
data class KkypMetadata(
    @Json(name = "verified") val verified: Boolean,
    @Json(name = "issues") val issues: List<String> = emptyList()
)
