package com.yusufculcu.trader.data

import androidx.room.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

object SupplierOfferStatus {
    const val DRAFT = "DRAFT"
    const val AVAILABLE = "AVAILABLE"
    const val ON_HOLD = "ON_HOLD"
    const val CLOSED = "CLOSED"
    val ALL = setOf(DRAFT, AVAILABLE, ON_HOLD, CLOSED)
}
object SourcingReviewStatus {
    const val SHORTLISTED = "SHORTLISTED"
    const val NEEDS_INFORMATION = "NEEDS_INFORMATION"
    const val NOT_SUITABLE = "NOT_SUITABLE"
    val ALL = setOf(SHORTLISTED, NEEDS_INFORMATION, NOT_SUITABLE)
}
@Entity(tableName = "supplier_offers", foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE)], indices = [Index("organizationId"), Index("status"), Index("updatedAt")])
data class SupplierOfferEntity(
    @PrimaryKey val id: String, val organizationId: String, val title: String, val description: String?,
    val status: String, val currency: String?, val originCountry: String?, val originCity: String?,
    val notes: String?, val createdAt: Long, val updatedAt: Long
)
@Entity(tableName = "supplier_offer_items", foreignKeys = [ForeignKey(entity = SupplierOfferEntity::class, parentColumns = ["id"], childColumns = ["offerId"], onDelete = ForeignKey.CASCADE)], indices = [Index("offerId")])
data class SupplierOfferLineItemEntity(
    @PrimaryKey val id: String, val offerId: String, val productName: String, val specification: String?,
    val material: String?, val quantityAvailable: String?, val quantityUnit: String?,
    val minimumOrderQuantity: String?, val unitPriceMinorUnits: Long?, val currency: String?,
    val leadTimeDays: Int?, val createdAt: Long
)
@Entity(tableName = "supplier_offer_evidence", foreignKeys = [ForeignKey(entity = SupplierOfferEntity::class, parentColumns = ["id"], childColumns = ["offerId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = OrganizationSourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE)], indices = [Index("offerId"), Index("sourceId"), Index(value = ["offerId", "sourceId"], unique = true)])
data class SupplierOfferEvidenceEntity(@PrimaryKey val id: String, val offerId: String, val sourceId: String, val linkedAt: Long)
@Entity(tableName = "sourcing_reviews", foreignKeys = [ForeignKey(entity = BuyerRequirementEntity::class, parentColumns = ["id"], childColumns = ["buyerRequirementId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = SupplierOfferEntity::class, parentColumns = ["id"], childColumns = ["supplierOfferId"], onDelete = ForeignKey.CASCADE)], indices = [Index("buyerRequirementId"), Index("supplierOfferId"), Index(value = ["buyerRequirementId", "supplierOfferId"], unique = true)])
data class SourcingReviewEntity(@PrimaryKey val id: String, val buyerRequirementId: String, val supplierOfferId: String, val status: String, val rationale: String?, val createdAt: Long, val updatedAt: Long)

@Dao interface SupplierOfferDao {
    @Query("SELECT * FROM supplier_offers ORDER BY updatedAt DESC") fun observeAll(): Flow<List<SupplierOfferEntity>>
    @Query("SELECT * FROM supplier_offers") suspend fun getAll(): List<SupplierOfferEntity>
    @Query("SELECT * FROM supplier_offers WHERE id=:id LIMIT 1") suspend fun getById(id: String): SupplierOfferEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(row: SupplierOfferEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(rows: List<SupplierOfferEntity>)
    @Update suspend fun update(row: SupplierOfferEntity)
    @Query("DELETE FROM supplier_offers WHERE id=:id") suspend fun deleteById(id: String): Int
    @Query("DELETE FROM supplier_offers") suspend fun deleteAll()
}
@Dao interface SupplierOfferLineItemDao {
    @Query("SELECT * FROM supplier_offer_items WHERE offerId=:offerId ORDER BY createdAt,id") fun observeForOffer(offerId: String): Flow<List<SupplierOfferLineItemEntity>>
    @Query("SELECT * FROM supplier_offer_items") suspend fun getAll(): List<SupplierOfferLineItemEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(row: SupplierOfferLineItemEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(rows: List<SupplierOfferLineItemEntity>)
    @Query("DELETE FROM supplier_offer_items") suspend fun deleteAll()
}
@Dao interface SupplierOfferEvidenceDao {
    @Query("SELECT * FROM supplier_offer_evidence WHERE offerId=:offerId ORDER BY linkedAt") fun observeForOffer(offerId: String): Flow<List<SupplierOfferEvidenceEntity>>
    @Query("SELECT * FROM supplier_offer_evidence") suspend fun getAll(): List<SupplierOfferEvidenceEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(row: SupplierOfferEvidenceEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(rows: List<SupplierOfferEvidenceEntity>)
    @Query("DELETE FROM supplier_offer_evidence") suspend fun deleteAll()
}
@Dao interface SourcingReviewDao {
    @Query("SELECT * FROM sourcing_reviews WHERE buyerRequirementId=:requirementId") fun observeForRequirement(requirementId: String): Flow<List<SourcingReviewEntity>>
    @Query("SELECT * FROM sourcing_reviews") suspend fun getAll(): List<SourcingReviewEntity>
    @Query("SELECT * FROM sourcing_reviews WHERE buyerRequirementId=:requirementId AND supplierOfferId=:offerId LIMIT 1") suspend fun getPair(requirementId: String, offerId: String): SourcingReviewEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(row: SourcingReviewEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(rows: List<SourcingReviewEntity>)
    @Update suspend fun update(row: SourcingReviewEntity)
    @Query("DELETE FROM sourcing_reviews") suspend fun deleteAll()
}

data class SupplierOfferDraft(val title: String, val description: String = "", val status: String = SupplierOfferStatus.AVAILABLE, val currency: String = "", val originCountry: String = "", val originCity: String = "", val notes: String = "")
data class SupplierOfferLineItemDraft(val productName: String, val specification: String = "", val material: String = "", val quantityAvailable: String = "", val quantityUnit: String = "", val minimumOrderQuantity: String = "", val unitPriceMinorUnits: Long? = null, val currency: String = "", val leadTimeDays: Int? = null)

class SupplierOfferRepository(private val db: TraderDatabase) {
    fun observeOffers() = db.supplierOfferDao().observeAll()
    fun observeItems(offerId: String) = db.supplierOfferLineItemDao().observeForOffer(offerId)
    fun observeEvidence(offerId: String) = db.supplierOfferEvidenceDao().observeForOffer(offerId)
    fun observeReviews(requirementId: String) = db.sourcingReviewDao().observeForRequirement(requirementId)
    fun observeRequirements() = db.buyerRequirementDao().observeAll()

    suspend fun create(
        organizationId: String,
        draft: SupplierOfferDraft,
        item: SupplierOfferLineItemDraft,
        sourceIds: List<String> = emptyList()
    ): SupplierOfferEntity = db.withTransaction {
        val organization = db.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Supplier company dossier does not exist.")
        val now = System.currentTimeMillis()
        val offer = buildOffer(organizationId, draft, UUID.randomUUID().toString(), now)
        db.supplierOfferDao().insert(offer)
        val firstItem = buildItem(offer.id, item, now)
        db.supplierOfferLineItemDao().insert(firstItem)
        sourceIds.distinct().forEach { sourceId ->
            val source = db.organizationSourceDao().getById(sourceId)
                ?: throw IllegalArgumentException("Save supplier evidence to the company dossier first.")
            require(source.organizationId == organizationId) {
                "Supplier evidence must belong to the supplier company dossier."
            }
            db.supplierOfferEvidenceDao().insert(SupplierOfferEvidenceEntity(UUID.randomUUID().toString(), offer.id, sourceId, now))
        }
        db.organizationDao().touch(organizationId, now)
        db.appendBusinessActivity(
            organizationId, organization.canonicalName, BusinessActivityEntityType.SUPPLIER_OFFER, offer.id,
            offer.title, BusinessActivityAction.CREATED, "Supplier offer created",
            toStatus = offer.status, details = "Initial line item: ${firstItem.productName}",
            occurredAt = now
        )
        offer
    }

    suspend fun addLineItem(offerId: String, draft: SupplierOfferLineItemDraft): SupplierOfferLineItemEntity = db.withTransaction {
        val offer = db.supplierOfferDao().getById(offerId)
            ?: throw IllegalArgumentException("Supplier offer no longer exists.")
        val organization = db.organizationDao().getById(offer.organizationId)
            ?: throw IllegalArgumentException("Supplier company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val item = buildItem(offerId, draft, now)
        db.supplierOfferLineItemDao().insert(item)
        db.organizationDao().touch(offer.organizationId, now)
        db.appendBusinessActivity(
            offer.organizationId, organization.canonicalName, BusinessActivityEntityType.SUPPLIER_OFFER, offer.id,
            offer.title, BusinessActivityAction.LINE_ADDED, "Supplier offer line item added",
            details = item.productName, occurredAt = now
        )
        item
    }

    suspend fun linkEvidence(offerId: String, sourceId: String): SupplierOfferEvidenceEntity = db.withTransaction {
        val offer = db.supplierOfferDao().getById(offerId)
            ?: throw IllegalArgumentException("Supplier offer no longer exists.")
        val organization = db.organizationDao().getById(offer.organizationId)
            ?: throw IllegalArgumentException("Supplier company dossier no longer exists.")
        val source = db.organizationSourceDao().getById(sourceId)
            ?: throw IllegalArgumentException("Save supplier evidence to the company dossier first.")
        require(source.organizationId == offer.organizationId) {
            "Supplier evidence must belong to the supplier company dossier."
        }
        val existing = db.supplierOfferEvidenceDao().getAll().firstOrNull {
            it.offerId == offerId && it.sourceId == sourceId
        }
        if (existing != null) return@withTransaction existing
        val now = System.currentTimeMillis()
        val link = SupplierOfferEvidenceEntity(UUID.randomUUID().toString(), offerId, sourceId, now)
        db.supplierOfferEvidenceDao().insert(link)
        db.appendBusinessActivity(
            offer.organizationId, organization.canonicalName, BusinessActivityEntityType.SUPPLIER_OFFER, offer.id,
            offer.title, BusinessActivityAction.EVIDENCE_LINKED, "Supplier offer evidence linked",
            details = source.title, occurredAt = now
        )
        link
    }

    suspend fun setStatus(offerId: String, status: String): Boolean = db.withTransaction {
        require(status in SupplierOfferStatus.ALL) { "Unsupported supplier offer status." }
        val offer = db.supplierOfferDao().getById(offerId) ?: return@withTransaction false
        if (offer.status == status) return@withTransaction true
        val organization = db.organizationDao().getById(offer.organizationId)
            ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.supplierOfferDao().update(offer.copy(status = status, updatedAt = now))
        db.organizationDao().touch(offer.organizationId, now)
        db.appendBusinessActivity(
            offer.organizationId, organization.canonicalName, BusinessActivityEntityType.SUPPLIER_OFFER, offer.id,
            offer.title, BusinessActivityAction.STATUS_CHANGED, "Supplier offer status changed",
            fromStatus = offer.status, toStatus = status, occurredAt = now
        )
        true
    }

    suspend fun review(requirementId: String, offerId: String, status: String, rationale: String = ""): SourcingReviewEntity = db.withTransaction {
        require(status in SourcingReviewStatus.ALL) { "Unsupported sourcing review decision." }
        val requirement = db.buyerRequirementDao().getById(requirementId)
            ?: throw IllegalArgumentException("Buyer requirement no longer exists.")
        val offer = db.supplierOfferDao().getById(offerId)
            ?: throw IllegalArgumentException("Supplier offer no longer exists.")
        val buyerOrganization = db.organizationDao().getById(requirement.organizationId)
            ?: throw IllegalArgumentException("Buyer company dossier no longer exists.")
        val supplierOrganization = db.organizationDao().getById(offer.organizationId)
            ?: throw IllegalArgumentException("Supplier company dossier no longer exists.")
        require(rationale.length <= 2_000) { "Review note is too long." }
        val cleanRationale = rationale.trim().ifBlank { null }
        val now = System.currentTimeMillis()
        val old = db.sourcingReviewDao().getPair(requirementId, offerId)
        if (old?.status == status && old.rationale == cleanRationale) return@withTransaction old
        val row = if (old == null) {
            SourcingReviewEntity(UUID.randomUUID().toString(), requirementId, offerId, status, cleanRationale, now, now)
                .also { db.sourcingReviewDao().insert(it) }
        } else {
            old.copy(status = status, rationale = cleanRationale, updatedAt = now)
                .also { db.sourcingReviewDao().update(it) }
        }
        val details = buildString {
            append("Supplier: ").append(supplierOrganization.canonicalName)
            if (!cleanRationale.isNullOrBlank()) append(" · ").append(cleanRationale)
        }.take(2_000)
        db.appendBusinessActivity(
            buyerOrganization.id, buyerOrganization.canonicalName, BusinessActivityEntityType.SOURCING_REVIEW,
            row.id, "${requirement.title} ↔ ${offer.title}".take(300), BusinessActivityAction.DECISION_RECORDED,
            "Supplier review decision recorded", fromStatus = old?.status, toStatus = status,
            details = details, occurredAt = now
        )
        row
    }

    suspend fun deleteOffer(offerId: String): Boolean = db.withTransaction {
        val offer = db.supplierOfferDao().getById(offerId) ?: return@withTransaction false
        val organization = db.organizationDao().getById(offer.organizationId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.appendBusinessActivity(
            offer.organizationId, organization.canonicalName, BusinessActivityEntityType.SUPPLIER_OFFER, offer.id,
            offer.title, BusinessActivityAction.DELETED, "Supplier offer deleted",
            fromStatus = offer.status, occurredAt = now
        )
        db.supplierOfferDao().deleteById(offerId) > 0
    }

    private fun buildOffer(org: String, draft: SupplierOfferDraft, id: String, now: Long): SupplierOfferEntity {
        require(draft.status in SupplierOfferStatus.ALL) { "Unsupported supplier offer status." }
        val currency = optional(draft.currency, 3, "Currency")?.uppercase()?.also {
            require(Regex("^[A-Z]{3}$").matches(it)) { "Currency must be a three-letter ISO code." }
        }
        return SupplierOfferEntity(
            id, org, required(draft.title, 200, "Offer title"), optional(draft.description, 4_000, "Description"),
            draft.status, currency, optional(draft.originCountry, 120, "Origin country"),
            optional(draft.originCity, 120, "Origin city"), optional(draft.notes, 2_000, "Notes"), now, now
        )
    }

    private fun buildItem(offerId: String, draft: SupplierOfferLineItemDraft, now: Long): SupplierOfferLineItemEntity {
        val available = quantity(draft.quantityAvailable, "Available quantity")
        val moq = quantity(draft.minimumOrderQuantity, "Minimum order quantity")
        require(draft.unitPriceMinorUnits == null || draft.unitPriceMinorUnits >= 0) {
            "Unit price cannot be negative."
        }
        val currency = optional(draft.currency, 3, "Currency")?.uppercase()?.also {
            require(Regex("^[A-Z]{3}$").matches(it)) { "Currency must be a three-letter ISO code." }
        }
        require(draft.unitPriceMinorUnits == null || currency != null) {
            "A currency is required when a unit price is provided."
        }
        require(draft.leadTimeDays == null || draft.leadTimeDays in 0..3_650) {
            "Lead time must be between 0 and 3650 days."
        }
        return SupplierOfferLineItemEntity(
            UUID.randomUUID().toString(), offerId, required(draft.productName, 200, "Product or service"),
            optional(draft.specification, 2_000, "Specification"), optional(draft.material, 120, "Material"),
            available, optional(draft.quantityUnit, 40, "Quantity unit"), moq, draft.unitPriceMinorUnits,
            currency, draft.leadTimeDays, now
        )
    }

    private fun quantity(raw: String, label: String): String? = optional(raw, 40, label)?.also {
        val value = runCatching { BigDecimal(it) }
            .getOrElse { throw IllegalArgumentException("$label must be a positive decimal number.") }
        require(value > BigDecimal.ZERO && value.scale() <= 6 && value.precision() <= 18) {
            "$label must be a positive decimal with at most six decimal places."
        }
    }

    private fun required(value: String, max: Int, label: String): String =
        value.trim().also { require(it.isNotBlank() && it.length <= max) { "$label is required and must be at most $max characters." } }

    private fun optional(value: String, max: Int, label: String): String =
        value.trim().also { require(it.length <= max) { "$label is too long." } }.ifBlank { null }
}

/** Explainable comparisons only. Missing or semantically different free text is UNKNOWN, never a pass. */
object SourcingMatchEngine {
    const val SUPPORTED = "SUPPORTED"
    const val CONTRADICTED = "CONTRADICTED"
    const val UNKNOWN = "UNKNOWN"
    data class Check(val field: String, val result: String, val explanation: String)
    data class Assessment(val checks: List<Check>) {
        val hasConflict get() = checks.any { it.result == CONTRADICTED }
        val hasUnknown get() = checks.any { it.result == UNKNOWN }
        val disposition get() = when { hasConflict -> "CONSTRAINT_CONFLICT"; hasUnknown -> "MORE_INFORMATION_NEEDED"; else -> "REVIEWABLE" }
    }

    fun compare(requirement: BuyerRequirementEntity, buyer: BuyerRequirementLineItemEntity, supplier: SupplierOfferLineItemEntity): Assessment {
        val checks = mutableListOf<Check>()
        val buyerName = normalize(buyer.productName)
        val supplierName = normalize(supplier.productName)
        checks += when {
            buyerName.isBlank() || supplierName.isBlank() -> Check("Product", UNKNOWN, "Product identity is missing.")
            buyerName == supplierName -> Check("Product", SUPPORTED, "Product names match exactly after normalization.")
            else -> Check("Product", UNKNOWN, "Names differ; technical equivalence needs human review.")
        }
        val buyerSpec = normalize(buyer.specification.orEmpty())
        val supplierSpec = normalize(supplier.specification.orEmpty())
        checks += when {
            buyerSpec.isBlank() || supplierSpec.isBlank() -> Check("Specification", UNKNOWN, "One or both specifications are missing.")
            buyerSpec == supplierSpec -> Check("Specification", SUPPORTED, "Specification wording matches exactly; this is not independent technical certification.")
            else -> Check("Specification", UNKNOWN, "Specification wording differs; equivalence is not inferred.")
        }
        val requested = decimal(buyer.quantity)
        val available = decimal(supplier.quantityAvailable)
        val buyerUnit = normalize(buyer.unit.orEmpty())
        val supplierUnit = normalize(supplier.quantityUnit.orEmpty())
        checks += when {
            requested == null || available == null || buyerUnit.isBlank() || supplierUnit.isBlank() -> Check("Capacity", UNKNOWN, "Quantity or comparable units are missing.")
            buyerUnit != supplierUnit -> Check("Capacity", UNKNOWN, "Units differ and are not converted.")
            available >= requested -> Check("Capacity", SUPPORTED, "Stated available quantity covers the request.")
            else -> Check("Capacity", CONTRADICTED, "Stated available quantity is below the requested quantity.")
        }
        val moq = decimal(supplier.minimumOrderQuantity)
        checks += when {
            requested == null || moq == null || buyerUnit.isBlank() || supplierUnit.isBlank() -> Check("Minimum order", UNKNOWN, "Requested quantity or supplier MOQ is missing.")
            buyerUnit != supplierUnit -> Check("Minimum order", UNKNOWN, "Units differ and are not converted.")
            requested >= moq -> Check("Minimum order", SUPPORTED, "Requested quantity meets the stated MOQ.")
            else -> Check("Minimum order", CONTRADICTED, "Requested quantity is below the stated MOQ.")
        }
        checks += when {
            buyer.targetUnitPriceMinorUnits == null || supplier.unitPriceMinorUnits == null -> Check("Price", UNKNOWN, "Buyer target or supplier price is missing.")
            buyerUnit.isBlank() || supplier.currency.isNullOrBlank() -> Check("Price", UNKNOWN, "Price basis or currency is missing.")
            !requirement.currency.equals(supplier.currency, ignoreCase = true) -> Check("Price", UNKNOWN, "Currencies differ; no exchange rate is assumed.")
            supplier.unitPriceMinorUnits <= buyer.targetUnitPriceMinorUnits -> Check("Price", SUPPORTED, "Stated supplier unit price is at or below the buyer target.")
            else -> Check("Price", UNKNOWN, "Stated supplier unit price exceeds the target; negotiate or review.")
        }
        return Assessment(checks)
    }

    private fun decimal(raw: String?): BigDecimal? = raw?.let { runCatching { BigDecimal(it) }.getOrNull() }
    private fun normalize(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
}
