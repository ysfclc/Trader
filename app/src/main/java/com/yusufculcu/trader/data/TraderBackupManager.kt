package com.yusufculcu.trader.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, passphrase-protected backup for every persisted Trader record.
 *
 * File format: magic + format version + PBKDF2 iteration count + random salt + random GCM IV
 * + AES-GCM ciphertext/authentication tag. Header bytes are authenticated as AAD.
 * The passphrase and derived key are never persisted.
 */
class TraderBackupManager(private val database: TraderDatabase) {
    suspend fun exportEncrypted(output: OutputStream, passphrase: CharArray) = withContext(Dispatchers.IO) {
        try {
            requireValidPassphrase(passphrase)
            val snapshot = database.withTransaction {
                BackupSnapshot(
                    problems = database.problemDao().getAll(),
                    planSteps = database.planStepDao().getAll(),
                    activityEvents = database.activityDao().getAll(),
                    evidence = database.evidenceDao().getAll(),
                    workflowRuns = database.workflowRunDao().getAll(),
                    workflowSteps = database.workflowStepDao().getAll(),
                    workflowEvents = database.workflowEventDao().getAll()
                )
            }
            val cleartext = encodeSnapshot(snapshot).toByteArray(StandardCharsets.UTF_8)
            try {
                require(cleartext.size <= MAX_BACKUP_BYTES) { "Workspace is too large for one backup file." }
                val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
                val iv = ByteArray(IV_BYTES).also(random::nextBytes)
                val header = ByteBuffer.allocate(HEADER_BYTES)
                    .put(MAGIC)
                    .put(FORMAT_VERSION)
                    .putInt(KDF_ITERATIONS)
                    .put(salt)
                    .put(iv)
                    .array()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, KDF_ITERATIONS), GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.updateAAD(header)
                output.write(header)
                output.write(cipher.doFinal(cleartext))
                output.flush()
            } finally {
                cleartext.fill(0)
            }
        } finally {
            passphrase.fill(0.toChar())
        }
    }

    suspend fun restoreEncrypted(input: InputStream, passphrase: CharArray) = withContext(Dispatchers.IO) {
        var encrypted: ByteArray? = null
        try {
            requireValidPassphrase(passphrase)
            val fileBytes = readBounded(input)
            encrypted = fileBytes
            require(fileBytes.size >= HEADER_BYTES + GCM_TAG_BYTES) { "This file is not a complete Trader backup." }
            val header = fileBytes.copyOfRange(0, HEADER_BYTES)
            val headerBuffer = ByteBuffer.wrap(header)
            val magic = ByteArray(MAGIC.size)
            headerBuffer.get(magic)
            require(magic.contentEquals(MAGIC)) { "This is not a Trader backup file." }
            require(headerBuffer.get() == FORMAT_VERSION) { "This Trader backup version is not supported by this app." }
            val iterations = headerBuffer.int
            require(iterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) { "Backup security parameters are not supported." }
            val salt = ByteArray(SALT_BYTES).also { headerBuffer.get(it) }
            val iv = ByteArray(IV_BYTES).also { headerBuffer.get(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(header)
            val cleartext = try {
                cipher.doFinal(fileBytes, HEADER_BYTES, fileBytes.size - HEADER_BYTES)
            } catch (_: AEADBadTagException) {
                throw IllegalArgumentException("The passphrase is incorrect or the backup file has been damaged.")
            }
            try {
                val snapshot = decodeSnapshot(String(cleartext, StandardCharsets.UTF_8))
                validateSnapshot(snapshot)
                // One Room transaction makes replacement atomic. A validation or insert failure rolls back.
                database.withTransaction {
                    database.workflowEventDao().deleteAll()
                    database.workflowStepDao().deleteAll()
                    database.workflowRunDao().deleteAll()
                    database.planStepDao().deleteAll()
                    database.activityDao().deleteAll()
                    database.evidenceDao().deleteAll()
                    database.problemDao().deleteAll()

                    database.problemDao().insertAll(snapshot.problems)
                    database.planStepDao().insertAll(snapshot.planSteps)
                    database.activityDao().insertAll(snapshot.activityEvents)
                    database.evidenceDao().insertAll(snapshot.evidence)
                    database.workflowRunDao().insertAll(snapshot.workflowRuns)
                    database.workflowStepDao().insertAll(snapshot.workflowSteps)
                    database.workflowEventDao().insertAll(snapshot.workflowEvents)
                }
            } finally {
                cleartext.fill(0)
            }
        } finally {
            encrypted?.fill(0)
            passphrase.fill(0.toChar())
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= MAX_BACKUP_BYTES + HEADER_BYTES + GCM_TAG_BYTES) { "Backup file exceeds the supported size limit." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun requireValidPassphrase(passphrase: CharArray) {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Use a passphrase with at least $MIN_PASSPHRASE_LENGTH characters."
        }
    }

    private fun encodeSnapshot(snapshot: BackupSnapshot): String {
        val root = JSONObject()
            .put("formatVersion", SNAPSHOT_VERSION)
            .put("databaseSchemaVersion", DATABASE_SCHEMA_VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("problems", jsonArray(snapshot.problems, ::encodeProblem))
            .put("planSteps", jsonArray(snapshot.planSteps, ::encodePlanStep))
            .put("activityEvents", jsonArray(snapshot.activityEvents, ::encodeActivity))
            .put("evidence", jsonArray(snapshot.evidence, ::encodeEvidence))
            .put("workflowRuns", jsonArray(snapshot.workflowRuns, ::encodeWorkflowRun))
            .put("workflowSteps", jsonArray(snapshot.workflowSteps, ::encodeWorkflowStep))
            .put("workflowEvents", jsonArray(snapshot.workflowEvents, ::encodeWorkflowEvent))
        return root.toString()
    }

    private fun decodeSnapshot(text: String): BackupSnapshot {
        val root = JSONObject(text)
        require(root.getInt("formatVersion") == SNAPSHOT_VERSION) { "This backup format is not supported." }
        require(root.getInt("databaseSchemaVersion") == DATABASE_SCHEMA_VERSION) {
            "This backup was created for a different Trader database schema."
        }
        return BackupSnapshot(
            problems = decodeArray(root, "problems", ::decodeProblem),
            planSteps = decodeArray(root, "planSteps", ::decodePlanStep),
            activityEvents = decodeArray(root, "activityEvents", ::decodeActivity),
            evidence = decodeArray(root, "evidence", ::decodeEvidence),
            workflowRuns = decodeArray(root, "workflowRuns", ::decodeWorkflowRun),
            workflowSteps = decodeArray(root, "workflowSteps", ::decodeWorkflowStep),
            workflowEvents = decodeArray(root, "workflowEvents", ::decodeWorkflowEvent)
        )
    }

    private fun validateSnapshot(snapshot: BackupSnapshot) {
        val rowCount = snapshot.problems.size.toLong() + snapshot.planSteps.size +
            snapshot.activityEvents.size + snapshot.evidence.size + snapshot.workflowRuns.size +
            snapshot.workflowSteps.size + snapshot.workflowEvents.size
        require(rowCount <= MAX_ROWS) { "Backup contains more records than this version supports." }

        fun unique(values: List<String>, label: String) {
            require(values.size == values.toSet().size) { "Backup contains duplicate $label identifiers." }
        }
        unique(snapshot.problems.map { it.id }, "case")
        unique(snapshot.planSteps.map { it.id }, "plan-step")
        unique(snapshot.activityEvents.map { it.id }, "activity-event")
        unique(snapshot.evidence.map { it.id }, "evidence")
        unique(snapshot.workflowRuns.map { it.id }, "workflow-run")
        unique(snapshot.workflowSteps.map { it.id }, "workflow-step")
        unique(snapshot.workflowEvents.map { it.id }, "workflow-event")
        unique(snapshot.workflowRuns.map { it.idempotencyKey }, "workflow idempotency")

        val problems = snapshot.problems.map { it.id }.toSet()
        val runs = snapshot.workflowRuns.associateBy { it.id }
        val steps = snapshot.workflowSteps.associateBy { it.id }
        require(snapshot.planSteps.all { it.problemId in problems }) { "A plan step points to a missing case." }
        require(snapshot.activityEvents.all { it.problemId in problems }) { "An activity event points to a missing case." }
        require(snapshot.evidence.all { it.problemId in problems }) { "An evidence record points to a missing case." }
        require(snapshot.workflowRuns.all { it.problemId in problems }) { "A workflow points to a missing case." }
        require(snapshot.workflowSteps.all { it.workflowRunId in runs }) { "A workflow step points to a missing run." }
        require(snapshot.workflowEvents.all { event ->
            val run = runs[event.workflowRunId]
            run != null && (event.stepId == null || steps[event.stepId]?.workflowRunId == run.id)
        }) { "A workflow event points to a missing run or unrelated step." }

        requireUniquePositions(snapshot.planSteps.map { it.problemId to it.position }, "plan-step")
        requireUniquePositions(snapshot.workflowSteps.map { it.workflowRunId to it.position }, "workflow-step")
    }

    private fun requireUniquePositions(values: List<Pair<String, Int>>, label: String) {
        require(values.size == values.toSet().size) { "Backup contains duplicate $label positions." }
    }

    private fun <T> jsonArray(values: List<T>, encode: (T) -> JSONObject): JSONArray =
        JSONArray().apply { values.forEach { put(encode(it)) } }

    private fun <T> decodeArray(root: JSONObject, key: String, decode: (JSONObject) -> T): List<T> {
        val array = root.getJSONArray(key)
        require(array.length() <= MAX_ROWS) { "Backup contains too many $key records." }
        return List(array.length()) { index -> decode(array.getJSONObject(index)) }
    }

    private fun encodeProblem(row: ProblemEntity) = json(
        "id" to row.id, "title" to row.title, "description" to row.description,
        "desiredOutcome" to row.desiredOutcome, "constraintsText" to row.constraintsText,
        "category" to row.category, "status" to row.status, "inputKind" to row.inputKind,
        "originalContent" to row.originalContent, "sourceUri" to row.sourceUri,
        "outcomeNote" to row.outcomeNote, "createdAt" to row.createdAt,
        "updatedAt" to row.updatedAt, "archivedAt" to row.archivedAt
    )

    private fun decodeProblem(o: JSONObject) = ProblemEntity(
        id = o.getString("id"), title = o.getString("title"), description = o.getString("description"),
        desiredOutcome = o.getString("desiredOutcome"), constraintsText = o.getString("constraintsText"),
        category = o.getString("category"), status = o.getString("status"), inputKind = o.getString("inputKind"),
        originalContent = o.getString("originalContent"), sourceUri = o.stringOrNull("sourceUri"),
        outcomeNote = o.getString("outcomeNote"), createdAt = o.getLong("createdAt"),
        updatedAt = o.getLong("updatedAt"), archivedAt = o.longOrNull("archivedAt")
    )

    private fun encodePlanStep(row: PlanStepEntity) = json(
        "id" to row.id, "problemId" to row.problemId, "title" to row.title,
        "rationale" to row.rationale, "requiredCapability" to row.requiredCapability,
        "expectedResult" to row.expectedResult, "status" to row.status,
        "position" to row.position, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )

    private fun decodePlanStep(o: JSONObject) = PlanStepEntity(
        id = o.getString("id"), problemId = o.getString("problemId"), title = o.getString("title"),
        rationale = o.getString("rationale"), requiredCapability = o.getString("requiredCapability"),
        expectedResult = o.getString("expectedResult"), status = o.getString("status"),
        position = o.getInt("position"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun encodeActivity(row: ActivityEventEntity) = json(
        "id" to row.id, "problemId" to row.problemId, "eventType" to row.eventType,
        "summary" to row.summary, "details" to row.details, "occurredAt" to row.occurredAt
    )

    private fun decodeActivity(o: JSONObject) = ActivityEventEntity(
        id = o.getString("id"), problemId = o.getString("problemId"), eventType = o.getString("eventType"),
        summary = o.getString("summary"), details = o.getString("details"), occurredAt = o.getLong("occurredAt")
    )

    private fun encodeEvidence(row: EvidenceEntity) = json(
        "id" to row.id, "problemId" to row.problemId, "title" to row.title, "content" to row.content,
        "sourceUri" to row.sourceUri, "sourceKind" to row.sourceKind,
        "verificationState" to row.verificationState, "capturedAt" to row.capturedAt
    )

    private fun decodeEvidence(o: JSONObject) = EvidenceEntity(
        id = o.getString("id"), problemId = o.getString("problemId"), title = o.getString("title"),
        content = o.getString("content"), sourceUri = o.stringOrNull("sourceUri"),
        sourceKind = o.getString("sourceKind"), verificationState = o.getString("verificationState"),
        capturedAt = o.getLong("capturedAt")
    )

    private fun encodeWorkflowRun(row: WorkflowRunEntity) = json(
        "id" to row.id, "problemId" to row.problemId, "title" to row.title, "status" to row.status,
        "idempotencyKey" to row.idempotencyKey, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        "startedAt" to row.startedAt, "finishedAt" to row.finishedAt, "lastError" to row.lastError
    )

    private fun decodeWorkflowRun(o: JSONObject) = WorkflowRunEntity(
        id = o.getString("id"), problemId = o.getString("problemId"), title = o.getString("title"),
        status = o.getString("status"), idempotencyKey = o.getString("idempotencyKey"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
        startedAt = o.longOrNull("startedAt"), finishedAt = o.longOrNull("finishedAt"),
        lastError = o.stringOrNull("lastError")
    )

    private fun encodeWorkflowStep(row: WorkflowStepEntity) = json(
        "id" to row.id, "workflowRunId" to row.workflowRunId, "title" to row.title,
        "capabilityId" to row.capabilityId, "inputJson" to row.inputJson,
        "expectedPostcondition" to row.expectedPostcondition, "status" to row.status,
        "position" to row.position, "retrySafe" to row.retrySafe, "requiresApproval" to row.requiresApproval,
        "payloadHash" to row.payloadHash, "approvedPayloadHash" to row.approvedPayloadHash,
        "approvedAt" to row.approvedAt, "attemptCount" to row.attemptCount, "lastError" to row.lastError,
        "createdAt" to row.createdAt, "updatedAt" to row.updatedAt,
        "startedAt" to row.startedAt, "finishedAt" to row.finishedAt
    )

    private fun decodeWorkflowStep(o: JSONObject) = WorkflowStepEntity(
        id = o.getString("id"), workflowRunId = o.getString("workflowRunId"), title = o.getString("title"),
        capabilityId = o.getString("capabilityId"), inputJson = o.getString("inputJson"),
        expectedPostcondition = o.getString("expectedPostcondition"), status = o.getString("status"),
        position = o.getInt("position"), retrySafe = o.getBoolean("retrySafe"),
        requiresApproval = o.getBoolean("requiresApproval"), payloadHash = o.getString("payloadHash"),
        approvedPayloadHash = o.stringOrNull("approvedPayloadHash"), approvedAt = o.longOrNull("approvedAt"),
        attemptCount = o.getInt("attemptCount"), lastError = o.stringOrNull("lastError"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
        startedAt = o.longOrNull("startedAt"), finishedAt = o.longOrNull("finishedAt")
    )

    private fun encodeWorkflowEvent(row: WorkflowEventEntity) = json(
        "id" to row.id, "workflowRunId" to row.workflowRunId, "stepId" to row.stepId,
        "eventType" to row.eventType, "fromStatus" to row.fromStatus, "toStatus" to row.toStatus,
        "summary" to row.summary, "details" to row.details, "occurredAt" to row.occurredAt
    )

    private fun decodeWorkflowEvent(o: JSONObject) = WorkflowEventEntity(
        id = o.getString("id"), workflowRunId = o.getString("workflowRunId"), stepId = o.stringOrNull("stepId"),
        eventType = o.getString("eventType"), fromStatus = o.stringOrNull("fromStatus"),
        toStatus = o.stringOrNull("toStatus"), summary = o.getString("summary"),
        details = o.getString("details"), occurredAt = o.getLong("occurredAt")
    )

    private fun json(vararg values: Pair<String, Any?>) = JSONObject().apply {
        values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }

    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    private data class BackupSnapshot(
        val problems: List<ProblemEntity>,
        val planSteps: List<PlanStepEntity>,
        val activityEvents: List<ActivityEventEntity>,
        val evidence: List<EvidenceEntity>,
        val workflowRuns: List<WorkflowRunEntity>,
        val workflowSteps: List<WorkflowStepEntity>,
        val workflowEvents: List<WorkflowEventEntity>
    )

    companion object {
        private const val SNAPSHOT_VERSION = 1
        private const val DATABASE_SCHEMA_VERSION = 3
        private const val FORMAT_VERSION: Byte = 1
        private val MAGIC = "TRDBKUP!".toByteArray(StandardCharsets.US_ASCII)
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES + IV_BYTES
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = 16
        private const val KDF_ITERATIONS = 210_000
        private const val MIN_KDF_ITERATIONS = 100_000
        private const val MAX_KDF_ITERATIONS = 1_000_000
        private const val MIN_PASSPHRASE_LENGTH = 12
        private const val MAX_BACKUP_BYTES = 49 * 1024 * 1024
        private const val MAX_ROWS = 100_000
        private val random = SecureRandom()
    }
}
