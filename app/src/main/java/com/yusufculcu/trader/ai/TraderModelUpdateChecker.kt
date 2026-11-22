package com.yusufculcu.trader.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.ByteArrayOutputStream
import java.io.InputStream

enum class ModelSourceStatus { CURRENT, UPDATE_AVAILABLE, ARTIFACT_MISSING, CHECK_FAILED }

data class ModelSourceSnapshot(val revision: String, val containsPinnedArtifact: Boolean)

data class ModelUpdateResult(val status: ModelSourceStatus, val checkedAtEpochMs: Long, val latestRevision: String = "", val detail: String)

object TraderModelUpdatePolicy {
    fun evaluate(spec: TraderModelSpec, snapshot: ModelSourceSnapshot, checkedAtEpochMs: Long): ModelUpdateResult {
        if (!snapshot.containsPinnedArtifact) return ModelUpdateResult(ModelSourceStatus.ARTIFACT_MISSING, checkedAtEpochMs, snapshot.revision, "The upstream default branch no longer lists the pinned artifact. The installed copy is not changed.")
        return if (snapshot.revision.startsWith(spec.sourceRevision, ignoreCase = true)) ModelUpdateResult(ModelSourceStatus.CURRENT, checkedAtEpochMs, snapshot.revision, "The pinned revision matches the upstream default branch.") else ModelUpdateResult(ModelSourceStatus.UPDATE_AVAILABLE, checkedAtEpochMs, snapshot.revision, "A different upstream revision exists. It is only a candidate: compatibility, license, artifact size and SHA-256 must be reviewed before Trader can offer an update.")
    }
}

/** User-triggered metadata check. It never downloads or installs a moving upstream artifact. */
class TraderModelUpdateChecker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("trader_model_sources", Context.MODE_PRIVATE)

    suspend fun checkAll(models: List<TraderModelSpec> = TraderModelCatalog.entries): Map<String, ModelUpdateResult> = withContext(Dispatchers.IO) { models.associate { model -> model.id to checkOne(model) } }

    fun cachedResult(model: TraderModelSpec): ModelUpdateResult? {
        val checkedAt = prefs.getLong(key(model, "checked_at"), 0L)
        if (checkedAt <= 0L) return null
        val status = runCatching { ModelSourceStatus.valueOf(prefs.getString(key(model, "status"), ModelSourceStatus.CHECK_FAILED.name)!!) }.getOrDefault(ModelSourceStatus.CHECK_FAILED)
        return ModelUpdateResult(status, checkedAt, prefs.getString(key(model, "revision"), "").orEmpty(), prefs.getString(key(model, "detail"), "No cached source status.").orEmpty())
    }

    private fun checkOne(model: TraderModelSpec): ModelUpdateResult {
        val now = System.currentTimeMillis()
        val result = runCatching {
            val repo = model.repository.split("/", limit = 2)
            require(repo.size == 2 && repo.all { it.isNotBlank() }) { "Invalid upstream repository identifier." }
            val encodedRepo = repo.joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20") }
            val endpoint = URL("https://huggingface.co/api/models/$encodedRepo/revision/main")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply { requestMethod = "GET"; connectTimeout = 12_000; readTimeout = 12_000; setRequestProperty("Accept", "application/json"); setRequestProperty("User-Agent", "Trader-Android-ModelCatalogue/1") }
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException("Model source returned HTTP $code")
                val declaredLength = connection.getHeaderFieldLong("Content-Length", -1L)
                require(declaredLength < 0L || declaredLength <= MAX_RESPONSE_BYTES) { "Upstream metadata response exceeded the safety limit." }
                val body = connection.inputStream.use { readBoundedUtf8(it, MAX_RESPONSE_BYTES) }
                val json = JSONObject(body)
                val revision = json.optString("sha").takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) } ?: throw IllegalStateException("Upstream response did not contain a valid commit revision.")
                val siblings = json.optJSONArray("siblings")
                val found = (0 until (siblings?.length() ?: 0)).any { index -> siblings?.optJSONObject(index)?.optString("rfilename") == model.fileName }
                TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot(revision, found), now)
            } finally { connection.disconnect() }
        }.getOrElse { error -> ModelUpdateResult(ModelSourceStatus.CHECK_FAILED, now, detail = error.message ?: "Could not check the upstream model source.") }
        prefs.edit().putLong(key(model, "checked_at"), result.checkedAtEpochMs).putString(key(model, "status"), result.status.name).putString(key(model, "revision"), result.latestRevision).putString(key(model, "detail"), result.detail).apply()
        return result
    }

    private fun key(model: TraderModelSpec, field: String) = "${model.id}_$field"
    companion object { private const val MAX_RESPONSE_BYTES = 1_000_000L }
}

/** Reads no more than maxBytes, even when the server omits or lies about Content-Length. */
internal fun readBoundedUtf8(input: InputStream, maxBytes: Long): String {
    require(maxBytes >= 0L) { "Maximum response size must not be negative." }
    val output = ByteArrayOutputStream(minOf(maxBytes, 8_192L).toInt())
    val buffer = ByteArray(8_192)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "Upstream metadata response exceeded the safety limit." }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}
}