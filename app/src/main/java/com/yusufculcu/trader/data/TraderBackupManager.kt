package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.domain.OpportunityRules
import com.yusufculcu.trader.domain.DiscoveryStrategyRules
import com.yusufculcu.trader.discovery.DiscoverySourceConfig
import com.yusufculcu.trader.discovery.DiscoverySourceStore
import com.yusufculcu.trader.discovery.RssAtomFeedAdapter
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
class TraderBackupManager(
    private val database: TraderDatabase,
    private val discoverySourceStore: DiscoverySourceStore? = null
) {
    suspend fun exportEncrypted(output: OutputStream, passphrase: CharArray) = withContext(Dispatchers.IO) {
        try {
            requireValidPassphrase(passphrase)
            val discoverySources = discoverySourceStore?.list().orEmpty()
            val snapshot = database.withTransaction {
                BackupSnapshot(
                    problems = database.problemDao().getAll(),
                    planSteps = database.planStepDao().getAll(),
                    activityEvents = database.activityDao().getAll(),
                    evidence = database.evidenceDao().getAll(),
                    opportunities = database.opportunityDao().getAll(),
                    workflowRuns = database.workflowRunDao().getAll(),
                    workflowSteps = database.workflowStepDao().getAll(),
                    workflowEvents = database.workflowEventDao().getAll(),
                    discoverySources = discoverySources,
                    proofAssets = database.proofAssetDao().getAll(),
                    discoveryStrategies = database.discoveryStrategyDao().getAll()
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
                require(snapshot.discoverySources.isEmpty() || discoverySourceStore != null) {
                    "This backup contains discovery sources; use the full Trader app restore flow."
                }
                val previousSources = discoverySourceStore?.list()
                try {
                    // SharedPreferences cannot join Room's transaction; apply settings first and compensate on DB failure.
                    discoverySourceStore?.replaceAll(snapshot.discoverySources)
                    database.withTransaction {
                        database.workflowEventDao().deleteAll()
                        database.discoveryStrategyDao().deleteAll()
                        database.proofAssetDao().deleteAll()
                        database.workflowStepDao().deleteAll()
                        database.workflowRunDao().deleteAll()
                        database.planStepDao().deleteAll()
                        database.activityDao().deleteAll()
                        database.evidenceDao().deleteAll()
                        database.opportunityDao().deleteAll()
                        database.problemDao().deleteAll()

                        database.problemDao().insertAll(snapshot.problems)
                        database.opportunityDao().insertAll(snapshot.opportunities)
                        database.planStepDao().insertAll(snapshot.planSteps)
                        database.activityDao().insertAll(snapshot.activityEvents)
                        database.evidenceDao().insertAll(snapshot.evidence)
                        database.workflowRunDao().insertAll(snapshot.workflowRuns)
                        database.workflowStepDao().insertAll(snapshot.workflowSteps)
                        database.workflowEventDao().insertAll(snapshot.workflowEvents)
                        database.proofAssetDao().insertAll(snapshot.proofAssets)
                        database.discoveryStrategyDao().insertAll(snapshot.discoveryStrategies)
                    }
                } catch (failure: Throwable) {
                    if (previousSources != null) {
                        runCatching { discoverySourceStore?.replaceAll(previousSources) }
                            .onFailure(failure::addSuppressed)
                    }
                    throw failure
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
            .put("opportunities", jsonArray(snapshot.opportunities, ::encodeOpportunity))
            .put("workflowRuns", jsonArray(snapshot.workflowRuns, ::encodeWorkflowRun))
            .put("workflowSteps", jsonArray(snapshot.workflowSteps, ::encodeWorkflowStep))
            .put("workflowEvents", jsonArray(snapshot.workflowEvents, ::encodeWorkflowEvent))
            .put("discoverySources", jsonArray(snapshot.discoverySources, ::encodeDiscoverySource))
            .put("proofAssets", jsonArray(snapshot.proofAssets, ::encodeProofAsset))
            .put("discoveryStrategies", jsonArray(snapshot.discoveryStrategies, ::encodeDiscoveryStrategy))
        return root.toString()
    }

    internal fun decodeSnapshot(text: String): BackupSnapshot {
        val root = JSONObject(text)
        val snapshotVersion = root.getInt("formatVersion")
        require(snapshotVersion in MIN_SNAPSHOT_VERSION..SNAPSHOT_VERSION) { "This backup format is not supported." }
        val backupSchemaVersion = root.getInt("databaseSchemaVersion")
        require(backupSchemaVersion in MIN_SUPPORTED_DATABASE_SCHEMA_VERSION..DATABASE_SCHEMA_VERSION) {
            "This backup was created for an unsupported Trader database schema."
        }
        val opportunities = if (backupSchemaVersion >= 4) {
            require(root.has("opportunities")) { "Backup is missing its opportunity records." }
            decodeArray(root, "opportunities", ::decodeOpportunity)
        } else {
            emptyList()
        }
        return BackupSnapshot(
            problems = decodeArray(root, "problems", ::decodeProblem),
            planSteps = decodeArray(root, "planSteps", ::decodePlanStep),
            activityEvents = decodeArray(root, "activityEvents", ::decodeActivity),
            evidence = decodeArray(root, "evidence", ::decodeEvidence),
            opportunities = opportunities,
            workflowRuns = decodeArray(root, "workflowRuns", ::decodeWorkflowRun),
            workflowSteps = decodeArray(root, "workflowSteps", ::decodeWorkflowStep),
            workflowEvents = decodeArray(root, "workflowEvents", ::decodeWorkflowEvent),
            discoverySources = if (snapshotVersion >= 2) decodeArray(root, "discoverySources", ::decodeDiscoverySource) else emptyList(),
            proofAssets = if (backupSchemaVersion >= 5) decodeArray(root, "proofAssets", ::decodeProofAsset) else emptyList(),
            discoveryStrategies = if (backupSchemaVersion >= 5) decodeArray(root, "discoveryStrategies", ::decodeDiscoveryStrategy) else emptyList()
        )
    }

    private fun validateSnapshot(snapshot: BackupSnapshot) {
        val rowCount = snapshot.problems.size.toLong() + snapshot.planSteps.size +
            snapshot.activityEvents.size + snapshot.evidence.size + snapshot.opportunities.size + snapshot.workflowRuns.size +
            snapshot.workflowSteps.size + snapshot.workflowEvents.size + snapshot.discoverySources.size + snapshot.proofAssets.size + snapshot.discoveryStrategies.size
        require(rowCount <= MAX_ROWS) { "Backup contains more records than this version supports." }

        fun unique(values: List<String>, label: String) {
            require(values.size == values.toSet().size) { "Backup contains duplicate $label identifiers." }
        }
        unique(snapshot.problems.map { it.id }, "case")
        unique(snapshot.planSteps.map { it.id }, "plan-step")
        unique(snapshot.activityEvents.map { it.id }, "activity-event")
        unique(snapshot.evidence.map { it.id }, "evidence")
        unique(snapshot.opportunities.map { it.id }, "opportunity")
        unique(snapshot.opportunities.map { it.problemId }, "opportunity problem")
        unique(snapshot.workflowRuns.map { it.id }, "workflow-run")
        unique(snapshot.workflowSteps.map { it.id }, "workflow-step")
        unique(snapshot.workflowEvents.map { it.id }, "workflow-event")
        unique(snapshot.workflowRuns.map { it.idempotencyKey }, "workflow idempotency")
        unique(snapshot.discoverySources.map { it.id }, "discovery source")
        unique(snapshot.proofAssets.map { it.id }, "proof asset")
        unique(snapshot.discoveryStrategies.map { it.id }, "discovery strategy")
        unique(snapshot.discoverySources.map { it.feedUrl.lowercase() }, "discovery feed URL")
        require(snapshot.discoverySources.all {
            it.adapterId == RssAtomFeedAdapter.ADAPTER_ID && it.name.isNotBlank() && it.name.length <= 100 &&
                it.id.isNotBlank() && it.id.length <= 128 && (it.lastCheckedAt == null || it.lastCheckedAt >= 0)
        }) { "Backup contains an invalid discovery source." }
        snapshot.discoverySources.forEach { RssAtomFeedAdapter.validateFeedUrl(it.feedUrl) }

        val proofAssets = snapshot.proofAssets.map { DiscoveryStrategyCodec.decodeAsset(it.payloadJson) }
        val strategies = snapshot.discoveryStrategies.map { DiscoveryStrategyCodec.decodeStrategy(it.payloadJson) }
        require(snapshot.proofAssets.all { row -> row.id.isNotBlank() && row.schemaVersion == 1 && row.kind.isNotBlank() && row.payloadJson.length <= 64_000 }) { "Backup contains an invalid proof asset." }
        require(snapshot.discoveryStrategies.all { row -> row.id.isNotBlank() && row.schemaVersion == 1 && row.payloadJson.length <= 128_000 }) { "Backup contains an invalid discovery strategy." }
        require(proofAssets.all { DiscoveryStrategyRules.validate(it).isEmpty() }) { "Backup contains an invalid proof asset payload." }
        require(snapshot.proofAssets.all { row ->
            val asset = proofAssets.firstOrNull { it.id == row.id }
            asset != null && asset.schemaVersion == row.schemaVersion && asset.kind.name == row.kind &&
                asset.createdAt == row.createdAt && asset.updatedAt == row.updatedAt
        }) { "Proof asset metadata does not match its payload." }
        require(strategies.all { strategy -> DiscoveryStrategyRules.validate(strategy, proofAssets).isEmpty() }) { "Backup contains an invalid discovery strategy payload." }
        require(snapshot.discoveryStrategies.all { row ->
            val strategy = strategies.firstOrNull { it.id == row.id }
            strategy != null && row.proofAssetId in snapshot.proofAssets.map { it.id } &&
                strategy.proofAssetId == row.proofAssetId && strategy.schemaVersion == row.schemaVersion &&
                strategy.createdAt == row.createdAt && strategy.updatedAt == row.updatedAt
        }) { "A discovery strategy is invalid or points to a missing proof asset." }
        val evidenceIds = snapshot.evidence.map { it.id }.toSet()
        require(proofAssets.flatMap { it.claims }.flatMap { it.evidenceIds }.all { it in evidenceIds }) {
            "A proof claim points to a missing evidence record."
        }

        val problems = snapshot.problems.map { it.id }.toSet()
        val runs = snapshot.workflowRuns.associateBy { it.id }
        val steps = snapshot.workflowSteps.associateBy { it.id }
        require(snapshot.planSteps.all { it.problemId in problems }) { "A plan step points to a missing case." }
        require(snapshot.activityEvents.all { it.problemId in problems }) { "An activity event points to a missing case." }
        require(snapshot.evidence.all { it.problemId in problems }) { "An evidence record points to a missing case." }
        require(snapshot.opportunities.all { it.problemId in problems }) { "An opportunity points to a missing case." }
        require(snapshot.opportunities.all {
            OpportunityRules.isValidLane(it.lane) && OpportunityRules.isValidStage(it.stage) && it.opportunityName.isNotBlank()
        }) { "An opportunity contains an unsupported lane, stage or missing name." }
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

    private fun encodeOpportunity(row: OpportunityEntity) = json(
        "id" to row.id, "problemId" to row.problemId, "lane" to row.lane,
        "organizationName" to row.organizationName, "opportunityName" to row.opportunityName,
        "contactName" to row.contactName, "contactRoute" to row.contactRoute, "fitNotes" to row.fitNotes,
        "stage" to row.stage, "nextAction" to row.nextAction, "followUpAt" to row.followUpAt,
        "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )

    private fun decodeOpportunity(o: JSONObject) = OpportunityEntity(
        id = o.getString("id"), problemId = o.getString("problemId"), lane = o.getString("lane"),
        organizationName = o.getString("organizationName"), opportunityName = o.getString("opportunityName"),
        contactName = o.getString("contactName"), contactRoute = o.getString("contactRoute"),
        fitNotes = o.getString("fitNotes"), stage = o.getString("stage"), nextAction = o.getString("nextAction"),
        followUpAt = o.longOrNull("followUpAt"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
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

    private fun encodeProofAsset(row: ProofAssetEntity) = json(
        "id" to row.id, "schemaVersion" to row.schemaVersion, "kind" to row.kind,
        "payloadJson" to row.payloadJson, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )

    private fun decodeProofAsset(o: JSONObject) = ProofAssetEntity(
        id = o.getString("id"), schemaVersion = o.getInt("schemaVersion"), kind = o.getString("kind"),
        payloadJson = o.getString("payloadJson"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun encodeDiscoveryStrategy(row: DiscoveryStrategyEntity) = json(
        "id" to row.id, "proofAssetId" to row.proofAssetId, "schemaVersion" to row.schemaVersion,
        "payloadJson" to row.payloadJson, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )

    private fun decodeDiscoveryStrategy(o: JSONObject) = DiscoveryStrategyEntity(
        id = o.getString("id"), proofAssetId = o.getString("proofAssetId"), schemaVersion = o.getInt("schemaVersion"),
        payloadJson = o.getString("payloadJson"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun encodeDiscoverySource(row: DiscoverySourceConfig) = json(
        "id" to row.id, "adapterId" to row.adapterId, "name" to row.name, "feedUrl" to row.feedUrl,
        "enabled" to row.enabled, "lastCheckedAt" to row.lastCheckedAt, "lastError" to row.lastError
    )

    private fun decodeDiscoverySource(o: JSONObject) = DiscoverySourceConfig(
        id = o.getString("id"),
        adapterId = o.getString("adapterId"),
        name = o.getString("name"),
        feedUrl = o.getString("feedUrl"),
        enabled = o.optBoolean("enabled", true),
        lastCheckedAt = o.longOrNull("lastCheckedAt"),
        lastError = o.stringOrNull("lastError")
    )

    private fun json(vararg values: Pair<String, Any?>) = JSONObject().apply {
        values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }

    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    internal data class BackupSnapshot(
        val problems: List<ProblemEntity>,
        val planSteps: List<PlanStepEntity>,
        val activityEvents: List<ActivityEventEntity>,
        val evidence: List<EvidenceEntity>,
        val opportunities: List<OpportunityEntity>,
        val workflowRuns: List<WorkflowRunEntity>,
        val workflowSteps: List<WorkflowStepEntity>,
        val workflowEvents: List<WorkflowEventEntity>,
        val discoverySources: List<DiscoverySourceConfig> = emptyList(),
        val proofAssets: List<ProofAssetEntity> = emptyList(),
        val discoveryStrategies: List<DiscoveryStrategyEntity> = emptyList()
    )

    companion object {
        private const val MIN_SNAPSHOT_VERSION = 1
        private const val SNAPSHOT_VERSION = 3
        private const val DATABASE_SCHEMA_VERSION = 5
        private const val MIN_SUPPORTED_DATABASE_SCHEMA_VERSION = 3
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
