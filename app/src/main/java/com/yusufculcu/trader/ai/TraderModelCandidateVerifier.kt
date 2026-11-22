package com.yusufculcu.trader.ai

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

private const val CANDIDATE_PROGRESS_INTERVAL_BYTES = 8L * 1024 * 1024

enum class CandidateVerificationStatus { DOWNLOADING, VERIFYING, BYTES_VERIFIED, CANCELLED, FAILED }

data class CandidateVerificationState(
    val status: CandidateVerificationStatus,
    val progress: Float = 0f,
    val detail: String,
    val revision: String,
    val actualSha256: String = ""
)

/**
 * Downloads a user-selected immutable candidate into app-private temporary storage,
 * hashes the actual stream, and always deletes the temporary file. It never installs,
 * activates, or repins a model.
 */
class TraderModelCandidateVerifier(context: Context) {
    private val app = context.applicationContext
    private val stagingDir = File(app.cacheDir, "trader-candidate-verification").apply { mkdirs() }
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "trader-candidate-verifier") }
    private val cancellation = ConcurrentHashMap<String, Boolean>()
    private val mutableStates = MutableStateFlow<Map<String, CandidateVerificationState>>(emptyMap())
    val states: StateFlow<Map<String, CandidateVerificationState>> = mutableStates

    init {
        // A process-death or force-stop must not leave large abandoned candidate files behind.
        stagingDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }

    fun hasEnoughStorage(candidate: ModelUpdateResult): Boolean {
        val expectedBytes = candidate.candidateSizeBytes ?: return false
        if (expectedBytes <= 0L || expectedBytes > MAX_CANDIDATE_BYTES) return false
        return runCatching {
            StatFs(stagingDir.absolutePath).availableBytes >= expectedBytes + STORAGE_RESERVE_BYTES
        }.getOrDefault(false)
    }

    fun verify(spec: TraderModelSpec, candidate: ModelUpdateResult) {
        val expectedBytes = candidate.candidateSizeBytes
        val digest = candidate.candidateSha256
        val valid = spec.isValidCatalogEntry() &&
            spec.id.matches(Regex("[a-z0-9][a-z0-9-]{0,79}")) &&
            candidate.status == ModelSourceStatus.CANDIDATE_REVIEW_REQUIRED &&
            candidate.latestRevision.matches(Regex("[a-fA-F0-9]{40}")) &&
            expectedBytes != null && expectedBytes in 1L..MAX_CANDIDATE_BYTES &&
            digest.matches(Regex("[a-fA-F0-9]{64}")) &&
            (expectedBytes != spec.sizeBytes || !digest.equals(spec.sha256, ignoreCase = true))
        if (!valid) {
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.FAILED, detail = "No valid changed candidate is available for byte verification.",
                revision = candidate.latestRevision
            ))
            return
        }
        val anotherVerificationRunning = mutableStates.value.any { (id, state) ->
            id != spec.id && (state.status == CandidateVerificationStatus.DOWNLOADING ||
                state.status == CandidateVerificationStatus.VERIFYING)
        }
        if (anotherVerificationRunning) {
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.FAILED,
                detail = "Another candidate verification is already running. Cancel or finish it before starting another.",
                revision = candidate.latestRevision
            ))
            return
        }
        val current = mutableStates.value[spec.id]?.status
        if (current == CandidateVerificationStatus.DOWNLOADING || current == CandidateVerificationStatus.VERIFYING) return
        if (!hasEnoughStorage(candidate)) {
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.FAILED, detail = "Not enough free storage for the full candidate plus the 150 MB safety reserve.",
                revision = candidate.latestRevision
            ))
            return
        }
        cancellation[spec.id] = false
        publish(spec.id, CandidateVerificationState(
            CandidateVerificationStatus.DOWNLOADING, detail = "Downloading the immutable candidate for temporary verification.",
            revision = candidate.latestRevision
        ))
        executor.execute { downloadAndVerify(spec, candidate, expectedBytes!!, digest) }
    }

    fun cancel(modelId: String) {
        cancellation[modelId] = true
    }

    private fun downloadAndVerify(spec: TraderModelSpec, candidate: ModelUpdateResult, expectedBytes: Long, expectedSha256: String) {
        val temporaryFile = File(stagingDir, "${spec.id}.candidate.tmp")
        var connection: HttpURLConnection? = null
        var verifiedSha256: String? = null
        try {
            if (temporaryFile.exists() && !temporaryFile.delete()) {
                throw IllegalStateException("Could not clear a previous temporary candidate file.")
            }
            connection = (URL(candidateUrl(spec, candidate.latestRevision)).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "Trader-Android-CandidateVerifier/1")
            }
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Candidate source returned HTTP $responseCode.")
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength >= 0L && declaredLength != expectedBytes) {
                throw IllegalStateException("Candidate Content-Length differs from the metadata-checked byte size.")
            }

            val actualSha256 = connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val hash = copyCandidateAndHash(
                        input = input,
                        output = output,
                        expectedSizeBytes = expectedBytes,
                        expectedSha256 = expectedSha256,
                        shouldCancel = { cancellation[spec.id] == true },
                        onProgress = { total ->
                            publish(spec.id, CandidateVerificationState(
                                CandidateVerificationStatus.DOWNLOADING,
                                (total.toDouble() / expectedBytes.toDouble()).toFloat().coerceIn(0f, 1f),
                                "${formatBytes(total)} / ${formatBytes(expectedBytes)} downloaded",
                                candidate.latestRevision
                            ))
                        }
                    )
                    output.fd.sync()
                    hash
                }
            }
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.VERIFYING, 1f, "Checking final byte count and SHA-256.",
                candidate.latestRevision
            ))
            check(temporaryFile.isFile && temporaryFile.length() == expectedBytes) {
                "Temporary candidate file does not match the expected byte size."
            }
            if (cancellation[spec.id] == true) throw CandidateVerificationCancelledException()
            verifiedSha256 = actualSha256
        } catch (_: CandidateVerificationCancelledException) {
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.CANCELLED, detail = "Verification cancelled. Temporary candidate data was discarded.",
                revision = candidate.latestRevision
            ))
        } catch (error: Exception) {
            publish(spec.id, CandidateVerificationState(
                CandidateVerificationStatus.FAILED,
                detail = error.message ?: "Candidate byte verification failed. Temporary candidate data was discarded.",
                revision = candidate.latestRevision
            ))
        } finally {
            connection?.disconnect()
            val cleanupSucceeded = !temporaryFile.exists() || temporaryFile.delete()
            cancellation.remove(spec.id)
            if (!cleanupSucceeded) {
                publish(spec.id, CandidateVerificationState(
                    CandidateVerificationStatus.FAILED,
                    detail = "Candidate bytes were checked, but Android could not remove the temporary file. Clear Trader's app cache before retrying.",
                    revision = candidate.latestRevision
                ))
            } else if (verifiedSha256 != null) {
                publish(spec.id, CandidateVerificationState(
                    CandidateVerificationStatus.BYTES_VERIFIED, 1f,
                    "Downloaded bytes match the immutable source metadata. The temporary file was deleted. This candidate is still not approved or installable.",
                    candidate.latestRevision, verifiedSha256
                ))
            }
        }
    }

    private fun candidateUrl(spec: TraderModelSpec, revision: String): String {
        val repoParts = spec.repository.split("/")
        require(repoParts.size == 2 && repoParts.all { it.isNotBlank() }) { "Invalid upstream repository identifier." }
        val fileParts = spec.fileName.split("/")
        require(fileParts.isNotEmpty() && fileParts.all { it.isNotBlank() && it != "." && it != ".." }) {
            "Invalid candidate artifact path."
        }
        val repo = repoParts.joinToString("/") { encodePathSegment(it) }
        val file = fileParts.joinToString("/") { encodePathSegment(it) }
        return "https://huggingface.co/$repo/resolve/$revision/$file?download=true"
    }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun publish(id: String, state: CandidateVerificationState) {
        mutableStates.value = mutableStates.value.toMutableMap().apply { put(id, state) }
    }

    private fun formatBytes(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000f)

    companion object {
        private const val STORAGE_RESERVE_BYTES = 150L * 1024 * 1024
        private const val MAX_CANDIDATE_BYTES = 8L * 1024 * 1024 * 1024
    }
}

