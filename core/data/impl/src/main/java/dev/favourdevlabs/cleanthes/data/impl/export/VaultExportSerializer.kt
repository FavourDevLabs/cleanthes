package dev.favourdevlabs.cleanthes.data.impl.export

import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes/deserializes a list of VaultItem to/from a JSON string.
 * Used exclusively for vault export/import — the resulting JSON is
 * always encrypted before it touches disk, never written in plaintext.
 */
object VaultExportSerializer {
    private const val FORMAT_VERSION = 1

    fun serialize(items: List<VaultItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj =
                JSONObject().apply {
                    put("title", item.title)
                    put("username", item.username)
                    put("password", item.password)
                    put("website", item.website ?: JSONObject.NULL)
                    put("category", item.category)
                    put("notes", item.notes ?: JSONObject.NULL)
                    put("isFavorite", item.isFavorite)
                    put("totpSecret", item.totpSecret ?: JSONObject.NULL)
                    put("totpIssuer", item.totpIssuer ?: JSONObject.NULL)
                    put("totpDigits", item.totpDigits)
                    put("totpPeriod", item.totpPeriod)
                    put("totpAlgorithm", item.totpAlgorithm)
                }
            array.put(obj)
        }
        val root =
            JSONObject().apply {
                put("formatVersion", FORMAT_VERSION)
                put("entries", array)
            }
        return root.toString()
    }

    fun deserialize(json: String): List<VaultItem> {
        val root = JSONObject(json)
        val array = root.getJSONArray("entries")
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            VaultItem(
                title = obj.getString("title"),
                username = obj.getString("username"),
                password = obj.getString("password"),
                website = obj.optStringOrNull("website"),
                category = obj.getString("category"),
                notes = obj.optStringOrNull("notes"),
                isFavorite = obj.optBoolean("isFavorite", false),
                totpSecret = obj.optStringOrNull("totpSecret"),
                totpIssuer = obj.optStringOrNull("totpIssuer"),
                totpDigits = obj.optInt("totpDigits", 6),
                totpPeriod = obj.optInt("totpPeriod", 30),
                totpAlgorithm = obj.optString("totpAlgorithm", "SHA1"),
            )
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
}
