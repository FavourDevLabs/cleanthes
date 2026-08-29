package dev.favourdevlabs.cleanthes.data.impl.usecase

import dev.favourdevlabs.cleanthes.data.api.usecase.CheckPasswordBreach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject

class CheckPasswordBreachImpl @Inject constructor() : CheckPasswordBreach {

    private fun sha1Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02X".format(it) }
    }

    override suspend fun invoke(password: String): CheckPasswordBreach.Result =
        withContext(Dispatchers.IO) {
            try {
                val hash = sha1Hex(password)
                val prefix = hash.substring(0, 5)
                val suffix = hash.substring(5)

                val url = URL("https://api.pwnedpasswords.com/range/$prefix")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Add-Padding", "true")
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext CheckPasswordBreach.Result.CheckFailed
                    }

                    connection.inputStream.bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val (returnedSuffix, count) = line.split(":")
                            if (returnedSuffix == suffix) {
                                return@withContext CheckPasswordBreach.Result.Breached(
                                    breachCount = count.trim().toInt(),
                                )
                            }
                        }
                    }
                    CheckPasswordBreach.Result.Safe
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                android.util.Log.e("BreachCheck", "FAILED", e)
                CheckPasswordBreach.Result.CheckFailed
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 5_000
    }
}
