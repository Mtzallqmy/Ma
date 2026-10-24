package com.dawahayah.app.data.remote

import com.dawahayah.app.data.local.SeedBundleDto
import kotlinx.serialization.Serializable

@Serializable
data class PublishedSnapshot(
    val schemaVersion: Int,
    val complete: Boolean,
    val revision: String,
    val bundle: SeedBundleDto
)
