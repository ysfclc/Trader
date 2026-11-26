package com.yusufculcu.trader.ai

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** Trader-owned private model store. Only catalog-pinned models become runnable. */
class TraderModelStore(context: Context) {
    enum class Status { NOT_INSTALLED, DOWNLOADING, VERIFYING, INSTALLED, PAUSED, FAILED }
    data class ModelState(val status: Status, val progress: Float = 0f, val detail: String = "")

    private val app = context.applicationContext
    private val dir = File(app.filesDir, "trader-models").apply { mkdirs() }
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "trader-model-store") }
    private val cancellation = ConcurrentHashMap<String, Boolean>()
    private val mutableStates = MutableStateFlow(TraderModelCatalog.entries.associate { spec ->
        spec.id to if (installedFile(spec) != null) ModelState(Status.INSTALLED, 1f, "Verified local model")
        else ModelState(Status.NOT_INSTALLED, 0f, "Not installed")
    })
    val states: StateFlow<Map<String, ModelState>> = mutableStates
    private val prefs = app.getSharedPreferences("trader_models", Context.MODE_PRIVATE)

    fun activeModelId(): String? = prefs.getString("active_model", null)?.takeIf { installedFile(TraderModelCatalog.find(it) ?: return null) != null }

    fun outputTokenLimit(spec: TraderModelSpec): Int =
        prefs.getInt("output_tokens_${spec.id}", spec.maxOutputTokens)
            .coerceIn(MIN_OUTPUT_TOKENS, spec.maxOutputTokens)

    fun setOutputTokenLimit(spec: TraderModelSpec, value: Int): Int {
        val safe = value.coerceIn(MIN_OUTPUT_TOKENS, spec.maxOutputTokens)
        prefs.edit().putInt("output_tokens_${spec.id}", safe).apply()
        return safe
    }
    fun setActiveModel(id: String): Boolean {
        val spec = TraderModelCatalog.find(id) ?: return false
        if (installedFile(spec) == null) return false
        prefs.edit().putString("active_model", id).apply()
        return true
    }
    fun activeModelFile(): File? = activeModelId()?.let { installedFile(TraderModelCatalog.find(it)!!) }

    fun installedFile(spec: TraderModelSpec): File? {
        val file = File(dir, spec.fileName)
        val sidecar = File(dir, spec.fileName + ".sha256")
        return file.takeIf { it.isFile && it.length() == spec.sizeBytes && sidecar.isFile && runCatching { sidecar.readText().trim() == spec.sha256 }.getOrDefault(false) }
    }

    fun hasEnoughStorage(spec: TraderModelSpec): Boolean {
        val stats = StatFs(dir.absolutePath)
        val partial = File(dir, spec.fileName + ".part").length()
        return stats.availableBytes >= (spec.sizeBytes - partial).coerceAtLeast(0L) + STORAGE_RESERVE_BYTES
    }

    fun hasRamHeadroom(spec: TraderModelSpec): Boolean {
        if (spec.minimumFreeRamBytes <= 0L) return true
        val info = ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        return info.availMem >= spec.minimumFreeRamBytes
    }

    fun download(spec: TraderModelSpec) {
        if (!spec.isValidCatalogEntry() || installedFile(spec) != null) return
        if (states.value[spec.id]?.status == Status.DOWNLOADING || states.value[spec.id]?.status == Status.VERIFYING) return
        cancellation[spec.id] = false
        publish(spec.id, ModelState(Status.DOWNLOADING, detail = "Connecting to pinned source"))
        worker.execute { downloadAndVerify(spec) }
    }
    fun pause(id: String) { cancellation[id] = true }

    private fun downloadAndVerify(spec: TraderModelSpec) {
        val partial = File(dir, spec.fileName + ".part")
        val target = File(dir, spec.fileName)
        var connection: HttpURLConnection? = null
        try {
            if (!hasEnoughStorage(spec)) throw IllegalStateException("Not enough free storage for this model and safety reserve.")
            val offset = partial.length().coerceAtMost(spec.sizeBytes)
            if (offset >= spec.sizeBytes) publish(spec.id, ModelState(Status.VERIFYING, 1f, "Verifying existing partial file"))
            else publish(spec.id, ModelState(Status.DOWNLOADING, (offset.toFloat() / spec.sizeBytes), "Resuming at ${formatBytes(offset)}"))
            connection = (URL(spec.pinnedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 45_000; instanceFollowRedirects = true
                if (offset > 0 && offset < spec.sizeBytes) setRequestProperty("Range", "bytes=$offset-")
            }
            val status = connection.responseCode
            if (status !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) throw IllegalStateException("Model source returned HTTP $status")
            val append = offset > 0 && status == HttpURLConnection.HTTP_PARTIAL
            val start = if (append) offset else 0L
            if (!append && partial.exists()) partial.delete()
            connection.inputStream.use { input ->
                FileOutputStream(partial, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = start
                    while (true) {
                        if (cancellation[spec.id] == true) throw DownloadPausedException()
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > spec.sizeBytes) throw IllegalStateException("Download exceeded pinned model size.")
                        output.write(buffer, 0, read)
                        if (total % (2 * 1024 * 1024) < read) publish(spec.id, ModelState(Status.DOWNLOADING, total.toFloat() / spec.sizeBytes, "${formatBytes(total)} / ${formatBytes(spec.sizeBytes)}"))
                    }
                    output.fd.sync()
                }
            }
            if (partial.length() != spec.sizeBytes) throw IllegalStateException("Incomplete download: ${partial.length()} of ${spec.sizeBytes} bytes")
            publish(spec.id, ModelState(Status.VERIFYING, 1f, "Checking SHA-256 integrity"))
            val digest = sha256(partial)
            if (!digest.equals(spec.sha256, ignoreCase = true)) { partial.delete(); throw IllegalStateException("SHA-256 verification failed; partial file discarded.") }
            if (target.exists() && !target.delete()) throw IllegalStateException("Could not replace previous model file")
            if (!partial.renameTo(target)) throw IllegalStateException("Verified file could not be installed")
            File(dir, spec.fileName + ".sha256").writeText(spec.sha256)
            publish(spec.id, ModelState(Status.INSTALLED, 1f, "Downloaded and SHA-256 verified"))
            if (activeModelId() == null) setActiveModel(spec.id)
        } catch (_: DownloadPausedException) {
            publish(spec.id, ModelState(Status.PAUSED, partial.length().toFloat() / spec.sizeBytes, "Paused; partial download retained"))
        } catch (error: Exception) {
            publish(spec.id, ModelState(Status.FAILED, detail = error.message ?: "Model download failed"))
        } finally {
            connection?.disconnect()
            cancellation.remove(spec.id)
        }
    }

    private fun publish(id: String, state: ModelState) {
        mutableStates.value = mutableStates.value.toMutableMap().apply { put(id, state) }
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun formatBytes(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000f)

    private class DownloadPausedException : Exception()
    companion object { private const val STORAGE_RESERVE_BYTES = 150L * 1024 * 1024; const val MIN_OUTPUT_TOKENS = 128 }
}