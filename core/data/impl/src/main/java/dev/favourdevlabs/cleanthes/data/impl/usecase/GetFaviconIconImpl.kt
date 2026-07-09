package dev.favourdevlabs.cleanthes.data.impl.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.favourdevlabs.cleanthes.data.api.usecase.GetFaviconIcon
import dev.favourdevlabs.cleanthes.data.impl.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject

class GetFaviconIconImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GetFaviconIcon {

    private val notFound = GetFaviconIcon.Result(found = false, bytes = null, contentType = null)

    private val cacheDir: File by lazy {
        File(context.cacheDir, "favicons").apply { mkdirs() }
    }

    private fun extractHost(website: String): String? {
        val trimmed = website.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        return try {
            URI(withScheme).host
        } catch (_: Exception) {
            null
        }
    }

    /** Deterministic filename for a domain — hashed so arbitrary domain strings
     * (which may contain characters unsafe for filenames) always map to a safe name. */
    private fun cacheFileFor(domain: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(domain.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return File(cacheDir, hex)
    }

    override suspend fun invoke(website: String): GetFaviconIcon.Result =
        withContext(Dispatchers.IO) {
            try {
                val domain = extractHost(website) ?: return@withContext notFound
                val cacheFile = cacheFileFor(domain)

                // Disk cache hit — no network call needed, works fully offline.
                if (cacheFile.exists()) {
                    val cachedBytes = cacheFile.readBytes()
                    return@withContext GetFaviconIcon.Result(
                        found = true,
                        bytes = cachedBytes,
                        // No stored content-type — decodeFaviconBitmap sniffs SVG vs
                        // raster directly from the bytes when this is null.
                        contentType = null,
                    )
                }

                val encodedDomain = URLEncoder.encode(domain, "UTF-8")
                val url = URL("${BuildConfig.FAVICON_PROXY_BASE_URL}/favicon?domain=$encodedDomain")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("X-API-Key", BuildConfig.FAVICON_PROXY_API_KEY)
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext notFound
                    }

                    val bytes = connection.inputStream.use { it.readBytes() }
                    val contentType = connection.contentType ?: "application/octet-stream"

                    // Best-effort write — a failed cache write shouldn't fail the
                    // request itself, the icon we already fetched is still valid.
                    try {
                        cacheFile.writeBytes(bytes)
                    } catch (_: Exception) {
                        // Ignored deliberately — see comment above.
                    }

                    GetFaviconIcon.Result(found = true, bytes = bytes, contentType = contentType)
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                notFound
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 5_000
    }
}
