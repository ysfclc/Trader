package com.yusufculcu.trader.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import java.util.UUID

object ProductStatus {
    const val DRAFT = "DRAFT"
    const val ACTIVE = "ACTIVE"
    const val DISCONTINUED = "DISCONTINUED"
    const val ARCHIVED = "ARCHIVED"
    val ALL = setOf(DRAFT, ACTIVE, DISCONTINUED, ARCHIVED)
}

@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("organizationId"),
        Index("status"),
        Index("updatedAt"),
        Index(value = ["organizationId", "skuKey"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val skuCode: String?,
    val skuKey: String?,
    val name: String,
    val description: String?,
    val specification: String?,
    val material: String?,
    val unit: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "product_evidence",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = OrganizationSourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("productId"),
        Index("sourceId"),
        Index(value = ["productId", "sourceId"], unique = true)
    ]
)
data class ProductEvidenceEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val sourceId: String,
    val linkedAt: Long
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE organizationId = :organizationId ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun observeForOrganization(organizationId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE organizationId = :organizationId AND skuKey = :skuKey LIMIT 1")
    suspend fun findByOrganizationAndSkuKey(organizationId: String, skuKey: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<ProductEntity>)

    @Update
    suspend fun update(row: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface ProductEvidenceDao {
    @Query("SELECT * FROM product_evidence WHERE productId = :productId ORDER BY linkedAt, id")
    fun observeForProduct(productId: String): Flow<List<ProductEvidenceEntity>>

    @Query("SELECT * FROM product_evidence WHERE productId = :productId ORDER BY linkedAt, id")
    suspend fun getForProduct(productId: String): List<ProductEvidenceEntity>

    @Query("SELECT * FROM product_evidence")
    suspend fun getAll(): List<ProductEvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: ProductEvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<ProductEvidenceEntity>)

    @Query("DELETE FROM product_evidence WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM product_evidence")
    suspend fun deleteAll()
}

data class ProductDraft(
    val skuCode: String = "",
    val name: String,
    val description: String = "",
    val specification: String = "",
    val material: String = "",
    val unit: String = "",
    val status: String = ProductStatus.DRAFT
)

/**
 * Company-owned product master data. It is intentionally distinct from buyer
 * requirement lines and supplier offer lines: creating one never asserts stock,
 * a price, an active offer, technical equivalence or a buyer match.
 */
class ProductCatalogRepository(private val db: TraderDatabase) {
    fun observeForOrganization(organizationId: String) = db.productDao().observeForOrganization(organizationId)
    fun observeEvidence(productId: String) = db.productEvidenceDao().observeForProduct(productId)

    suspend fun create(organizationId: String, draft: ProductDraft): ProductEntity = db.withTransaction {
        val organization = db.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val now = System.currentTimeMillis()
        val row = build(organizationId, draft, UUID.randomUUID().toString(), now)
        ensureUniqueSku(organizationId, row.skuKey, null)
        db.productDao().insert(row)
        db.organizationDao().touch(organizationId, now)
        db.appendBusinessActivity(
            organizationId, organization.canonicalName, BusinessActivityEntityType.PRODUCT, row.id,
            row.name, BusinessActivityAction.CREATED, "Product catalogue record created",
            toStatus = row.status,
            details = "A company product master record was created; stock, pricing and buyer fit were not inferred.",
            occurredAt = now
        )
        row
    }

    suspend fun update(productId: String, draft: ProductDraft): ProductEntity = db.withTransaction {
        val old = db.productDao().getById(productId)
            ?: throw IllegalArgumentException("Product record no longer exists.")
        val organization = db.organizationDao().getById(old.organizationId)
            ?: throw IllegalArgumentException("Company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val row = build(old.organizationId, draft, old.id, old.createdAt).copy(updatedAt = now)
        ensureUniqueSku(old.organizationId, row.skuKey, old.id)
        db.productDao().update(row)
        db.organizationDao().touch(old.organizationId, now)
        db.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.PRODUCT, row.id,
            row.name,
            if (old.status == row.status) BusinessActivityAction.UPDATED else BusinessActivityAction.STATUS_CHANGED,
            if (old.status == row.status) "Product catalogue record updated" else "Product catalogue status changed",
            fromStatus = old.status.takeIf { it != row.status },
            toStatus = row.status,
            occurredAt = now
        )
        row
    }

    suspend fun setStatus(productId: String, status: String): Boolean = db.withTransaction {
        require(status in ProductStatus.ALL) { "Unsupported product status." }
        val old = db.productDao().getById(productId) ?: return@withTransaction false
        if (old.status == status) return@withTransaction true
        val organization = db.organizationDao().getById(old.organizationId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        db.productDao().update(old.copy(status = status, updatedAt = now))
        db.organizationDao().touch(old.organizationId, now)
        db.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.PRODUCT, old.id,
            old.name, BusinessActivityAction.STATUS_CHANGED, "Product catalogue status changed",
            fromStatus = old.status, toStatus = status, occurredAt = now
        )
        true
    }

    /**
     * Reconciles the complete source set atomically. Every source must already
     * belong to this company's dossier; links are provenance, not verification.
     */
    suspend fun setEvidenceLinks(productId: String, sourceIds: Set<String>): Int = db.withTransaction {
        val product = db.productDao().getById(productId)
            ?: throw IllegalArgumentException("Product record no longer exists.")
        val organization = db.organizationDao().getById(product.organizationId)
            ?: throw IllegalArgumentException("Company dossier no longer exists.")
        require(sourceIds.size <= 100) { "A product can link at most 100 source records." }
        val desiredIds = sourceIds.map { it.trim() }.toSet()
        require(desiredIds.size == sourceIds.size && desiredIds.all { it.isNotBlank() && it.length <= 128 }) {
            "Product evidence source selection is invalid."
        }
        val desiredSources = desiredIds.map { sourceId ->
            db.organizationSourceDao().getById(sourceId)
                ?: throw IllegalArgumentException("A selected company source no longer exists.")
        }
        require(desiredSources.all { it.organizationId == product.organizationId }) {
            "Product evidence can only use sources attached to the same company dossier."
        }
        val current = db.productEvidenceDao().getForProduct(productId)
        val currentIds = current.map { it.sourceId }.toSet()
        val toRemove = current.filter { it.sourceId !in desiredIds }
        val toAdd = desiredSources.filter { it.id !in currentIds }
        if (toRemove.isEmpty() && toAdd.isEmpty()) return@withTransaction 0

        val now = System.currentTimeMillis()
        toRemove.forEach { link ->
            val source = db.organizationSourceDao().getById(link.sourceId)
            db.productEvidenceDao().deleteById(link.id)
            db.appendBusinessActivity(
                product.organizationId, organization.canonicalName, BusinessActivityEntityType.PRODUCT, product.id,
                product.name, BusinessActivityAction.EVIDENCE_UNLINKED, "Product source evidence unlinked",
                details = source?.title.orEmpty(), occurredAt = now
            )
        }
        toAdd.forEach { source ->
            db.productEvidenceDao().insert(ProductEvidenceEntity(UUID.randomUUID().toString(), product.id, source.id, now))
            db.appendBusinessActivity(
                product.organizationId, organization.canonicalName, BusinessActivityEntityType.PRODUCT, product.id,
                product.name, BusinessActivityAction.EVIDENCE_LINKED, "Product source evidence linked",
                details = source.title, occurredAt = now
            )
        }
        db.productDao().update(product.copy(updatedAt = now))
        db.organizationDao().touch(product.organizationId, now)
        toRemove.size + toAdd.size
    }

    private suspend fun ensureUniqueSku(organizationId: String, skuKey: String?, excludingId: String?) {
        if (skuKey == null) return
        val existing = db.productDao().findByOrganizationAndSkuKey(organizationId, skuKey)
        require(existing == null || existing.id == excludingId) {
            "This company already has a product with that SKU code."
        }
    }

    private fun build(organizationId: String, draft: ProductDraft, id: String, createdAt: Long): ProductEntity {
        require(draft.status in ProductStatus.ALL) { "Unsupported product status." }
        val skuCode = optional(draft.skuCode, 80, "SKU code")
        val skuKey = skuCode?.uppercase(Locale.ROOT)
        return ProductEntity(
            id = id,
            organizationId = organizationId,
            skuCode = skuCode,
            skuKey = skuKey,
            name = required(draft.name, 200, "Product name"),
            description = optional(draft.description, 4_000, "Description"),
            specification = optional(draft.specification, 2_000, "Specification"),
            material = optional(draft.material, 120, "Material"),
            unit = optional(draft.unit, 40, "Unit"),
            status = draft.status,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    private fun required(value: String, max: Int, label: String): String =
        value.trim().also { require(it.isNotBlank() && it.length <= max) { "$label is required and must be at most $max characters." } }

    private fun optional(value: String, max: Int, label: String): String? =
        value.trim().also { require(it.length <= max) { "$label is too long." } }.ifBlank { null }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS products (
            id TEXT NOT NULL, organizationId TEXT NOT NULL, skuCode TEXT, skuKey TEXT,
            name TEXT NOT NULL, description TEXT, specification TEXT, material TEXT, unit TEXT,
            status TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_organizationId ON products (organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_status ON products (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_updatedAt ON products (updatedAt)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_organizationId_skuKey ON products (organizationId, skuKey)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS product_evidence (
            id TEXT NOT NULL, productId TEXT NOT NULL, sourceId TEXT NOT NULL, linkedAt INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(sourceId) REFERENCES organization_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_product_evidence_productId ON product_evidence (productId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_product_evidence_sourceId ON product_evidence (sourceId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_evidence_productId_sourceId ON product_evidence (productId, sourceId)")
    }
}
