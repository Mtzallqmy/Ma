package com.dawahayah.app.core.contentpacks

import com.dawahayah.app.domain.model.ContentPackManifest
import com.dawahayah.app.core.security.IntegrityVerifier
import java.io.File

/** Validates all untrusted directory components before touching app storage. */
object PackManifestValidator {
    private const val MAX_BYTES = 250L * 1024L * 1024L
    private val idPattern = Regex("[a-zA-Z0-9_-]{1,128}")
    private val versionPattern = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}")

    fun isValid(manifest: ContentPackManifest, requestedId: String): Boolean {
        if (!idPattern.matches(requestedId) || manifest.packId != requestedId ||
            !versionPattern.matches(manifest.version) || manifest.version.contains("..") ||
            manifest.schemaVersion != 1 || manifest.files.isEmpty() || manifest.files.size > 300 ||
            manifest.sizeBytes !in 1..MAX_BYTES ||
            manifest.files.map { it.path }.distinct().size != manifest.files.size ||
            manifest.files.map { it.assetId }.distinct().size != manifest.files.size
        ) return false
        if (manifest.files.any {
            it.sizeBytes !in 1..MAX_BYTES || !idPattern.matches(it.assetId) ||
                !it.sha256.matches(Regex("[0-9a-fA-F]{64}")) || it.path.contains('\\') ||
                it.path.split('/').any { part -> part.isBlank() || part == "." || part == ".." } ||
                !IntegrityVerifier.isSafeRelativePath(it.path, File("pack-validation"))
        }) return false
        // Every value was bounded above, so this sum cannot overflow Long.
        return manifest.files.sumOf { it.sizeBytes } == manifest.sizeBytes
    }
}
