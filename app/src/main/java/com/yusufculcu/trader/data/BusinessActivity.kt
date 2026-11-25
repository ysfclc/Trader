package com.yusufculcu.trader.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object BusinessActivityEntityType {
    const val ORGANIZATION = "ORGANIZATION"
    const val CONTACT = "CONTACT"
    const val CLAIM = "CLAIM"
    const val BUYER_REQUIREMENT = "BUYER_REQUIREMENT"
    const val SUPPLIER_OFFER = "SUPPLIER_OFFER"
    const val SOURCING_REVIEW = "SOURCING_REVIEW"
    val ALL = setOf(ORGANIZATION, CONTACT, CLAIM, BUYER_REQUIREMENT, SUPPLIER_OFFER, SOURCING_REVIEW)
}

object BusinessActivityAction {
    const val CREATED = "CREATED"
    const val UPDATED = "UPDATED"
    const val STATUS_CHANGED = "STATUS_CHANGED"
    const val SOURCE_ADDED = "SOURCE_ADDED"
    const val EVIDENCE_LINKED = "EVIDENCE_LINKED"
    const val CLAIM_REVIEWED = "CLAIM_REVIEWED"
    const val LINE_ADDED = "LINE_ADDED"
    const val DECISION_RECORDED = "DECISION_RECORDED"
    const val DELETED = "DELETED"
    val ALL = setOf(CREATED, UPDATED, STATUS_CHANGED, SOURCE_ADDED, EVIDENCE_LINKED, CLAIM_REVIEWED, LINE_ADDED, DECISION_RECORDED, DELETED)
}

/**
 * Immutable, non-secret business timeline snapshot. It deliberately has no foreign key:
 * deleting a company must not erase the fact that an operator previously changed a record.
 * The DAO exposes inserts and restore replacement only; normal product flows never edit events.
 */
@Entity(
    tableName = "business_activity_events",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["organizationId"]),
        Index(value = ["entityType", "entityId"])
    ]
)
data class BusinessActivityEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val organizationName: String,
    val entityType: String,
    val entityId: String,
    val entityLabel: String,
    val eventType: String,
    val fromStatus: String?,
    val toStatus: String?,
    val summary: String,
    val details: String,
    val occurredAt: Long
)

@Dao
interface BusinessActivityDao {
    @Query("SELECT * FROM business_activity_events ORDER BY occurredAt DESC, id DESC LIMIT 250")
    fun observeRecent(): Flow<List<BusinessActivityEntity>>

    @Query("SELECT * FROM business_activity_events ORDER BY occurredAt, id")
    suspend fun getAll(): List<BusinessActivityEntity>
    @Query("SELECT MAX(occurredAt) FROM business_activity_events")
    suspend fun latestOccurredAt(): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: BusinessActivityEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<BusinessActivityEntity>)

    @Query("DELETE FROM business_activity_events")
    suspend fun deleteAll()
}

internal suspend fun TraderDatabase.appendBusinessActivity(
    organizationId: String,
    organizationName: String,
    entityType: String,
    entityId: String,
    entityLabel: String,
    eventType: String,
    summary: String,
    fromStatus: String? = null,
    toStatus: String? = null,
    details: String = "",
    occurredAt: Long = System.currentTimeMillis()
): BusinessActivityEntity {
    val cleanOrganizationName = organizationName.trim()
    val cleanEntityLabel = entityLabel.trim()
    val cleanSummary = summary.trim()
    val cleanDetails = details.trim()
    require(organizationId.isNotBlank() && organizationId.length <= 128) { "Activity company reference is invalid." }
    require(cleanOrganizationName.isNotBlank() && cleanOrganizationName.length <= 200) { "Activity company name is invalid." }
    require(entityType in BusinessActivityEntityType.ALL) { "Activity entity type is unsupported." }
    require(entityId.isNotBlank() && entityId.length <= 128) { "Activity entity reference is invalid." }
    require(cleanEntityLabel.isNotBlank() && cleanEntityLabel.length <= 300) { "Activity entity label is invalid." }
    require(eventType in BusinessActivityAction.ALL) { "Activity action is unsupported." }
    require(cleanSummary.isNotBlank() && cleanSummary.length <= 200) { "Activity summary is invalid." }
    require(cleanDetails.length <= 2_000) { "Activity details are too long." }
    require(fromStatus == null || fromStatus.length <= 80) { "Previous status is too long." }
    require(toStatus == null || toStatus.length <= 80) { "New status is too long." }
    require(occurredAt >= 0) { "Activity timestamp is invalid." }
    val nextTimestamp = maxOf(occurredAt, (businessActivityDao().latestOccurredAt() ?: -1L) + 1L)
    return BusinessActivityEntity(
        id = UUID.randomUUID().toString(),
        organizationId = organizationId,
        organizationName = cleanOrganizationName,
        entityType = entityType,
        entityId = entityId,
        entityLabel = cleanEntityLabel,
        eventType = eventType,
        fromStatus = fromStatus,
        toStatus = toStatus,
        summary = cleanSummary,
        details = cleanDetails,
        occurredAt = nextTimestamp
    ).also { businessActivityDao().insert(it) }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS business_activity_events (
                id TEXT NOT NULL, organizationId TEXT NOT NULL, organizationName TEXT NOT NULL,
                entityType TEXT NOT NULL, entityId TEXT NOT NULL, entityLabel TEXT NOT NULL,
                eventType TEXT NOT NULL, fromStatus TEXT, toStatus TEXT, summary TEXT NOT NULL,
                details TEXT NOT NULL, occurredAt INTEGER NOT NULL, PRIMARY KEY(id)
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_activity_events_occurredAt ON business_activity_events (occurredAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_activity_events_organizationId ON business_activity_events (organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_activity_events_entityType_entityId ON business_activity_events (entityType, entityId)")
    }
}
