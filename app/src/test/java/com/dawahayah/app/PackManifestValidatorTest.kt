package com.dawahayah.app

import com.dawahayah.app.core.contentpacks.PackManifestValidator
import com.dawahayah.app.core.security.IntegrityVerifier
import com.dawahayah.app.domain.model.ContentPackManifest
import com.dawahayah.app.domain.model.PackFileEntry
import org.junit.Assert.*
import org.junit.Test

class PackManifestValidatorTest {
    private val entry = PackFileEntry("content/lesson.json", "application/json", 20, "a".repeat(64), "asset-lesson")
    private val manifest = ContentPackManifest("pack-safe", "1.0.0", "organ", "cor", "درس", "Lesson",
        "ar", "lesson", "low", 1, "0.2.0", 20, listOf(entry), license = "CC0",
        attribution = "Original", publishedAt = "2026-10-24")

    @Test fun safePackIsAccepted() = assertTrue(PackManifestValidator.isValid(manifest, manifest.packId))

    @Test fun untrustedVersionCannotEscapePackDirectory() {
        listOf("../db", "a/../../db", "/absolute", "x\\y", "..", "").forEach {
            assertFalse(it, PackManifestValidator.isValid(manifest.copy(version = it), manifest.packId))
        }
    }

    @Test fun rejectOverflowsDuplicatesAndInvalidHashes() {
        val invalid = listOf(
            manifest.copy(sizeBytes = Long.MAX_VALUE),
            manifest.copy(files = listOf(entry.copy(sizeBytes = Long.MAX_VALUE))),
            manifest.copy(files = listOf(entry, entry), sizeBytes = 40),
            manifest.copy(files = listOf(entry.copy(sha256 = "bad"))),
            manifest.copy(files = listOf(entry.copy(path = "../db"))),
            manifest.copy(files = listOf(entry.copy(path = "a\\b"))),
            manifest.copy(packId = "different"),
            manifest.copy(schemaVersion = 2)
        )
        invalid.forEach { assertFalse(PackManifestValidator.isValid(it, "pack-safe")) }
    }

    @Test fun secureEndpointRejectsCredentialsMalformedHostsAndFragments() {
        listOf("https://", "https://user:password@example.com/a", "https://example.com/a#secret",
            "https://bad host/a", "https://example.com:99999", " https://example.com").forEach {
            assertFalse(it, IntegrityVerifier.isSecureHttpsEndpoint(it))
        }
        assertTrue(IntegrityVerifier.isSecureHttpsEndpoint("https://example.com/file?signature=abc"))
    }
}
