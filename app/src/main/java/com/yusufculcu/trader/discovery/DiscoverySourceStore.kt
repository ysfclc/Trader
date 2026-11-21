package com.yusufculcu.trader.discovery

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DiscoverySourceConfig(
    val id: String,
    val adapterId: String,
    val name: String,
    val feedUrl: String,
    val enabled: Boolean = true,
    val lastCheckedAt: Long? = null,
    val lastError: String? = null
)

/** Local user-owned source instances. Credentials are deliberately unsupported in this first version. */
class DiscoverySourceStore(context: Context, preferencesName: String = PREFERENCES) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    @Synchronized
    fun list(): List<DiscoverySourceConfig> {
        val raw = preferences.getString(KEY_SOURCES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                DiscoverySourceConfig(
                    id = item.getString("id"),
                    adapterId = item.optString("adapterId", RssAtomFeedAdapter.ADAPTER_ID),
                    name = item.getString("name"),
                    feedUrl = item.getString("feedUrl"),
                    enabled = item.optBoolean("enabled", true),
                    lastCheckedAt = item.takeIf { it.has("lastCheckedAt") && !it.isNull("lastCheckedAt") }?.optLong("lastCheckedAt"),
                    lastError = item.optString("lastError").takeIf { it.isNotBlank() }
                )
            }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun add(name: String, feedUrl: String): DiscoverySourceConfig {
        val cleanName = name.trim().take(100)
        require(cleanName.isNotBlank()) { "Give this source a name." }
        val cleanUrl = RssAtomFeedAdapter.validateFeedUrl(feedUrl).toString()
        val existing = list().firstOrNull { it.feedUrl.equals(cleanUrl, ignoreCase = true) }
        if (existing != null) return existing
        val source = DiscoverySourceConfig(
            id = UUID.randomUUID().toString(),
            adapterId = RssAtomFeedAdapter.ADAPTER_ID,
            name = cleanName,
            feedUrl = cleanUrl
        )
        persist(list() + source)
        return source
    }

    /** Replace the complete portable source list after validating every item. */
    @Synchronized
    fun replaceAll(sources: List<DiscoverySourceConfig>) {
        require(sources.size <= MAX_SOURCES) { "This backup contains too many discovery sources." }
        val normalized = sources.map { source ->
            require(source.id.isNotBlank() && source.id.length <= 128) { "A discovery source has an invalid identifier." }
            require(source.adapterId == RssAtomFeedAdapter.ADAPTER_ID) { "This backup contains an unsupported source adapter." }
            val name = source.name.trim().take(100)
            require(name.isNotBlank()) { "A discovery source is missing its name." }
            val url = RssAtomFeedAdapter.validateFeedUrl(source.feedUrl).toString()
            require(source.lastCheckedAt == null || source.lastCheckedAt >= 0) { "A discovery source has an invalid check timestamp." }
            source.copy(name = name, feedUrl = url, lastError = source.lastError?.take(300))
        }
        require(normalized.map { it.id }.distinct().size == normalized.size) { "Discovery source identifiers must be unique." }
        require(normalized.map { it.feedUrl.lowercase() }.distinct().size == normalized.size) { "Discovery feed URLs must be unique." }
        persist(normalized)
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) =
        persist(list().map { if (it.id == id) it.copy(enabled = enabled) else it })

    @Synchronized
    fun remove(id: String) = persist(list().filterNot { it.id == id })

    @Synchronized
    fun markChecked(id: String, at: Long, error: String? = null) =
        persist(list().map {
            if (it.id == id) it.copy(lastCheckedAt = at, lastError = error?.take(300)) else it
        })

    private fun persist(sources: List<DiscoverySourceConfig>) {
        val array = JSONArray()
        sources.forEach { source ->
            array.put(
                JSONObject()
                    .put("id", source.id)
                    .put("adapterId", source.adapterId)
                    .put("name", source.name)
                    .put("feedUrl", source.feedUrl)
                    .put("enabled", source.enabled)
                    .put("lastCheckedAt", source.lastCheckedAt ?: JSONObject.NULL)
                    .put("lastError", source.lastError ?: "")
            )
        }
        check(preferences.edit().putString(KEY_SOURCES, array.toString()).commit()) {
            "Could not save discovery sources locally."
        }
    }

    companion object {
        private const val PREFERENCES = "trader_discovery_sources"
        private const val KEY_SOURCES = "configured_public_feeds"
        private const val MAX_SOURCES = 500
    }
}
