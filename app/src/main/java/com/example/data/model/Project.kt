package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * PROJECT BLACKCLOUD içindeki projeleri temsil eden veri modeli.
 * Örneğin: theia-soul, manisa-permakultur
 */
@JsonClass(generateAdapter = true)
data class Project(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "createdAt") val createdAt: String
)
