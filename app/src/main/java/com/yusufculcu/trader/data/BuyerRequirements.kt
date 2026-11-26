package com.yusufculcu.trader.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

object BuyerRequirementState {
    const val DRAFT="DRAFT"; const val OPEN="OPEN"; const val PAUSED="PAUSED"; const val FULFILLED="FULFILLED"; const val CLOSED="CLOSED"
    val ALL=setOf(DRAFT,OPEN,PAUSED,FULFILLED,CLOSED)
}
@Entity(tableName="buyer_requirements", foreignKeys=[ForeignKey(entity=OrganizationEntity::class,parentColumns=["id"],childColumns=["organizationId"],onDelete=ForeignKey.CASCADE)], indices=[Index("organizationId"),Index("status"),Index("updatedAt"),Index("deletedAt")])
data class BuyerRequirementEntity(@PrimaryKey val id:String,val organizationId:String,val title:String,val description:String?,val status:String,val destinationCountry:String?,val destinationCity:String?,val requiredByText:String?,val currency:String?,val budgetMinorUnits:Long?,val notes:String?,val createdAt:Long,val updatedAt:Long,val deletedAt:Long?=null)
@Entity(tableName="buyer_requirement_items", foreignKeys=[ForeignKey(entity=BuyerRequirementEntity::class,parentColumns=["id"],childColumns=["requirementId"],onDelete=ForeignKey.CASCADE)], indices=[Index("requirementId")])
data class BuyerRequirementLineItemEntity(@PrimaryKey val id:String,val requirementId:String,val productName:String,val specification:String?,val quantity:String?,val unit:String?,val targetUnitPriceMinorUnits:Long?,val createdAt:Long)
@Entity(tableName="buyer_requirement_evidence", foreignKeys=[ForeignKey(entity=BuyerRequirementEntity::class,parentColumns=["id"],childColumns=["requirementId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=OrganizationSourceEntity::class,parentColumns=["id"],childColumns=["sourceId"],onDelete=ForeignKey.CASCADE)],indices=[Index("requirementId"),Index("sourceId"),Index(value=["requirementId","sourceId"],unique=true)])
data class BuyerRequirementEvidenceEntity(@PrimaryKey val id:String,val requirementId:String,val sourceId:String,val linkedAt:Long)

@Dao interface BuyerRequirementDao {
 @Query("SELECT * FROM buyer_requirements WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeAll(): Flow<List<BuyerRequirementEntity>>
 @Query("SELECT * FROM buyer_requirements WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC") fun observeDeleted(): Flow<List<BuyerRequirementEntity>>
 @Query("SELECT * FROM buyer_requirements WHERE organizationId=:organizationId AND deletedAt IS NULL ORDER BY updatedAt DESC") fun observeForOrganization(organizationId:String):Flow<List<BuyerRequirementEntity>>
 @Query("SELECT * FROM buyer_requirements") suspend fun getAll():List<BuyerRequirementEntity>
 @Query("SELECT * FROM buyer_requirements WHERE id=:id AND deletedAt IS NULL LIMIT 1") suspend fun getById(id:String):BuyerRequirementEntity?
 @Query("SELECT * FROM buyer_requirements WHERE id=:id LIMIT 1") suspend fun getIncludingDeletedById(id:String):BuyerRequirementEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementEntity>)
 @Update suspend fun update(row:BuyerRequirementEntity)
 @Query("DELETE FROM buyer_requirements WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirements") suspend fun deleteAll()
}
@Dao interface BuyerRequirementLineItemDao {
 @Query("SELECT * FROM buyer_requirement_items WHERE requirementId=:requirementId ORDER BY createdAt,id") fun observeForRequirement(requirementId:String):Flow<List<BuyerRequirementLineItemEntity>>
 @Query("SELECT * FROM buyer_requirement_items") suspend fun getAll():List<BuyerRequirementLineItemEntity>
 @Query("SELECT * FROM buyer_requirement_items WHERE id=:id LIMIT 1") suspend fun getById(id:String):BuyerRequirementLineItemEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementLineItemEntity)
 @Update suspend fun update(row:BuyerRequirementLineItemEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementLineItemEntity>)
 @Query("DELETE FROM buyer_requirement_items WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirement_items") suspend fun deleteAll()
}
@Dao interface BuyerRequirementEvidenceDao {
 @Query("SELECT * FROM buyer_requirement_evidence WHERE requirementId=:requirementId ORDER BY linkedAt") fun observeForRequirement(requirementId:String):Flow<List<BuyerRequirementEvidenceEntity>>
 @Query("SELECT * FROM buyer_requirement_evidence") suspend fun getAll():List<BuyerRequirementEvidenceEntity>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(row:BuyerRequirementEvidenceEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertAll(rows:List<BuyerRequirementEvidenceEntity>)
 @Query("DELETE FROM buyer_requirement_evidence WHERE id=:id") suspend fun deleteById(id:String):Int
 @Query("DELETE FROM buyer_requirement_evidence") suspend fun deleteAll()
}
data class BuyerRequirementDraft(val title:String,val description:String="",val status:String=BuyerRequirementState.DRAFT,val destinationCountry:String="",val destinationCity:String="",val requiredByText:String="",val currency:String="",val budgetMinorUnits:Long?=null,val notes:String="")
data class BuyerRequirementLineItemDraft(val productName:String,val specification:String="",val quantity:String="",val unit:String="",val targetUnitPriceMinorUnits:Long?=null)

/** User-authored requirements with precise line-item quantities and source links constrained to the same dossier. */
class BuyerRequirementRepository(private val db: TraderDatabase) {
    fun observeAll() = db.buyerRequirementDao().observeAll()
    fun observeDeleted() = db.buyerRequirementDao().observeDeleted()
    fun observeForOrganization(id: String) = db.buyerRequirementDao().observeForOrganization(id)
    fun observeItems(id: String) = db.buyerRequirementLineItemDao().observeForRequirement(id)
    fun observeEvidence(id: String) = db.buyerRequirementEvidenceDao().observeForRequirement(id)

    suspend fun create(orgId: String, draft: BuyerRequirementDraft) = db.withTransaction {
        val organization = db.organizationDao().getById(orgId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val now = System.currentTimeMillis()
        val row = build(orgId, draft, UUID.randomUUID().toString(), now)
        db.buyerRequirementDao().insert(row)
        db.organizationDao().touch(orgId, now)
        db.appendBusinessActivity(
            orgId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, row.id,
            row.title, BusinessActivityAction.CREATED, "Buyer requirement created",
            toStatus = row.status, occurredAt = now
        )
        row
    }

    suspend fun update(id: String, draft: BuyerRequirementDraft) = db.withTransaction {
        val old = db.buyerRequirementDao().getById(id)
            ?: throw IllegalArgumentException("Buyer requirement no longer exists.")
        val organization = db.organizationDao().getById(old.organizationId)
            ?: throw IllegalArgumentException("Buyer company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val row = build(old.organizationId, draft, old.id, old.createdAt).copy(updatedAt = now)
        db.buyerRequirementDao().update(row)
        db.organizationDao().touch(old.organizationId, now)
        db.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, row.id,
            row.title,
            if (old.status == row.status) BusinessActivityAction.UPDATED else BusinessActivityAction.STATUS_CHANGED,
            if (old.status == row.status) "Buyer requirement updated" else "Buyer requirement status changed",
            fromStatus = old.status.takeIf { it != row.status }, toStatus = row.status,
            occurredAt = now
        )
        row
    }

    suspend fun setStatus(id: String, status: String): Boolean = db.withTransaction {
        require(status in BuyerRequirementState.ALL) { "Unsupported buyer requirement status." }
        val old = db.buyerRequirementDao().getById(id) ?: return@withTransaction false
        if (old.status == status) return@withTransaction true
        val organization = db.organizationDao().getById(old.organizationId)
            ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.buyerRequirementDao().update(old.copy(status = status, updatedAt = now))
        db.organizationDao().touch(old.organizationId, now)
        db.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, old.id,
            old.title, BusinessActivityAction.STATUS_CHANGED, "Buyer requirement status changed",
            fromStatus = old.status, toStatus = status, occurredAt = now
        )
        true
    }

    suspend fun addLineItem(requirementId: String, draft: BuyerRequirementLineItemDraft) = db.withTransaction {
        val req = db.buyerRequirementDao().getById(requirementId)
            ?: throw IllegalArgumentException("Buyer requirement does not exist.")
        val organization = db.organizationDao().getById(req.organizationId)
            ?: throw IllegalArgumentException("Buyer company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val row = buildLineItem(requirementId, draft, UUID.randomUUID().toString(), now)
        db.buyerRequirementLineItemDao().insert(row)
        db.buyerRequirementDao().update(req.copy(updatedAt = now))
        db.organizationDao().touch(req.organizationId, now)
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.LINE_ADDED, "Buyer requirement line item added",
            details = row.productName, occurredAt = now
        )
        row
    }

    suspend fun updateLineItem(id: String, draft: BuyerRequirementLineItemDraft) = db.withTransaction {
        val old = db.buyerRequirementLineItemDao().getById(id)
            ?: throw IllegalArgumentException("Buyer requirement line item no longer exists.")
        val req = db.buyerRequirementDao().getById(old.requirementId)
            ?: throw IllegalArgumentException("Buyer requirement no longer exists.")
        val organization = db.organizationDao().getById(req.organizationId)
            ?: throw IllegalArgumentException("Buyer company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val row = buildLineItem(old.requirementId, draft, old.id, old.createdAt)
        db.buyerRequirementLineItemDao().update(row)
        db.buyerRequirementDao().update(req.copy(updatedAt = now))
        db.organizationDao().touch(req.organizationId, now)
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.LINE_UPDATED, "Buyer requirement line item updated",
            details = row.productName, occurredAt = now
        )
        row
    }

    suspend fun linkEvidence(requirementId: String, sourceId: String) = db.withTransaction {
        val req = db.buyerRequirementDao().getById(requirementId)
            ?: throw IllegalArgumentException("Buyer requirement does not exist.")
        val organization = db.organizationDao().getById(req.organizationId)
            ?: throw IllegalArgumentException("Buyer company dossier no longer exists.")
        val source = db.organizationSourceDao().getById(sourceId)
            ?: throw IllegalArgumentException("Save the evidence source to this company dossier first.")
        require(source.organizationId == req.organizationId) { "Evidence must belong to the same company dossier." }
        val existing = db.buyerRequirementEvidenceDao().getAll()
            .firstOrNull { it.requirementId == requirementId && it.sourceId == sourceId }
        if (existing != null) return@withTransaction existing
        val now = System.currentTimeMillis()
        val link = BuyerRequirementEvidenceEntity(UUID.randomUUID().toString(), requirementId, sourceId, now)
        db.buyerRequirementEvidenceDao().insert(link)
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.EVIDENCE_LINKED, "Buyer requirement evidence linked",
            details = source.title, occurredAt = now
        )
        link
    }

    /** Move a requirement to recoverable trash; related sourcing data remains linked. */
    suspend fun deleteRequirement(id: String): Boolean = db.withTransaction {
        val req = db.buyerRequirementDao().getById(id) ?: return@withTransaction false
        val organization = db.organizationDao().getById(req.organizationId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.buyerRequirementDao().update(req.copy(deletedAt = now, updatedAt = now))
        db.organizationDao().touch(req.organizationId, now)
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.DELETED, "Buyer requirement moved to trash",
            fromStatus = req.status,
            details = "Requirement, line items, evidence links and sourcing decisions retained for recovery.",
            occurredAt = now
        )
        true
    }

    suspend fun restoreRequirement(id: String): Boolean = db.withTransaction {
        val req = db.buyerRequirementDao().getIncludingDeletedById(id) ?: return@withTransaction false
        if (req.deletedAt == null) return@withTransaction true
        val organization = db.organizationDao().getById(req.organizationId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.buyerRequirementDao().update(req.copy(deletedAt = null, updatedAt = now))
        db.organizationDao().touch(req.organizationId, now)
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.RESTORED, "Buyer requirement restored",
            fromStatus = req.status,
            details = "Restored from trash with original line items, evidence links and sourcing decisions.",
            occurredAt = now
        )
        true
    }

    suspend fun deleteLineItem(id: String): Boolean = db.withTransaction {
        val item = db.buyerRequirementLineItemDao().getAll().firstOrNull { it.id == id }
            ?: return@withTransaction false
        val req = db.buyerRequirementDao().getById(item.requirementId) ?: return@withTransaction false
        val organization = db.organizationDao().getById(req.organizationId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.DELETED, "Buyer requirement line item removed",
            details = item.productName, occurredAt = now
        )
        db.buyerRequirementLineItemDao().deleteById(id) > 0
    }

    suspend fun deleteEvidenceLink(id: String): Boolean = db.withTransaction {
        val link = db.buyerRequirementEvidenceDao().getAll().firstOrNull { it.id == id }
            ?: return@withTransaction false
        val req = db.buyerRequirementDao().getById(link.requirementId) ?: return@withTransaction false
        val organization = db.organizationDao().getById(req.organizationId) ?: return@withTransaction false
        val source = db.organizationSourceDao().getById(link.sourceId)
        val now = System.currentTimeMillis()
        db.appendBusinessActivity(
            req.organizationId, organization.canonicalName, BusinessActivityEntityType.BUYER_REQUIREMENT, req.id,
            req.title, BusinessActivityAction.UPDATED, "Buyer requirement evidence unlinked",
            details = source?.title.orEmpty(), occurredAt = now
        )
        db.buyerRequirementEvidenceDao().deleteById(id) > 0
    }

    private fun buildLineItem(
        requirementId: String,
        draft: BuyerRequirementLineItemDraft,
        id: String,
        createdAt: Long
    ): BuyerRequirementLineItemEntity {
        val quantity = optional(draft.quantity, 40, "Quantity")?.also { raw ->
            val number = runCatching { BigDecimal(raw) }
                .getOrElse { throw IllegalArgumentException("Quantity must be a positive decimal number.") }
            require(number > BigDecimal.ZERO && number.scale() <= 6 && number.precision() <= 18) {
                "Quantity must be a positive decimal with at most six decimal places."
            }
        }
        require(draft.targetUnitPriceMinorUnits == null || draft.targetUnitPriceMinorUnits >= 0) {
            "Target unit price cannot be negative."
        }
        return BuyerRequirementLineItemEntity(
            id, requirementId, required(draft.productName, 200, "Product or service"),
            optional(draft.specification, 2_000, "Specification"), quantity, optional(draft.unit, 40, "Unit"),
            draft.targetUnitPriceMinorUnits, createdAt
        )
    }

    private fun build(org: String, draft: BuyerRequirementDraft, id: String, createdAt: Long): BuyerRequirementEntity {
        require(draft.status in BuyerRequirementState.ALL) { "Unsupported buyer requirement status." }
        val currency = optional(draft.currency, 3, "Currency")?.uppercase()?.also {
            require(Regex("^[A-Z]{3}$").matches(it)) { "Currency must be a three-letter ISO code." }
        }
        require(draft.budgetMinorUnits == null || draft.budgetMinorUnits >= 0) { "Budget cannot be negative." }
        return BuyerRequirementEntity(
            id, org, required(draft.title, 200, "Requirement title"), optional(draft.description, 4_000, "Description"),
            draft.status, optional(draft.destinationCountry, 120, "Destination country"),
            optional(draft.destinationCity, 120, "Destination city"), optional(draft.requiredByText, 120, "Required-by timing"),
            currency, draft.budgetMinorUnits, optional(draft.notes, 2_000, "Notes"), createdAt, createdAt
        )
    }

    private fun required(value: String, max: Int, label: String): String =
        value.trim().also { require(it.isNotBlank() && it.length <= max) { "$label is required and must be at most $max characters." } }

    private fun optional(value: String, max: Int, label: String): String? =
        value.trim().also { require(it.length <= max) { "$label is too long." } }.ifBlank { null }
}

val MIGRATION_9_10=object:Migration(9,10){
 override fun migrate(db:SupportSQLiteDatabase){
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirements (id TEXT NOT NULL, organizationId TEXT NOT NULL, title TEXT NOT NULL, description TEXT, status TEXT NOT NULL, destinationCountry TEXT, destinationCity TEXT, requiredByText TEXT, currency TEXT, budgetMinorUnits INTEGER, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_organizationId ON buyer_requirements (organizationId)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_status ON buyer_requirements (status)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_updatedAt ON buyer_requirements (updatedAt)")
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirement_items (id TEXT NOT NULL, requirementId TEXT NOT NULL, productName TEXT NOT NULL, specification TEXT, quantity TEXT, unit TEXT, targetUnitPriceMinorUnits INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(requirementId) REFERENCES buyer_requirements(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_items_requirementId ON buyer_requirement_items (requirementId)")
  db.execSQL("""CREATE TABLE IF NOT EXISTS buyer_requirement_evidence (id TEXT NOT NULL, requirementId TEXT NOT NULL, sourceId TEXT NOT NULL, linkedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(requirementId) REFERENCES buyer_requirements(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceId) REFERENCES organization_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_evidence_requirementId ON buyer_requirement_evidence (requirementId)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirement_evidence_sourceId ON buyer_requirement_evidence (sourceId)")
  db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_buyer_requirement_evidence_requirementId_sourceId ON buyer_requirement_evidence (requirementId, sourceId)")
 }
}


/** Adds nullable tombstones without rewriting or cascading existing sourcing data. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE buyer_requirements ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE supplier_offers ADD COLUMN deletedAt INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_buyer_requirements_deletedAt ON buyer_requirements (deletedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offers_deletedAt ON supplier_offers (deletedAt)")
    }
}
