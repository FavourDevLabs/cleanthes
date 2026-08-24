package dev.favourdevlabs.cleanthes.attestation.impl

import android.content.Context
import dev.favourdevlabs.cleanthes.attestation.impl.R
import org.json.JSONArray
import java.io.BufferedReader
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Loads the pinned Google Hardware Attestation root certificates bundled as
 * a raw resource (fetched from android.googleapis.com/attestation/root and
 * committed to the repo — see attestation_roots.json). Not fetched at
 * runtime: Cleanthes makes no network calls, by design.
 *
 * Entries in the JSON array are full PEM strings (BEGIN/END CERTIFICATE
 * blocks), not bare base64 DER — CertificateFactory parses PEM directly.
 */
internal class RootCertLoader(private val context: Context) {

    fun loadRoots(): List<X509Certificate> {
        val json = context.resources.openRawResource(R.raw.attestation_roots)
            .bufferedReader()
            .use(BufferedReader::readText)

        val pemEntries = JSONArray(json)
        val factory = CertificateFactory.getInstance("X.509")

        return (0 until pemEntries.length()).map { i ->
            val pem = pemEntries.getString(i)
            factory.generateCertificate(pem.byteInputStream(Charsets.UTF_8)) as X509Certificate
        }
    }
}

