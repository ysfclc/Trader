package com.yusufculcu.trader.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class ModelSourceStatus { CURRENT, CANDIDATE_REVIEW_REQUIRED, ARTIFACT_MISSING, ARTIFACT_UNVERIFIED, CHECK_FAILED }

data class ModelArtifactMetadata(val path: String, val sizeBytes: Long, val sha256: String)
data class ModelSourceSnapshot(val revision: String, val artifact: ModelArtifactMetadata?)
data class ModelUpdateResult(
    val status: ModelSourceStatus,
    val checkedAtEpochMs: Long,
    val latestRevision: String = "",
    val detail: String,
    val candidateSizeBytes: Long? = null,
    val candidateSha256: String = ""
)

object TraderModelUpdatePolicy {
    fun evaluate(spec: TraderModelSpec, snapshot: ModelSourceSnapshot, checkedAtEpochMs: Long): ModelUpdateResult {
        val artifact = snapshot.artifact
            ?: return ModelUpdateResult(ModelSourceStatus.ARTIFACT_MISSING, checkedAtEpochMs, snapshot.revision, "The immutable upstream revision does not contain the catalogued artifact. The installed copy is unchanged.")
        if (artifact.path != spec.fileName || artifact.sizeBytes <= 0L || !artifact.sha256.matches(Regex("[a-fA-F0-9]{64}"))) {
            return ModelUpdateResult(ModelSourceStatus.ARTIFACT_UNVERIFIED, checkedAtEpochMs, snapshot.revision, "Upstream did not provide valid path, byte-size and SHA-256 metadata for this artifact.")
        }
        val matchesPinnedBytes = artifact.sizeBytes == spec.sizeBytes && artifact.sha256.equals(spec.sha256, ignoreCase = true)
        if (snapshot.revision.startsWith(spec.sourceRevision, ignoreCase = true) && matchesPinnedBytes) {
            return ModelUpdateResult(ModelSourceStatus.CURRENT, checkedAtEpochMs, snapshot.revision, "Pinned revision, artifact size and source-declared SHA-256 match the reviewed catalogue.", artifact.sizeBytes, artifact.sha256)
        }
        if (matchesPinnedBytes) {
            return ModelUpdateResult(ModelSourceStatus.CURRENT, checkedAtEpochMs, snapshot.revision, "The upstream revision changed, but the artifact's byte size and SHA-256 are identical to the pinned release. No model update is needed.", artifact.sizeBytes, artifact.sha256)
        }
        return ModelUpdateResult(
            ModelSourceStatus.CANDIDATE_REVIEW_REQUIRED, checkedAtEpochMs, snapshot.revision,
            "A different artifact is published. Its upstream-declared size and SHA-256 were retrieved, but they are not independently approved. This is not installable: review the model card, license, runtime compatibility and device performance, then independently verify and pin the artifact.",
            artifact.sizeBytes, artifact.sha256
        )
    }
}

/** User-triggered immutable-release metadata check. It never downloads or installs a candidate model. */
class TraderModelUpdateChecker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("trader_model_sources", Context.MODE_PRIVATE)

    suspend fun checkAll(models: List<TraderModelSpec> = TraderModelCatalog.entries): Map<String, ModelUpdateResult> =
        withContext(Dispatchers.IO) { models.associate { model -> model.id to checkOne(model) } }

    fun cachedResult(model: TraderModelSpec): ModelUpdateResult? {
        val checkedAt = prefs.getLong(key(model, "checked_at"), 0L)
        if (checkedAt <= 0L) return null
        val status = runCatching { ModelSourceStatus.valueOf(prefs.getString(key(model, "status"), ModelSourceStatus.CHECK_FAILED.name)!!) }.getOrDefault(ModelSourceStatus.CHECK_FAILED)
        return ModelUpdateResult(
            status, checkedAt, prefs.getString(key(model, "revision"), "").orEmpty(),
            prefs.getString(key(model, "detail"), "No cached source status.").orEmpty(),
            prefs.getLong(key(model, "candidate_size"), -1L).takeIf { it > 0L },
            prefs.getString(key(model, "candidate_sha256"), "").orEmpty()
        )
    }

    private fun checkOne(model: TraderModelSpec): ModelUpdateResult {
        val now = System.currentTimeMillis()
        val result = runCatching {
            val repoParts = model.repository.split("/", limit = 2)
            require(repoParts.size == 2 && repoParts.all { it.isNotBlank() }) { "Invalid upstream repository identifier." }
            val encodedRepo = repoParts.joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20") }
            val revisionJson = getJson("https://huggingface.co/api/models/$encodedRepo/revision/main")
            val revision = revisionJson.optString("sha").takeIf { it.matches(Regex("[a-fA-F0-9]{40}")) }
                ?: throw IllegalStateException("Upstream response did not contain a valid commit revision.")
            val encodedRevision = URLEncoder.encode(revision, Charsets.UTF_8.name()).replace("+", "%20")
            val tree = getJsonArray("https://huggingface.co/api/models/$encodedRepo/tree/$encodedRevision?recursive=true&expand=true")
            val artifact = parseArtifact(tree, model.fileName)
            TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot(revision, artifact), now)
        }.getOrElse { error ->
            ModelUpdateResult(ModelSourceStatus.CHECK_FAILED, now, detail = error.message ?: "Could not verify upstream artifact metadata.")
        }
        prefs.edit()
            .putLong(key(model, "checked_at"), result.checkedAtEpochMs)
            .putString(key(model, "status"), result.status.name)
            .putString(key(model, "revision"), result.latestRevision)
            .putString(key(model, "detail"), result.detail)
            .putLong(key(model, "candidate_size"), result.candidateSizeBytes ?: -1L)
            .putString(key(model, "candidate_sha256"), result.candidateSha256)
            .apply()
        return result
    }

    private fun parseArtifact(tree: JSONArray, expectedPath: String): ModelArtifactMetadata? {
        for (index in 0 until tree.length()) {
            val item = tree.optJSONObject(index) ?: continue
            if (item.optString("path") != expectedPath || item.optString("type") != "file") continue
            val size = item.optLong("size", -1L)
            val lfs = item.optJSONObject("lfs")
            val oid = lfs?.optString("oid").orEmpty()
            val digest = oid.removePrefix("sha256:").takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) } ?: return null
            return ModelArtifactMetadata(expectedPath, size, digest)
        }
        return null
    }

    private fun getJson(url: String): JSONObject {
        val body = requestBounded(url)
        return JSONObject(body)
    }

    private fun getJsonArray(url: String): JSONArray {
        val body = requestBounded(url)
        return JSONArray(body)
    }

    private fun requestBounded(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Trader-Android-ModelCatalogue/2")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Model source returned HTTP $code")
            val declaredLength = connection.getHeaderFieldLong("Content-Length", -1L)
            require(declaredLength < 0L || declaredLength <= MAX_RESPONSE_BYTES) { "Upstream metadata response exceeded the safety limit." }
            return connection.inputStream.use { readBoundedUtf8(it, MAX_RESPONSE_BYTES) }
        } finally {
            connection.disconnect()
        }
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
