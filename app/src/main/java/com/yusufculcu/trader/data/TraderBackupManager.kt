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
                    discoveryStrategies = database.discoveryStrategyDao().getAll(),
                    discoveryRuns = database.discoveryRunDao().getAll(),
                    sourceRuns = database.sourceRunDao().getAll(),
                    discoveryDocuments = database.discoveryDocumentDao().getAll(),
                    organizations = database.organizationDao().getAll(),
                    organizationSources = database.organizationSourceDao().getAll(),
                    organizationClaims = database.organizationClaimDao().getAll(),
                    organizationPeople = database.organizationPersonDao().getAll(),
                    buyerRequirements = database.buyerRequirementDao().getAll(),
                    buyerRequirementItems = database.buyerRequirementLineItemDao().getAll(),
                    buyerRequirementEvidence = database.buyerRequirementEvidenceDao().getAll(),
                    supplierOffers = database.supplierOfferDao().getAll(),
                    supplierOfferItems = database.supplierOfferLineItemDao().getAll(),
                    supplierOfferEvidence = database.supplierOfferEvidenceDao().getAll(),
                    sourcingReviews = database.sourcingReviewDao().getAll(),
                    businessActivityEvents = database.businessActivityDao().getAll(),
                    products = database.productDao().getAll(),
                    productEvidence = database.productEvidenceDao().getAll()
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
                        database.sourcingReviewDao().deleteAll()
                        database.businessActivityDao().deleteAll()
                        database.productEvidenceDao().deleteAll()
                        database.productDao().deleteAll()
                        database.supplierOfferEvidenceDao().deleteAll()
                        database.supplierOfferLineItemDao().deleteAll()
                        database.supplierOfferDao().deleteAll()
                        database.buyerRequirementEvidenceDao().deleteAll()
                        database.buyerRequirementLineItemDao().deleteAll()
                        database.buyerRequirementDao().deleteAll()
                        database.organizationPersonDao().deleteAll()
                        database.organizationClaimDao().deleteAll()
                        database.organizationSourceDao().deleteAll()
                        database.organizationDao().deleteAll()
                        database.discoveryDocumentDao().deleteAll()
                        database.sourceRunDao().deleteAll()
                        database.discoveryRunDao().deleteAll()
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
                        database.discoveryRunDao().insertAll(snapshot.discoveryRuns)
                        database.sourceRunDao().insertAll(snapshot.sourceRuns)
                        database.discoveryDocumentDao().insertAll(snapshot.discoveryDocuments)
                        database.organizationDao().insertAll(snapshot.organizations)
                        database.organizationSourceDao().insertAll(snapshot.organizationSources)
                        database.productDao().insertAll(snapshot.products)
                        database.productEvidenceDao().insertAll(snapshot.productEvidence)
                        database.organizationClaimDao().insertAll(snapshot.organizationClaims)
                        database.organizationPersonDao().insertAll(snapshot.organizationPeople)
                        database.buyerRequirementDao().insertAll(snapshot.buyerRequirements)
                        database.buyerRequirementLineItemDao().insertAll(snapshot.buyerRequirementItems)
                        database.buyerRequirementEvidenceDao().insertAll(snapshot.buyerRequirementEvidence)
                        database.supplierOfferDao().insertAll(snapshot.supplierOffers)
                        database.supplierOfferLineItemDao().insertAll(snapshot.supplierOfferItems)
                        database.supplierOfferEvidenceDao().insertAll(snapshot.supplierOfferEvidence)
                        database.sourcingReviewDao().insertAll(snapshot.sourcingReviews)
                        database.businessActivityDao().insertAll(snapshot.businessActivityEvents)
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
            .put("discoveryRuns", jsonArray(snapshot.discoveryRuns, ::encodeDiscoveryRun))
            .put("sourceRuns", jsonArray(snapshot.sourceRuns, ::encodeSourceRun))
            .put("discoveryDocuments", jsonArray(snapshot.discoveryDocuments, ::encodeDiscoveryDocument))
            .put("organizations", jsonArray(snapshot.organizations, ::encodeOrganization))
            .put("organizationSources", jsonArray(snapshot.organizationSources, ::encodeOrganizationSource))
            .put("organizationClaims", jsonArray(snapshot.organizationClaims, ::encodeOrganizationClaim))
            .put("organizationPeople", jsonArray(snapshot.organizationPeople, ::encodeOrganizationPerson))
            .put("buyerRequirements", jsonArray(snapshot.buyerRequirements, ::encodeBuyerRequirement))
            .put("buyerRequirementItems", jsonArray(snapshot.buyerRequirementItems, ::encodeBuyerRequirementItem))
            .put("buyerRequirementEvidence", jsonArray(snapshot.buyerRequirementEvidence, ::encodeBuyerRequirementEvidence))
            .put("supplierOffers", jsonArray(snapshot.supplierOffers, ::encodeSupplierOffer))
            .put("supplierOfferItems", jsonArray(snapshot.supplierOfferItems, ::encodeSupplierOfferItem))
            .put("supplierOfferEvidence", jsonArray(snapshot.supplierOfferEvidence, ::encodeSupplierOfferEvidence))
            .put("sourcingReviews", jsonArray(snapshot.sourcingReviews, ::encodeSourcingReview))
            .put("businessActivityEvents", jsonArray(snapshot.businessActivityEvents, ::encodeBusinessActivity))
            .put("products", jsonArray(snapshot.products, ::encodeProduct))
            .put("productEvidence", jsonArray(snapshot.productEvidence, ::encodeProductEvidence))
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
            discoveryStrategies = if (backupSchemaVersion >= 5) decodeArray(root, "discoveryStrategies", ::decodeDiscoveryStrategy) else emptyList(),
            discoveryRuns = if (backupSchemaVersion >= 6 && root.has("discoveryRuns")) decodeArray(root, "discoveryRuns", ::decodeDiscoveryRun) else emptyList(),
            sourceRuns = if (backupSchemaVersion >= 6 && root.has("sourceRuns")) decodeArray(root, "sourceRuns", ::decodeSourceRun) else emptyList(),
            discoveryDocuments = if (backupSchemaVersion >= 7 && root.has("discoveryDocuments")) decodeArray(root, "discoveryDocuments", ::decodeDiscoveryDocument) else emptyList(),
            organizations = if (backupSchemaVersion >= 8 && root.has("organizations")) decodeArray(root, "organizations", ::decodeOrganization) else emptyList(),
            organizationSources = if (backupSchemaVersion >= 8 && root.has("organizationSources")) decodeArray(root, "organizationSources", ::decodeOrganizationSource) else emptyList(),
            organizationClaims = if (backupSchemaVersion >= 8 && root.has("organizationClaims")) decodeArray(root, "organizationClaims", ::decodeOrganizationClaim) else emptyList(),
            organizationPeople = if (backupSchemaVersion >= 9) { require(root.has("organizationPeople")) { "Backup is missing its company contact records." }; decodeArray(root, "organizationPeople", ::decodeOrganizationPerson) } else emptyList(),
            buyerRequirements = if (backupSchemaVersion >= 10) { require(root.has("buyerRequirements")) { "Backup is missing buyer requirements." }; decodeArray(root, "buyerRequirements", ::decodeBuyerRequirement) } else emptyList(),
            buyerRequirementItems = if (backupSchemaVersion >= 10) { require(root.has("buyerRequirementItems")) { "Backup is missing buyer requirement line items." }; decodeArray(root, "buyerRequirementItems", ::decodeBuyerRequirementItem) } else emptyList(),
            buyerRequirementEvidence = if (backupSchemaVersion >= 10) { require(root.has("buyerRequirementEvidence")) { "Backup is missing buyer requirement evidence links." }; decodeArray(root, "buyerRequirementEvidence", ::decodeBuyerRequirementEvidence) } else emptyList(),
            supplierOffers = if (backupSchemaVersion >= 11) { require(root.has("supplierOffers")) { "Backup is missing supplier offers." }; decodeArray(root, "supplierOffers", ::decodeSupplierOffer) } else emptyList(),
            supplierOfferItems = if (backupSchemaVersion >= 11) { require(root.has("supplierOfferItems")) { "Backup is missing supplier offer line items." }; decodeArray(root, "supplierOfferItems", ::decodeSupplierOfferItem) } else emptyList(),
            supplierOfferEvidence = if (backupSchemaVersion >= 11) { require(root.has("supplierOfferEvidence")) { "Backup is missing supplier offer evidence links." }; decodeArray(root, "supplierOfferEvidence", ::decodeSupplierOfferEvidence) } else emptyList(),
            sourcingReviews = if (backupSchemaVersion >= 11) { require(root.has("sourcingReviews")) { "Backup is missing sourcing review decisions." }; decodeArray(root, "sourcingReviews", ::decodeSourcingReview) } else emptyList(),
            businessActivityEvents = if (backupSchemaVersion >= 12) { require(root.has("businessActivityEvents")) { "Backup is missing business activity history." }; decodeArray(root, "businessActivityEvents", ::decodeBusinessActivity) } else emptyList(),
            products = if (backupSchemaVersion >= 14) { require(root.has("products")) { "Backup is missing product catalogue records." }; decodeArray(root, "products", ::decodeProduct) } else emptyList(),
            productEvidence = if (backupSchemaVersion >= 14) { require(root.has("productEvidence")) { "Backup is missing product evidence links." }; decodeArray(root, "productEvidence", ::decodeProductEvidence) } else emptyList()
        )
    }

    private fun validateSnapshot(snapshot: BackupSnapshot) {
        val rowCount = snapshot.problems.size.toLong() + snapshot.planSteps.size +
            snapshot.activityEvents.size + snapshot.evidence.size + snapshot.opportunities.size + snapshot.workflowRuns.size +
            snapshot.workflowSteps.size + snapshot.workflowEvents.size + snapshot.discoverySources.size + snapshot.proofAssets.size + snapshot.discoveryStrategies.size + snapshot.discoveryRuns.size + snapshot.sourceRuns.size + snapshot.discoveryDocuments.size + snapshot.organizations.size + snapshot.organizationSources.size + snapshot.organizationClaims.size + snapshot.organizationPeople.size + snapshot.buyerRequirements.size + snapshot.buyerRequirementItems.size + snapshot.buyerRequirementEvidence.size + snapshot.supplierOffers.size + snapshot.supplierOfferItems.size + snapshot.supplierOfferEvidence.size + snapshot.sourcingReviews.size + snapshot.businessActivityEvents.size + snapshot.products.size + snapshot.productEvidence.size
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
        unique(snapshot.discoveryRuns.map { it.id }, "discovery run")
        unique(snapshot.sourceRuns.map { it.id }, "source run")
        unique(snapshot.discoveryDocuments.map { it.id }, "discovery document")
        unique(snapshot.organizations.map { it.id }, "organization")
        unique(snapshot.organizationSources.map { it.id }, "organization source")
        unique(snapshot.organizationSources.map { it.organizationId + "|" + it.url }, "organization source URL")
        unique(snapshot.organizationClaims.map { it.id }, "organization claim")
        unique(snapshot.organizationPeople.map { it.id }, "organization person")
        unique(snapshot.buyerRequirements.map { it.id }, "buyer requirement")
        unique(snapshot.buyerRequirementItems.map { it.id }, "buyer requirement line item")
        unique(snapshot.buyerRequirementEvidence.map { it.id }, "buyer requirement evidence link")
        unique(snapshot.buyerRequirementEvidence.map { it.requirementId + "|" + it.sourceId }, "buyer requirement evidence pair")
        unique(snapshot.supplierOffers.map { it.id }, "supplier offer")
        unique(snapshot.supplierOfferItems.map { it.id }, "supplier offer line item")
        unique(snapshot.supplierOfferEvidence.map { it.id }, "supplier offer evidence link")
        unique(snapshot.supplierOfferEvidence.map { it.offerId + "|" + it.sourceId }, "supplier offer evidence pair")
        unique(snapshot.sourcingReviews.map { it.id }, "sourcing review")
        unique(snapshot.businessActivityEvents.map { it.id }, "business activity event")
        unique(snapshot.products.map { it.id }, "product")
        unique(snapshot.products.mapNotNull { row -> row.skuKey?.let { row.organizationId + "|" + it } }, "company product SKU")
        unique(snapshot.productEvidence.map { it.id }, "product evidence link")
        unique(snapshot.productEvidence.map { it.productId + "|" + it.sourceId }, "product evidence pair")
        unique(snapshot.sourcingReviews.map { it.buyerRequirementId + "|" + it.supplierOfferId }, "sourcing review pair")
        require(snapshot.supplierOffers.all { it.organizationId in snapshot.organizations.map(OrganizationEntity::id) }) { "Supplier offer references a missing company." }
        require(snapshot.supplierOfferItems.all { it.offerId in snapshot.supplierOffers.map(SupplierOfferEntity::id) }) { "Supplier offer item references a missing offer." }
        require(snapshot.supplierOfferEvidence.all { link -> link.offerId in snapshot.supplierOffers.map(SupplierOfferEntity::id) && link.sourceId in snapshot.organizationSources.map(OrganizationSourceEntity::id) && snapshot.supplierOffers.first { it.id == link.offerId }.organizationId == snapshot.organizationSources.first { it.id == link.sourceId }.organizationId }) { "Supplier offer evidence link is invalid." }
        require(snapshot.sourcingReviews.all { it.buyerRequirementId in snapshot.buyerRequirements.map(BuyerRequirementEntity::id) && it.supplierOfferId in snapshot.supplierOffers.map(SupplierOfferEntity::id) && it.status in SourcingReviewStatus.ALL }) { "Sourcing review decision is invalid." }
        require(snapshot.businessActivityEvents.all { event ->
            event.id.isNotBlank() && event.organizationId.isNotBlank() && event.organizationId.length <= 128 &&
                event.organizationName.isNotBlank() && event.organizationName.length <= 200 &&
                event.entityType in BusinessActivityEntityType.ALL && event.entityId.isNotBlank() && event.entityId.length <= 128 &&
                event.entityLabel.isNotBlank() && event.entityLabel.length <= 300 && event.eventType in BusinessActivityAction.ALL &&
                (event.fromStatus == null || event.fromStatus.length <= 80) && (event.toStatus == null || event.toStatus.length <= 80) &&
                event.summary.isNotBlank() && event.summary.length <= 200 && event.details.length <= 2_000 && event.occurredAt >= 0
        }) { "Backup contains invalid business activity history." }
        require(snapshot.buyerRequirements.all { it.organizationId in snapshot.organizations.map(OrganizationEntity::id) }) { "Buyer requirement references a missing company." }
        require(snapshot.buyerRequirementItems.all { it.requirementId in snapshot.buyerRequirements.map(BuyerRequirementEntity::id) }) { "Buyer requirement line item references a missing requirement." }
        require(snapshot.buyerRequirementEvidence.all { link -> link.requirementId in snapshot.buyerRequirements.map(BuyerRequirementEntity::id) && link.sourceId in snapshot.organizationSources.map(OrganizationSourceEntity::id) && snapshot.buyerRequirements.first { it.id == link.requirementId }.organizationId == snapshot.organizationSources.first { it.id == link.sourceId }.organizationId }) { "Buyer requirement evidence link is invalid." }
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
        val strategyIds = snapshot.discoveryStrategies.map { it.id }.toSet()
        require(snapshot.discoveryRuns.all { it.id.isNotBlank() && it.objective.isNotBlank() && it.objective.length <= 2000 && (it.strategyId == null || it.strategyId in strategyIds) && it.status in setOf(DiscoveryRunStatus.CREATED, DiscoveryRunStatus.RUNNING, DiscoveryRunStatus.COMPLETED, DiscoveryRunStatus.PARTIAL, DiscoveryRunStatus.FAILED, DiscoveryRunStatus.CANCELLED) }) { "Backup contains an invalid discovery run." }
        val discoveryRuns = snapshot.discoveryRuns.associateBy { it.id }
        require(snapshot.sourceRuns.all { it.id.isNotBlank() && it.discoveryRunId in discoveryRuns && it.sourceId.isNotBlank() && it.adapterId.isNotBlank() && it.resultCount >= 0 && it.status in setOf(SourceRunStatus.PENDING, SourceRunStatus.RUNNING, SourceRunStatus.SUCCEEDED, SourceRunStatus.FAILED, SourceRunStatus.SKIPPED, SourceRunStatus.CANCELLED) && (it.errorCode == null || it.errorCode.length <= 80) && (it.errorMessage == null || it.errorMessage.length <= 2000) }) { "Backup contains an invalid source run." }
        require(snapshot.discoveryDocuments.all { it.id.isNotBlank() && it.discoveryRunId in discoveryRuns && it.sourceId.isNotBlank() && it.sourceId.length <= 128 && it.sourceName.isNotBlank() && it.sourceName.length <= 200 && it.nativeId.length <= 4096 && it.title.length <= 300 && it.url.startsWith("https://") && it.url.length <= 2048 && it.summary.length <= 4000 && it.retrievedAt >= 0 && (it.publishedAt == null || it.publishedAt >= 0) }) { "Backup contains an invalid discovery document." }
        val organizationIds = snapshot.organizations.map { it.id }.toSet()
        val organizationSources = snapshot.organizationSources.associateBy { it.id }
        require(snapshot.organizationPeople.all { person ->
            person.id.isNotBlank() && person.organizationId in organizationIds && person.fullName.isNotBlank() && person.fullName.length <= 160 &&
                (person.role == null || person.role.length <= 160) && (person.email == null || (person.email.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(person.email))) &&
                (person.phone == null || person.phone.length <= 64) && (person.profileUrl == null || (person.profileUrl.startsWith("https://") && person.profileUrl.length <= 2048 && runCatching { java.net.URI(person.profileUrl).host?.let { it.equals("linkedin.com", true) || it.endsWith(".linkedin.com", true) } == true }.getOrDefault(false))) &&
                (person.notes == null || person.notes.length <= 2000) && person.createdAt >= 0 && person.updatedAt >= person.createdAt
        }) { "Backup contains an invalid organization contact or missing company relationship." }
        require(snapshot.organizations.all {
            it.id.isNotBlank() && it.canonicalName.isNotBlank() && it.canonicalName.length <= 200 &&
                (it.websiteUrl == null || (it.websiteUrl.startsWith("https://") && it.websiteUrl.length <= 2_048)) &&
                (it.domain == null || (it.domain.isNotBlank() && it.domain.length <= 253)) &&
                (it.country == null || it.country.length <= 120) && (it.city == null || it.city.length <= 120) &&
                (it.industry == null || it.industry.length <= 160) && (it.notes == null || it.notes.length <= 4_000) &&
                it.createdAt >= 0 && it.updatedAt >= it.createdAt
        }) { "Backup contains an invalid organization." }
        require(snapshot.organizationSources.all {
            it.id.isNotBlank() && it.organizationId in organizationIds && it.sourceKind in OrganizationSourceKind.ALL &&
                it.title.isNotBlank() && it.title.length <= 300 && it.url.startsWith("https://") &&
                it.url.length <= 2_048 && !it.url.contains("@") && it.excerpt.length <= 8_000 && it.capturedAt >= 0
        }) { "Backup contains an invalid organization source." }
        val productsById = snapshot.products.associateBy { it.id }
        require(snapshot.products.all { row ->
            val skuValid = if (row.skuCode == null) row.skuKey == null else
                row.skuCode.isNotBlank() && row.skuCode == row.skuCode.trim() && row.skuCode.length <= 80 &&
                    row.skuKey == row.skuCode.uppercase(java.util.Locale.ROOT)
            row.id.isNotBlank() && row.organizationId in organizationIds && skuValid &&
                (row.skuKey == null || row.skuKey.length <= 80) &&
                row.name.isNotBlank() && row.name.length <= 200 &&
                (row.description == null || row.description.length <= 4_000) &&
                (row.specification == null || row.specification.length <= 2_000) &&
                (row.material == null || row.material.length <= 120) &&
                (row.unit == null || row.unit.length <= 40) &&
                row.status in ProductStatus.ALL && row.createdAt >= 0 && row.updatedAt >= row.createdAt
        }) { "Backup contains an invalid product catalogue record." }
        require(snapshot.productEvidence.all { link ->
            val product = productsById[link.productId]
            val source = organizationSources[link.sourceId]
            link.id.isNotBlank() && product != null && source != null &&
                product.organizationId == source.organizationId && link.linkedAt >= 0
        }) { "Backup contains a product evidence link with a missing or cross-company relationship." }
        require(snapshot.organizationClaims.all {
            val source = organizationSources[it.sourceId]
            it.id.isNotBlank() && it.organizationId in organizationIds && source != null &&
                source.organizationId == it.organizationId && it.field.isNotBlank() && it.field.length <= 80 &&
                it.statement.isNotBlank() && it.statement.length <= 2_000 && it.reviewState in OrganizationClaimState.ALL &&
                (it.reviewNote == null || it.reviewNote.length <= 1_000) &&
                ((it.reviewState == OrganizationClaimState.UNREVIEWED && it.reviewedAt == null) ||
                    (it.reviewState != OrganizationClaimState.UNREVIEWED && it.reviewedAt != null && it.reviewedAt >= 0)) &&
                it.createdAt >= 0 && it.updatedAt >= it.createdAt
        }) { "Backup contains an invalid organization claim or missing source relationship." }
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

    private fun encodeDiscoveryRun(row: DiscoveryRunEntity) = json(
        "id" to row.id, "strategyId" to row.strategyId, "objective" to row.objective, "status" to row.status,
        "createdAt" to row.createdAt, "startedAt" to row.startedAt, "finishedAt" to row.finishedAt,
        "cancellationRequestedAt" to row.cancellationRequestedAt, "lastError" to row.lastError
    )
    private fun decodeDiscoveryRun(o: JSONObject) = DiscoveryRunEntity(
        id = o.getString("id"), strategyId = o.stringOrNull("strategyId"), objective = o.getString("objective"), status = o.getString("status"),
        createdAt = o.getLong("createdAt"), startedAt = o.longOrNull("startedAt"), finishedAt = o.longOrNull("finishedAt"),
        cancellationRequestedAt = o.longOrNull("cancellationRequestedAt"), lastError = o.stringOrNull("lastError")
    )
    private fun encodeDiscoveryDocument(row: DiscoveryDocumentEntity) = json("id" to row.id, "discoveryRunId" to row.discoveryRunId, "sourceId" to row.sourceId, "sourceName" to row.sourceName, "nativeId" to row.nativeId, "title" to row.title, "url" to row.url, "summary" to row.summary, "publishedAt" to row.publishedAt, "retrievedAt" to row.retrievedAt)
    private fun decodeDiscoveryDocument(o: JSONObject) = DiscoveryDocumentEntity(o.getString("id"), o.getString("discoveryRunId"), o.getString("sourceId"), o.getString("sourceName"), o.getString("nativeId"), o.getString("title"), o.getString("url"), o.getString("summary"), o.longOrNull("publishedAt"), o.getLong("retrievedAt"))

    private fun encodeOrganization(row: OrganizationEntity) = json(
        "id" to row.id, "canonicalName" to row.canonicalName, "websiteUrl" to row.websiteUrl,
        "domain" to row.domain, "country" to row.country, "city" to row.city, "industry" to row.industry,
        "notes" to row.notes, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )
    private fun decodeOrganization(o: JSONObject) = OrganizationEntity(
        id = o.getString("id"), canonicalName = o.getString("canonicalName"),
        websiteUrl = o.stringOrNull("websiteUrl"), domain = o.stringOrNull("domain"),
        country = o.stringOrNull("country"), city = o.stringOrNull("city"),
        industry = o.stringOrNull("industry"), notes = o.stringOrNull("notes"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )
    private fun encodeOrganizationSource(row: OrganizationSourceEntity) = json(
        "id" to row.id, "organizationId" to row.organizationId, "sourceKind" to row.sourceKind,
        "title" to row.title, "url" to row.url, "excerpt" to row.excerpt, "capturedAt" to row.capturedAt
    )
    private fun decodeOrganizationSource(o: JSONObject) = OrganizationSourceEntity(
        id = o.getString("id"), organizationId = o.getString("organizationId"),
        sourceKind = o.getString("sourceKind"), title = o.getString("title"),
        url = o.getString("url"), excerpt = o.getString("excerpt"), capturedAt = o.getLong("capturedAt")
    )
    private fun encodeOrganizationClaim(row: OrganizationClaimEntity) = json(
        "id" to row.id, "organizationId" to row.organizationId, "sourceId" to row.sourceId,
        "field" to row.field, "statement" to row.statement, "reviewState" to row.reviewState,
        "reviewNote" to row.reviewNote, "reviewedAt" to row.reviewedAt,
        "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )
    private fun decodeOrganizationClaim(o: JSONObject) = OrganizationClaimEntity(
        id = o.getString("id"), organizationId = o.getString("organizationId"),
        sourceId = o.getString("sourceId"), field = o.getString("field"),
        statement = o.getString("statement"), reviewState = o.getString("reviewState"),
        reviewNote = o.stringOrNull("reviewNote"), reviewedAt = o.longOrNull("reviewedAt"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun encodeSupplierOffer(row: SupplierOfferEntity) = json("id" to row.id, "organizationId" to row.organizationId, "title" to row.title, "description" to row.description, "status" to row.status, "currency" to row.currency, "originCountry" to row.originCountry, "originCity" to row.originCity, "notes" to row.notes, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt, "deletedAt" to row.deletedAt)
    private fun decodeSupplierOffer(o: JSONObject) = SupplierOfferEntity(o.getString("id"), o.getString("organizationId"), o.getString("title"), o.stringOrNull("description"), o.getString("status"), o.stringOrNull("currency"), o.stringOrNull("originCountry"), o.stringOrNull("originCity"), o.stringOrNull("notes"), o.getLong("createdAt"), o.getLong("updatedAt"), o.longOrNull("deletedAt"))
    private fun encodeSupplierOfferItem(row: SupplierOfferLineItemEntity) = json("id" to row.id, "offerId" to row.offerId, "productName" to row.productName, "specification" to row.specification, "material" to row.material, "quantityAvailable" to row.quantityAvailable, "quantityUnit" to row.quantityUnit, "minimumOrderQuantity" to row.minimumOrderQuantity, "unitPriceMinorUnits" to row.unitPriceMinorUnits, "currency" to row.currency, "leadTimeDays" to row.leadTimeDays, "createdAt" to row.createdAt)
    private fun decodeSupplierOfferItem(o: JSONObject) = SupplierOfferLineItemEntity(o.getString("id"), o.getString("offerId"), o.getString("productName"), o.stringOrNull("specification"), o.stringOrNull("material"), o.stringOrNull("quantityAvailable"), o.stringOrNull("quantityUnit"), o.stringOrNull("minimumOrderQuantity"), o.longOrNull("unitPriceMinorUnits"), o.stringOrNull("currency"), o.optInt("leadTimeDays").takeIf { !o.isNull("leadTimeDays") }, o.getLong("createdAt"))
    private fun encodeSupplierOfferEvidence(row: SupplierOfferEvidenceEntity) = json("id" to row.id, "offerId" to row.offerId, "sourceId" to row.sourceId, "linkedAt" to row.linkedAt)
    private fun decodeSupplierOfferEvidence(o: JSONObject) = SupplierOfferEvidenceEntity(o.getString("id"), o.getString("offerId"), o.getString("sourceId"), o.getLong("linkedAt"))
    private fun encodeSourcingReview(row: SourcingReviewEntity) = json("id" to row.id, "buyerRequirementId" to row.buyerRequirementId, "supplierOfferId" to row.supplierOfferId, "status" to row.status, "rationale" to row.rationale, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt)
    private fun decodeSourcingReview(o: JSONObject) = SourcingReviewEntity(o.getString("id"), o.getString("buyerRequirementId"), o.getString("supplierOfferId"), o.getString("status"), o.stringOrNull("rationale"), o.getLong("createdAt"), o.getLong("updatedAt"))

    private fun encodeProduct(row: ProductEntity) = json(
        "id" to row.id, "organizationId" to row.organizationId, "skuCode" to row.skuCode,
        "skuKey" to row.skuKey, "name" to row.name, "description" to row.description,
        "specification" to row.specification, "material" to row.material, "unit" to row.unit,
        "status" to row.status, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt
    )
    private fun decodeProduct(o: JSONObject) = ProductEntity(
        id = o.getString("id"), organizationId = o.getString("organizationId"),
        skuCode = o.stringOrNull("skuCode"), skuKey = o.stringOrNull("skuKey"),
        name = o.getString("name"), description = o.stringOrNull("description"),
        specification = o.stringOrNull("specification"), material = o.stringOrNull("material"),
        unit = o.stringOrNull("unit"), status = o.getString("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )
    private fun encodeProductEvidence(row: ProductEvidenceEntity) = json(
        "id" to row.id, "productId" to row.productId, "sourceId" to row.sourceId, "linkedAt" to row.linkedAt
    )
    private fun decodeProductEvidence(o: JSONObject) = ProductEvidenceEntity(
        id = o.getString("id"), productId = o.getString("productId"),
        sourceId = o.getString("sourceId"), linkedAt = o.getLong("linkedAt")
    )

    private fun encodeBusinessActivity(row: BusinessActivityEntity) = json(
        "id" to row.id, "organizationId" to row.organizationId, "organizationName" to row.organizationName,
        "entityType" to row.entityType, "entityId" to row.entityId, "entityLabel" to row.entityLabel,
        "eventType" to row.eventType, "fromStatus" to row.fromStatus, "toStatus" to row.toStatus,
        "summary" to row.summary, "details" to row.details, "occurredAt" to row.occurredAt
    )

    private fun decodeBusinessActivity(o: JSONObject) = BusinessActivityEntity(
        id = o.getString("id"), organizationId = o.getString("organizationId"),
        organizationName = o.getString("organizationName"), entityType = o.getString("entityType"),
        entityId = o.getString("entityId"), entityLabel = o.getString("entityLabel"),
        eventType = o.getString("eventType"), fromStatus = o.stringOrNull("fromStatus"),
        toStatus = o.stringOrNull("toStatus"), summary = o.getString("summary"),
        details = o.getString("details"), occurredAt = o.getLong("occurredAt")
    )

    private fun encodeBuyerRequirement(row: BuyerRequirementEntity) = json("id" to row.id, "organizationId" to row.organizationId, "title" to row.title, "description" to row.description, "status" to row.status, "destinationCountry" to row.destinationCountry, "destinationCity" to row.destinationCity, "requiredByText" to row.requiredByText, "currency" to row.currency, "budgetMinorUnits" to row.budgetMinorUnits, "notes" to row.notes, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt, "deletedAt" to row.deletedAt)
    private fun decodeBuyerRequirement(o: JSONObject) = BuyerRequirementEntity(o.getString("id"),o.getString("organizationId"),o.getString("title"),o.stringOrNull("description"),o.getString("status"),o.stringOrNull("destinationCountry"),o.stringOrNull("destinationCity"),o.stringOrNull("requiredByText"),o.stringOrNull("currency"),o.longOrNull("budgetMinorUnits"),o.stringOrNull("notes"),o.getLong("createdAt"),o.getLong("updatedAt"),o.longOrNull("deletedAt"))
    private fun encodeBuyerRequirementItem(row: BuyerRequirementLineItemEntity) = json("id" to row.id, "requirementId" to row.requirementId, "productName" to row.productName, "specification" to row.specification, "quantity" to row.quantity, "unit" to row.unit, "targetUnitPriceMinorUnits" to row.targetUnitPriceMinorUnits, "createdAt" to row.createdAt)
    private fun decodeBuyerRequirementItem(o: JSONObject) = BuyerRequirementLineItemEntity(o.getString("id"),o.getString("requirementId"),o.getString("productName"),o.stringOrNull("specification"),o.stringOrNull("quantity"),o.stringOrNull("unit"),o.longOrNull("targetUnitPriceMinorUnits"),o.getLong("createdAt"))
    private fun encodeBuyerRequirementEvidence(row: BuyerRequirementEvidenceEntity) = json("id" to row.id, "requirementId" to row.requirementId, "sourceId" to row.sourceId, "linkedAt" to row.linkedAt)
    private fun decodeBuyerRequirementEvidence(o: JSONObject) = BuyerRequirementEvidenceEntity(o.getString("id"),o.getString("requirementId"),o.getString("sourceId"),o.getLong("linkedAt"))

    private fun encodeOrganizationPerson(row: OrganizationPersonEntity) = json("id" to row.id, "organizationId" to row.organizationId, "fullName" to row.fullName, "role" to row.role, "email" to row.email, "phone" to row.phone, "profileUrl" to row.profileUrl, "notes" to row.notes, "createdAt" to row.createdAt, "updatedAt" to row.updatedAt)
    private fun decodeOrganizationPerson(o: JSONObject) = OrganizationPersonEntity(o.getString("id"), o.getString("organizationId"), o.getString("fullName"), o.stringOrNull("role"), o.stringOrNull("email"), o.stringOrNull("phone"), o.stringOrNull("profileUrl"), o.stringOrNull("notes"), o.getLong("createdAt"), o.getLong("updatedAt"))

    private fun encodeSourceRun(row: SourceRunEntity) = json(
        "id" to row.id, "discoveryRunId" to row.discoveryRunId, "sourceId" to row.sourceId, "adapterId" to row.adapterId,
        "status" to row.status, "startedAt" to row.startedAt, "finishedAt" to row.finishedAt, "resultCount" to row.resultCount,
        "errorCode" to row.errorCode, "errorMessage" to row.errorMessage
    )
    private fun decodeSourceRun(o: JSONObject) = SourceRunEntity(
        id = o.getString("id"), discoveryRunId = o.getString("discoveryRunId"), sourceId = o.getString("sourceId"), adapterId = o.getString("adapterId"),
        status = o.getString("status"), startedAt = o.longOrNull("startedAt"), finishedAt = o.longOrNull("finishedAt"), resultCount = o.getInt("resultCount"),
        errorCode = o.stringOrNull("errorCode"), errorMessage = o.stringOrNull("errorMessage")
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
        val discoveryStrategies: List<DiscoveryStrategyEntity> = emptyList(),
        val discoveryRuns: List<DiscoveryRunEntity> = emptyList(),
        val sourceRuns: List<SourceRunEntity> = emptyList(),
        val discoveryDocuments: List<DiscoveryDocumentEntity> = emptyList(),
        val organizations: List<OrganizationEntity> = emptyList(),
        val organizationSources: List<OrganizationSourceEntity> = emptyList(),
        val organizationClaims: List<OrganizationClaimEntity> = emptyList(),
        val organizationPeople: List<OrganizationPersonEntity> = emptyList(),
        val buyerRequirements: List<BuyerRequirementEntity> = emptyList(),
        val buyerRequirementItems: List<BuyerRequirementLineItemEntity> = emptyList(),
        val buyerRequirementEvidence: List<BuyerRequirementEvidenceEntity> = emptyList(),
        val supplierOffers: List<SupplierOfferEntity> = emptyList(),
        val supplierOfferItems: List<SupplierOfferLineItemEntity> = emptyList(),
        val supplierOfferEvidence: List<SupplierOfferEvidenceEntity> = emptyList(),
        val sourcingReviews: List<SourcingReviewEntity> = emptyList(),
        val businessActivityEvents: List<BusinessActivityEntity> = emptyList(),
        val products: List<ProductEntity> = emptyList(),
        val productEvidence: List<ProductEvidenceEntity> = emptyList()
    )

    companion object {
        private const val MIN_SNAPSHOT_VERSION = 1
        private const val SNAPSHOT_VERSION = 11
        private const val DATABASE_SCHEMA_VERSION = 14
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
