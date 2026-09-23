package com.example.domain.engine

import com.example.data.model.BookFormat
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class FileValidationResult(
    val isValid: Boolean,
    val detectedFormat: BookFormat,
    val sha256Hash: String,
    val fileSize: Long,
    val isDrmProtected: Boolean = false,
    val drmExplanation: String? = null,
    val errorMessage: String? = null
)

object FileValidator {

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun validateFile(file: File): FileValidationResult {
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            return FileValidationResult(
                isValid = false,
                detectedFormat = BookFormat.TXT,
                sha256Hash = "",
                fileSize = 0L,
                errorMessage = "File tidak ditemukan atau tidak dapat dibaca (${file.name})"
            )
        }

        val fileSize = file.length()
        val hash = try {
            computeSha256(file)
        } catch (e: Exception) {
            return FileValidationResult(
                isValid = false,
                detectedFormat = BookFormat.TXT,
                sha256Hash = "",
                fileSize = fileSize,
                errorMessage = "Gagal menghitung checksum file: ${e.message}"
            )
        }

        val name = file.name.lowercase()

        // Check for PDF
        if (name.endsWith(".pdf")) {
            val isPdfHeader = try {
                FileInputStream(file).use { fis ->
                    val header = ByteArray(5)
                    val read = fis.read(header)
                    read >= 4 && String(header, 0, 4) == "%PDF"
                }
            } catch (e: Exception) {
                false
            }
            if (!isPdfHeader) {
                return FileValidationResult(
                    isValid = false,
                    detectedFormat = BookFormat.PDF,
                    sha256Hash = hash,
                    fileSize = fileSize,
                    errorMessage = "File PDF korup atau header tidak valid (%PDF missing)"
                )
            }
            return FileValidationResult(
                isValid = true,
                detectedFormat = BookFormat.PDF,
                sha256Hash = hash,
                fileSize = fileSize
            )
        }

        // Check for CBZ
        if (name.endsWith(".cbz")) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = BookFormat.CBZ,
                sha256Hash = hash,
                fileSize = fileSize
            )
        }

        // Check for TXT
        if (name.endsWith(".txt")) {
            return FileValidationResult(
                isValid = true,
                detectedFormat = BookFormat.TXT,
                sha256Hash = hash,
                fileSize = fileSize
            )
        }

        // Check ZIP or EPUB
        if (name.endsWith(".epub") || name.endsWith(".zip")) {
            var hasMimeEpub = false
            var hasContainerXml = false
            var hasEncryptionXml = false

            try {
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName == "mimetype") {
                            val content = zis.bufferedReader().readText().trim()
                            if (content.contains("application/epub+zip")) {
                                hasMimeEpub = true
                            }
                        } else if (entryName.contains("container.xml")) {
                            hasContainerXml = true
                        } else if (entryName.contains("encryption.xml") || entryName.contains("rights.xml")) {
                            hasEncryptionXml = true
                        }
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) {
                return FileValidationResult(
                    isValid = false,
                    detectedFormat = if (name.endsWith(".epub")) BookFormat.EPUB else BookFormat.ZIP_EPUB,
                    sha256Hash = hash,
                    fileSize = fileSize,
                    errorMessage = "Arsip ZIP/EPUB rusak atau terpotong: ${e.message}"
                )
            }

            // DRM check (FMT-15)
            if (hasEncryptionXml) {
                return FileValidationResult(
                    isValid = false,
                    detectedFormat = BookFormat.EPUB,
                    sha256Hash = hash,
                    fileSize = fileSize,
                    isDrmProtected = true,
                    drmExplanation = "File ini diproteksi oleh DRM (Adobe Digital Editions / KFX Rights). Yomu tidak mendukung pelucutan DRM sesuai kebijakan keamanan & hukum.",
                    errorMessage = "File dilindungi DRM enkripsi"
                )
            }

            // Detect if ZIP is actually an EPUB (FMT-12)
            if (hasMimeEpub || hasContainerXml || name.endsWith(".epub")) {
                return FileValidationResult(
                    isValid = true,
                    detectedFormat = BookFormat.EPUB,
                    sha256Hash = hash,
                    fileSize = fileSize
                )
            }

            // Fallback: regular CBZ/ZIP containing images
            return FileValidationResult(
                isValid = true,
                detectedFormat = BookFormat.CBZ,
                sha256Hash = hash,
                fileSize = fileSize
            )
        }

        return FileValidationResult(
            isValid = false,
            detectedFormat = BookFormat.TXT,
            sha256Hash = hash,
            fileSize = fileSize,
            errorMessage = "Format file '${file.extension}' belum didukung. Yomu mendukung EPUB, PDF, CBZ, TXT, dan ZIP-EPUB."
        )
    }
}