internal class CandidateVerificationCancelledException : Exception("Candidate verification was cancelled.")

/** Streams to temporary storage while hashing, rejecting overlong, short, cancelled or mismatched artifacts. */
internal fun copyCandidateAndHash(
    input: InputStream,
    output: OutputStream,
    expectedSizeBytes: Long,
    expectedSha256: String,
    shouldCancel: () -> Boolean,
    onProgress: (Long) -> Unit = {}
): String {
    require(expectedSizeBytes > 0L) { "Expected candidate size must be positive." }
    require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Expected candidate SHA-256 is invalid." }
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    var lastProgressReport = 0L
    while (true) {
        if (shouldCancel()) throw CandidateVerificationCancelledException()
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        check(total <= expectedSizeBytes) { "Downloaded candidate exceeded the metadata-checked byte size." }
        digest.update(buffer, 0, count)
        output.write(buffer, 0, count)
        if (total - lastProgressReport >= CANDIDATE_PROGRESS_INTERVAL_BYTES || total == expectedSizeBytes) {
            onProgress(total)
            lastProgressReport = total
        }
    }
    check(total == expectedSizeBytes) { "Incomplete candidate download: $total of $expectedSizeBytes bytes." }
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    check(actual.equals(expectedSha256, ignoreCase = true)) {
        "Downloaded candidate SHA-256 does not match the upstream-declared digest."
    }
    return actual
}
