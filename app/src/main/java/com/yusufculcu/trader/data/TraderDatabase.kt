package com.yusufculcu.trader.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "problem_cases", indices = [Index(value = ["updatedAt"]), Index(value = ["status"]), Index(value = ["archivedAt"])])
data class ProblemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val desiredOutcome: String,
    val constraintsText: String,
    val category: String,
    val status: String,
    val inputKind: String,
    val originalContent: String,
    val sourceUri: String?,
    val outcomeNote: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null
)

@Entity(
    tableName = "plan_steps",
    foreignKeys = [ForeignKey(entity = ProblemEntity::class, parentColumns = ["id"], childColumns = ["problemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["problemId"]), Index(value = ["problemId", "position"], unique = true)]
)
data class PlanStepEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val title: String,
    val rationale: String,
    val requiredCapability: String,
    val expectedResult: String,
    val status: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "activity_events",
    foreignKeys = [ForeignKey(entity = ProblemEntity::class, parentColumns = ["id"], childColumns = ["problemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["problemId"]), Index(value = ["occurredAt"])]
)
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val eventType: String,
    val summary: String,
    val details: String,
    val occurredAt: Long
)


@Entity(
    tableName = "evidence_records",
    foreignKeys = [ForeignKey(entity = ProblemEntity::class, parentColumns = ["id"], childColumns = ["problemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["problemId"]), Index(value = ["capturedAt"])]
)
data class EvidenceEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val title: String,
    val content: String,
    val sourceUri: String?,
    val sourceKind: String,
    val verificationState: String,
    val capturedAt: Long
)

@Entity(
    tableName = "opportunity_records",
    foreignKeys = [ForeignKey(entity = ProblemEntity::class, parentColumns = ["id"], childColumns = ["problemId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index(value = ["problemId"], unique = true),
        Index(value = ["stage"]),
        Index(value = ["followUpAt"])
    ]
)
data class OpportunityEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val lane: String,
    val organizationName: String,
    val opportunityName: String,
    val contactName: String,
    val contactRoute: String,
    val fitNotes: String,
    val stage: String,
    val nextAction: String,
    val followUpAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "workflow_runs",
    foreignKeys = [ForeignKey(entity = ProblemEntity::class, parentColumns = ["id"], childColumns = ["problemId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index(value = ["problemId"]),
        Index(value = ["status"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class WorkflowRunEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val title: String,
    val status: String,
    val idempotencyKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val lastError: String?
)

@Entity(
    tableName = "workflow_steps",
    foreignKeys = [ForeignKey(entity = WorkflowRunEntity::class, parentColumns = ["id"], childColumns = ["workflowRunId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index(value = ["workflowRunId"]),
        Index(value = ["workflowRunId", "position"], unique = true)
    ]
)
data class WorkflowStepEntity(
    @PrimaryKey val id: String,
    val workflowRunId: String,
    val title: String,
    val capabilityId: String,
    val inputJson: String,
    val expectedPostcondition: String,
    val status: String,
    val position: Int,
    val retrySafe: Boolean,
    val requiresApproval: Boolean,
    val payloadHash: String,
    val approvedPayloadHash: String?,
    val approvedAt: Long?,
    val attemptCount: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?
)

@Entity(
    tableName = "workflow_events",
    foreignKeys = [ForeignKey(entity = WorkflowRunEntity::class, parentColumns = ["id"], childColumns = ["workflowRunId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["workflowRunId"]), Index(value = ["occurredAt"])]
)
data class WorkflowEventEntity(
    @PrimaryKey val id: String,
    val workflowRunId: String,
    val stepId: String?,
    val eventType: String,
    val fromStatus: String?,
    val toStatus: String?,
    val summary: String,
    val details: String,
    val occurredAt: Long
)

@Entity(tableName = "proof_assets", indices = [Index(value = ["updatedAt"])])
data class ProofAssetEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int,
    val kind: String,
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "discovery_strategies",
    foreignKeys = [ForeignKey(entity = ProofAssetEntity::class, parentColumns = ["id"], childColumns = ["proofAssetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["proofAssetId"]), Index(value = ["updatedAt"])]
)
data class DiscoveryStrategyEntity(
    @PrimaryKey val id: String,
    val proofAssetId: String,
    val schemaVersion: Int,
    val payloadJson: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problem_cases")
    suspend fun getAll(): List<ProblemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ProblemEntity>)

    @Query("DELETE FROM problem_cases")
    suspend fun deleteAll()

    @Query("SELECT * FROM problem_cases WHERE archivedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProblemEntity>>
    @Query("SELECT * FROM problem_cases WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<ProblemEntity>>
    @Query("SELECT * FROM problem_cases WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ProblemEntity?>
    @Query("SELECT * FROM problem_cases WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProblemEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(problem: ProblemEntity)
    @Query("UPDATE problem_cases SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)
    @Query("UPDATE problem_cases SET title = :title, description = :description, desiredOutcome = :desiredOutcome, constraintsText = :constraintsText, updatedAt = :updatedAt WHERE id = :id AND archivedAt IS NULL")
    suspend fun updateDetails(id: String, title: String, description: String, desiredOutcome: String, constraintsText: String, updatedAt: Long)
    @Query("UPDATE problem_cases SET archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAt: Long?, updatedAt: Long)
    @Query("UPDATE problem_cases SET status = :status, outcomeNote = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, note: String, updatedAt: Long)
    @Query("SELECT COUNT(*) FROM problem_cases WHERE archivedAt IS NULL")
    fun observeCount(): Flow<Int>
}

@Dao
interface PlanStepDao {
    @Query("SELECT * FROM plan_steps")
    suspend fun getAll(): List<PlanStepEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<PlanStepEntity>)

    @Query("DELETE FROM plan_steps")
    suspend fun deleteAll()

    @Query("SELECT * FROM plan_steps WHERE problemId = :problemId ORDER BY position ASC")
    fun observeForProblem(problemId: String): Flow<List<PlanStepEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: PlanStepEntity)
    @Query("UPDATE plan_steps SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
    @Update
    suspend fun update(step: PlanStepEntity)
    @Query("DELETE FROM plan_steps WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM plan_steps WHERE problemId = :problemId")
    suspend fun nextPosition(problemId: String): Int
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_events")
    suspend fun getAll(): List<ActivityEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ActivityEventEntity>)

    @Query("DELETE FROM activity_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM activity_events ORDER BY occurredAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<ActivityEventEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ActivityEventEntity)
}


@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_records")
    suspend fun getAll(): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<EvidenceEntity>)

    @Query("DELETE FROM evidence_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM evidence_records WHERE problemId = :problemId ORDER BY capturedAt DESC")
    fun observeForProblem(problemId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): EvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: EvidenceEntity)

    @Query("UPDATE evidence_records SET verificationState = :state WHERE id = :id")
    suspend fun updateVerificationState(id: String, state: String)
}

@Dao
interface OpportunityDao {
    @Query("SELECT * FROM opportunity_records")
    suspend fun getAll(): List<OpportunityEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OpportunityEntity>)

    @Query("DELETE FROM opportunity_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM opportunity_records WHERE problemId = :problemId LIMIT 1")
    fun observeForProblem(problemId: String): Flow<OpportunityEntity?>

    @Query("SELECT * FROM opportunity_records ORDER BY CASE WHEN stage IN ('WON', 'LOST') THEN 1 ELSE 0 END, CASE WHEN followUpAt IS NULL THEN 1 ELSE 0 END, followUpAt ASC, updatedAt DESC")
    fun observeAll(): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunity_records WHERE problemId = :problemId LIMIT 1")
    suspend fun getByProblemId(problemId: String): OpportunityEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(opportunity: OpportunityEntity)

    @Update
    suspend fun update(opportunity: OpportunityEntity)
}

@Dao
interface WorkflowRunDao {
    @Query("SELECT * FROM workflow_runs")
    suspend fun getAll(): List<WorkflowRunEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<WorkflowRunEntity>)

    @Query("DELETE FROM workflow_runs")
    suspend fun deleteAll()

    @Query("SELECT * FROM workflow_runs WHERE problemId = :problemId ORDER BY updatedAt DESC")
    fun observeForProblem(problemId: String): Flow<List<WorkflowRunEntity>>

    @Query("SELECT * FROM workflow_runs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkflowRunEntity?

    @Query("SELECT * FROM workflow_runs WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): WorkflowRunEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(run: WorkflowRunEntity)

    @Update
    suspend fun update(run: WorkflowRunEntity)
}

@Dao
interface WorkflowStepDao {
    @Query("SELECT * FROM workflow_steps")
    suspend fun getAll(): List<WorkflowStepEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<WorkflowStepEntity>)

    @Query("DELETE FROM workflow_steps")
    suspend fun deleteAll()

    @Query("SELECT * FROM workflow_steps WHERE workflowRunId = :runId ORDER BY position ASC")
    fun observeForRun(runId: String): Flow<List<WorkflowStepEntity>>

    @Query("SELECT * FROM workflow_steps WHERE workflowRunId = :runId ORDER BY position ASC")
    suspend fun getForRun(runId: String): List<WorkflowStepEntity>

    @Query("SELECT * FROM workflow_steps WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkflowStepEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(step: WorkflowStepEntity)

    @Update
    suspend fun update(step: WorkflowStepEntity)
}

@Dao
interface WorkflowEventDao {
    @Query("SELECT * FROM workflow_events")
    suspend fun getAll(): List<WorkflowEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<WorkflowEventEntity>)

    @Query("DELETE FROM workflow_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM workflow_events WHERE workflowRunId = :runId ORDER BY occurredAt ASC")
    fun observeForRun(runId: String): Flow<List<WorkflowEventEntity>>

    @Query("SELECT * FROM workflow_events WHERE workflowRunId = :runId ORDER BY occurredAt ASC")
    suspend fun getForRun(runId: String): List<WorkflowEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: WorkflowEventEntity)
}

@Dao
interface ProofAssetDao {
    @Query("SELECT * FROM proof_assets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProofAssetEntity>>
    @Query("SELECT * FROM proof_assets")
    suspend fun getAll(): List<ProofAssetEntity>
    @Query("SELECT * FROM proof_assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProofAssetEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: ProofAssetEntity)
    @Update
    suspend fun update(asset: ProofAssetEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ProofAssetEntity>)
    @Query("DELETE FROM proof_assets")
    suspend fun deleteAll()
}

@Dao
interface DiscoveryStrategyDao {
    @Query("SELECT * FROM discovery_strategies ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DiscoveryStrategyEntity>>
    @Query("SELECT * FROM discovery_strategies")
    suspend fun getAll(): List<DiscoveryStrategyEntity>
    @Query("SELECT * FROM discovery_strategies WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiscoveryStrategyEntity?
    @Query("SELECT * FROM discovery_strategies WHERE proofAssetId = :proofAssetId ORDER BY updatedAt DESC")
    fun observeForProofAsset(proofAssetId: String): Flow<List<DiscoveryStrategyEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(strategy: DiscoveryStrategyEntity)
    @Update
    suspend fun update(strategy: DiscoveryStrategyEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<DiscoveryStrategyEntity>)
    @Query("DELETE FROM discovery_strategies")
    suspend fun deleteAll()
}


@Entity(
    tableName = "discovery_runs",
    foreignKeys = [ForeignKey(entity = DiscoveryStrategyEntity::class, parentColumns = ["id"], childColumns = ["strategyId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["strategyId"]), Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class DiscoveryRunEntity(
    @PrimaryKey val id: String, val strategyId: String?, val objective: String, val status: String,
    val createdAt: Long, val startedAt: Long?, val finishedAt: Long?,
    val cancellationRequestedAt: Long?, val lastError: String?
)

@Entity(
    tableName = "source_runs",
    foreignKeys = [ForeignKey(entity = DiscoveryRunEntity::class, parentColumns = ["id"], childColumns = ["discoveryRunId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["discoveryRunId"]), Index(value = ["sourceId"]), Index(value = ["status"])]
)
data class SourceRunEntity(
    @PrimaryKey val id: String, val discoveryRunId: String, val sourceId: String, val adapterId: String,
    val status: String, val startedAt: Long?, val finishedAt: Long?, val resultCount: Int,
    val errorCode: String?, val errorMessage: String?
)

@Entity(tableName = "discovery_documents", foreignKeys = [ForeignKey(entity = DiscoveryRunEntity::class, parentColumns = ["id"], childColumns = ["discoveryRunId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["discoveryRunId"]), Index(value = ["sourceId"]), Index(value = ["retrievedAt"])])
data class DiscoveryDocumentEntity(
    @PrimaryKey val id: String, val discoveryRunId: String, val sourceId: String, val sourceName: String,
    val nativeId: String, val title: String, val url: String, val summary: String,
    val publishedAt: Long?, val retrievedAt: Long
)

@Entity(tableName = "organizations", indices = [Index(value = ["updatedAt"]), Index(value = ["domain"])])
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val websiteUrl: String?,
    val domain: String?,
    val country: String?,
    val city: String?,
    val industry: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "organization_sources",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["organizationId"]), Index(value = ["organizationId", "url"], unique = true), Index(value = ["capturedAt"])]
)
data class OrganizationSourceEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val sourceKind: String,
    val title: String,
    val url: String,
    val excerpt: String,
    val capturedAt: Long
)

@Entity(
    tableName = "organization_claims",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = OrganizationSourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["organizationId"]), Index(value = ["sourceId"]), Index(value = ["reviewState"])]
)
data class OrganizationClaimEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val sourceId: String,
    val field: String,
    val statement: String,
    val reviewState: String,
    val reviewNote: String?,
    val reviewedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "organization_people", foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["organizationId"]), Index(value = ["email"])])
data class OrganizationPersonEntity(@PrimaryKey val id: String, val organizationId: String, val fullName: String, val role: String?, val email: String?, val phone: String?, val profileUrl: String?, val notes: String?, val createdAt: Long, val updatedAt: Long)

@Dao
interface OrganizationPersonDao {
 @Query("SELECT * FROM organization_people WHERE organizationId = :organizationId ORDER BY fullName COLLATE NOCASE ASC, createdAt ASC") fun observeForOrganization(organizationId: String): Flow<List<OrganizationPersonEntity>>
 @Query("SELECT * FROM organization_people") suspend fun getAll(): List<OrganizationPersonEntity>
 @Query("SELECT * FROM organization_people WHERE id = :id LIMIT 1") suspend fun getById(id: String): OrganizationPersonEntity?
 @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(person: OrganizationPersonEntity)
 @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(items: List<OrganizationPersonEntity>)
 @Update suspend fun update(person: OrganizationPersonEntity)
 @Query("DELETE FROM organization_people WHERE id = :id") suspend fun deleteById(id: String): Int
 @Query("DELETE FROM organization_people") suspend fun deleteAll()
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations ORDER BY updatedAt DESC, canonicalName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<OrganizationEntity>>
    @Query("SELECT * FROM organizations WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<OrganizationEntity?>
    @Query("SELECT * FROM organizations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrganizationEntity?
    @Query("SELECT * FROM organizations WHERE domain = :domain COLLATE NOCASE ORDER BY updatedAt DESC")
    suspend fun findByDomain(domain: String): List<OrganizationEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(organization: OrganizationEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrganizationEntity>)
    @Update
    suspend fun update(organization: OrganizationEntity)
    @Query("UPDATE organizations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)
    @Query("DELETE FROM organizations WHERE id = :id")
    suspend fun deleteById(id: String): Int
    @Query("DELETE FROM organizations")
    suspend fun deleteAll()
    @Query("SELECT * FROM organizations")
    suspend fun getAll(): List<OrganizationEntity>
}

@Dao
interface OrganizationSourceDao {
    @Query("SELECT * FROM organization_sources WHERE organizationId = :organizationId ORDER BY capturedAt DESC, id ASC")
    fun observeForOrganization(organizationId: String): Flow<List<OrganizationSourceEntity>>
    @Query("SELECT * FROM organization_sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrganizationSourceEntity?
    @Query("SELECT * FROM organization_sources WHERE organizationId = :organizationId AND url = :url LIMIT 1")
    suspend fun findByOrganizationAndUrl(organizationId: String, url: String): OrganizationSourceEntity?
    @Query("SELECT * FROM organization_sources")
    suspend fun getAll(): List<OrganizationSourceEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(source: OrganizationSourceEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrganizationSourceEntity>)
    @Query("DELETE FROM organization_sources")
    suspend fun deleteAll()
}

@Dao
interface OrganizationClaimDao {
    @Query("SELECT * FROM organization_claims WHERE organizationId = :organizationId ORDER BY createdAt DESC, id ASC")
    fun observeForOrganization(organizationId: String): Flow<List<OrganizationClaimEntity>>
    @Query("SELECT * FROM organization_claims WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrganizationClaimEntity?
    @Query("SELECT * FROM organization_claims")
    suspend fun getAll(): List<OrganizationClaimEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(claim: OrganizationClaimEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrganizationClaimEntity>)
    @Update
    suspend fun update(claim: OrganizationClaimEntity)
    @Query("DELETE FROM organization_claims")
    suspend fun deleteAll()
}

@Dao
interface DiscoveryDocumentDao {
    @Query("SELECT * FROM discovery_documents WHERE discoveryRunId = :runId ORDER BY retrievedAt DESC, id ASC")
    fun observeForRun(runId: String): Flow<List<DiscoveryDocumentEntity>>
    @Query("SELECT * FROM discovery_documents")
    suspend fun getAll(): List<DiscoveryDocumentEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: DiscoveryDocumentEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(documents: List<DiscoveryDocumentEntity>)
    @Query("DELETE FROM discovery_documents")
    suspend fun deleteAll()
}

@Dao
interface DiscoveryRunDao {
    @Query("SELECT * FROM discovery_runs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiscoveryRunEntity>>
    @Query("SELECT * FROM discovery_runs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiscoveryRunEntity?
    @Query("SELECT * FROM discovery_runs WHERE status = 'RUNNING'")
    suspend fun getRunning(): List<DiscoveryRunEntity>
    @Query("SELECT * FROM discovery_runs")
    suspend fun getAll(): List<DiscoveryRunEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(run: DiscoveryRunEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(runs: List<DiscoveryRunEntity>)
    @Query("DELETE FROM discovery_runs")
    suspend fun deleteAll()
    @Update
    suspend fun update(run: DiscoveryRunEntity)
}

@Dao
interface SourceRunDao {
    @Query("SELECT * FROM source_runs WHERE discoveryRunId = :runId ORDER BY rowid ASC")
    fun observeForRun(runId: String): Flow<List<SourceRunEntity>>
    @Query("SELECT * FROM source_runs WHERE discoveryRunId = :runId ORDER BY rowid ASC")
    suspend fun getForRun(runId: String): List<SourceRunEntity>
    @Query("SELECT * FROM source_runs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SourceRunEntity?
    @Query("SELECT * FROM source_runs")
    suspend fun getAll(): List<SourceRunEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sourceRun: SourceRunEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(sourceRuns: List<SourceRunEntity>)
    @Query("DELETE FROM source_runs")
    suspend fun deleteAll()
    @Update
    suspend fun update(sourceRun: SourceRunEntity)
    @Query("UPDATE source_runs SET status = 'CANCELLED', finishedAt = :at WHERE discoveryRunId = :runId AND status IN ('PENDING', 'RUNNING')")
    suspend fun cancelPending(runId: String, at: Long)
    @Query("UPDATE source_runs SET status = 'FAILED', finishedAt = :at, errorCode = 'INTERRUPTED', errorMessage = 'Process stopped before source execution completed.' WHERE discoveryRunId = :runId AND status = 'RUNNING'")
    suspend fun markInterrupted(runId: String, at: Long)
}


@Entity(
    tableName = "fit_assessments",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organizationId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProofAssetEntity::class, parentColumns = ["id"], childColumns = ["proofAssetId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["organizationId"]),
        Index(value = ["proofAssetId"]),
        Index(value = ["organizationId", "proofAssetId"], unique = true),
        Index(value = ["updatedAt"])
    ]
)
data class FitAssessmentEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val proofAssetId: String,
    val computedOutcome: String,
    val computedSnapshotJson: String?,
    val evaluatedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "fit_assessment_evidence",
    foreignKeys = [
        ForeignKey(entity = FitAssessmentEntity::class, parentColumns = ["id"], childColumns = ["assessmentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = OrganizationClaimEntity::class, parentColumns = ["id"], childColumns = ["organizationClaimId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["assessmentId"]),
        Index(value = ["organizationClaimId"]),
        Index(value = ["assessmentId", "organizationClaimId", "proofClaimId"], unique = true)
    ]
)
data class FitAssessmentEvidenceEntity(
    @PrimaryKey val id: String,
    val assessmentId: String,
    val organizationClaimId: String,
    val proofClaimId: String,
    val relationship: String,
    val rationale: String,
    val createdAt: Long
)

@Entity(
    tableName = "fit_assessment_overrides",
    foreignKeys = [
        ForeignKey(entity = FitAssessmentEntity::class, parentColumns = ["id"], childColumns = ["assessmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["assessmentId"]), Index(value = ["assessmentId", "reviewedAt"])]
)
data class FitAssessmentOverrideEntity(
    @PrimaryKey val id: String,
    val assessmentId: String,
    val decision: String,
    val rationale: String,
    val reviewerLabel: String?,
    val reviewedAt: Long
)

@Dao
interface FitAssessmentDao {
    @Query("SELECT * FROM fit_assessments WHERE organizationId = :organizationId ORDER BY updatedAt DESC, id ASC")
    fun observeForOrganization(organizationId: String): Flow<List<FitAssessmentEntity>>

    @Query("SELECT * FROM fit_assessments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FitAssessmentEntity?

    @Query("SELECT * FROM fit_assessments WHERE organizationId = :organizationId AND proofAssetId = :proofAssetId LIMIT 1")
    suspend fun findForOrganizationAndProofAsset(organizationId: String, proofAssetId: String): FitAssessmentEntity?

    @Query("SELECT * FROM fit_assessments")
    suspend fun getAll(): List<FitAssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assessment: FitAssessmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<FitAssessmentEntity>)

    @Update
    suspend fun update(assessment: FitAssessmentEntity)

    @Query("DELETE FROM fit_assessments")
    suspend fun deleteAll()
}

@Dao
interface FitAssessmentEvidenceDao {
    @Query("SELECT * FROM fit_assessment_evidence WHERE assessmentId = :assessmentId ORDER BY createdAt ASC, id ASC")
    fun observeForAssessment(assessmentId: String): Flow<List<FitAssessmentEvidenceEntity>>

    @Query("SELECT * FROM fit_assessment_evidence")
    suspend fun getAll()

    @Query("SELECT * FROM fit_assessment_evidence WHERE assessmentId = :assessmentId ORDER BY createdAt ASC, id ASC")
    suspend fun getForAssessment(assessmentId: String): List<FitAssessmentEvidenceEntity>: List<FitAssessmentEvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: FitAssessmentEvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<FitAssessmentEvidenceEntity>)

    @Query("DELETE FROM fit_assessment_evidence WHERE assessmentId = :assessmentId")
    suspend fun deleteForAssessment(assessmentId: String): Int

    @Query("DELETE FROM fit_assessment_evidence")
    suspend fun deleteAll()
}

@Dao
interface FitAssessmentOverrideDao {
    @Query("SELECT * FROM fit_assessment_overrides WHERE assessmentId = :assessmentId ORDER BY reviewedAt DESC, id DESC")
    fun observeHistory(assessmentId: String): Flow<List<FitAssessmentOverrideEntity>>

    @Query("SELECT * FROM fit_assessment_overrides WHERE assessmentId = :assessmentId ORDER BY reviewedAt DESC, id DESC LIMIT 1")
    suspend fun latestForAssessment(assessmentId: String): FitAssessmentOverrideEntity?

    @Query("SELECT * FROM fit_assessment_overrides")
    suspend fun getAll(): List<FitAssessmentOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun append(override: FitAssessmentOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<FitAssessmentOverrideEntity>)

    @Query("DELETE FROM fit_assessment_overrides")
    suspend fun deleteAll()
}

@Database(
    entities = [
        ProblemEntity::class,
        PlanStepEntity::class,
        ActivityEventEntity::class,
        EvidenceEntity::class,
        OpportunityEntity::class,
        WorkflowRunEntity::class,
        WorkflowStepEntity::class,
        WorkflowEventEntity::class,
        ProofAssetEntity::class,
        DiscoveryStrategyEntity::class,
        DiscoveryRunEntity::class,
        SourceRunEntity::class,
        DiscoveryDocumentEntity::class,
        OrganizationEntity::class,
        OrganizationSourceEntity::class,
        OrganizationClaimEntity::class,
        OrganizationPersonEntity::class,
        BuyerRequirementEntity::class,
        BuyerRequirementLineItemEntity::class,
        BuyerRequirementEvidenceEntity::class,
        SupplierOfferEntity::class,
        SupplierOfferLineItemEntity::class,
        SupplierOfferEvidenceEntity::class,
        SourcingReviewEntity::class,
        ProductEntity::class,
        ProductEvidenceEntity::class,
        BusinessActivityEntity::class,
        FitAssessmentEntity::class,
        FitAssessmentEvidenceEntity::class,
        FitAssessmentOverrideEntity::class
    ],
    version = 16,
    exportSchema = true
)
abstract class TraderDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun planStepDao(): PlanStepDao
    abstract fun activityDao(): ActivityDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun opportunityDao(): OpportunityDao
    abstract fun workflowRunDao(): WorkflowRunDao
    abstract fun workflowStepDao(): WorkflowStepDao
    abstract fun workflowEventDao(): WorkflowEventDao
    abstract fun proofAssetDao(): ProofAssetDao
    abstract fun discoveryStrategyDao(): DiscoveryStrategyDao
    abstract fun discoveryRunDao(): DiscoveryRunDao
    abstract fun sourceRunDao(): SourceRunDao
    abstract fun discoveryDocumentDao(): DiscoveryDocumentDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun organizationSourceDao(): OrganizationSourceDao
    abstract fun organizationClaimDao(): OrganizationClaimDao
    abstract fun organizationPersonDao(): OrganizationPersonDao
    abstract fun buyerRequirementDao(): BuyerRequirementDao
    abstract fun buyerRequirementLineItemDao(): BuyerRequirementLineItemDao
    abstract fun buyerRequirementEvidenceDao(): BuyerRequirementEvidenceDao
    abstract fun supplierOfferDao(): SupplierOfferDao
    abstract fun supplierOfferLineItemDao(): SupplierOfferLineItemDao
    abstract fun supplierOfferEvidenceDao(): SupplierOfferEvidenceDao
    abstract fun sourcingReviewDao(): SourcingReviewDao
    abstract fun productDao(): ProductDao
    abstract fun productEvidenceDao(): ProductEvidenceDao
    abstract fun businessActivityDao(): BusinessActivityDao
    abstract fun fitAssessmentDao(): FitAssessmentDao
    abstract fun fitAssessmentEvidenceDao(): FitAssessmentEvidenceDao
    abstract fun fitAssessmentOverrideDao(): FitAssessmentOverrideDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workflow_runs` (
                `id` TEXT NOT NULL,
                `problemId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `idempotencyKey` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `startedAt` INTEGER,
                `finishedAt` INTEGER,
                `lastError` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`problemId`) REFERENCES `problem_cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workflow_runs_problemId` ON `workflow_runs` (`problemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workflow_runs_status` ON `workflow_runs` (`status`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workflow_runs_idempotencyKey` ON `workflow_runs` (`idempotencyKey`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workflow_steps` (
                `id` TEXT NOT NULL,
                `workflowRunId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `capabilityId` TEXT NOT NULL,
                `inputJson` TEXT NOT NULL,
                `expectedPostcondition` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `retrySafe` INTEGER NOT NULL,
                `requiresApproval` INTEGER NOT NULL,
                `payloadHash` TEXT NOT NULL,
                `approvedPayloadHash` TEXT,
                `approvedAt` INTEGER,
                `attemptCount` INTEGER NOT NULL,
                `lastError` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `startedAt` INTEGER,
                `finishedAt` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workflowRunId`) REFERENCES `workflow_runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workflow_steps_workflowRunId` ON `workflow_steps` (`workflowRunId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workflow_steps_workflowRunId_position` ON `workflow_steps` (`workflowRunId`, `position`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workflow_events` (
                `id` TEXT NOT NULL,
                `workflowRunId` TEXT NOT NULL,
                `stepId` TEXT,
                `eventType` TEXT NOT NULL,
                `fromStatus` TEXT,
                `toStatus` TEXT,
                `summary` TEXT NOT NULL,
                `details` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workflowRunId`) REFERENCES `workflow_runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workflow_events_workflowRunId` ON `workflow_events` (`workflowRunId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workflow_events_occurredAt` ON `workflow_events` (`occurredAt`)")
    }
}


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE problem_cases ADD COLUMN archivedAt INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_problem_cases_archivedAt ON problem_cases (archivedAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS evidence_records (
                id TEXT NOT NULL,
                problemId TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                sourceUri TEXT,
                sourceKind TEXT NOT NULL,
                verificationState TEXT NOT NULL,
                capturedAt INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(problemId) REFERENCES problem_cases(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_records_problemId ON evidence_records (problemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidence_records_capturedAt ON evidence_records (capturedAt)")
    }
}


val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS opportunity_records (
                id TEXT NOT NULL,
                problemId TEXT NOT NULL,
                lane TEXT NOT NULL,
                organizationName TEXT NOT NULL,
                opportunityName TEXT NOT NULL,
                contactName TEXT NOT NULL,
                contactRoute TEXT NOT NULL,
                fitNotes TEXT NOT NULL,
                stage TEXT NOT NULL,
                nextAction TEXT NOT NULL,
                followUpAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(problemId) REFERENCES problem_cases(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_opportunity_records_problemId ON opportunity_records (problemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opportunity_records_stage ON opportunity_records (stage)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_opportunity_records_followUpAt ON opportunity_records (followUpAt)")
    }
}


/** Adds durable, versioned proof assets and strategies without rewriting existing records. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS proof_assets (
                id TEXT NOT NULL,
                schemaVersion INTEGER NOT NULL,
                kind TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_proof_assets_updatedAt ON proof_assets (updatedAt)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS discovery_strategies (
                id TEXT NOT NULL,
                proofAssetId TEXT NOT NULL,
                schemaVersion INTEGER NOT NULL,
                payloadJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(proofAssetId) REFERENCES proof_assets(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_strategies_proofAssetId ON discovery_strategies (proofAssetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_strategies_updatedAt ON discovery_strategies (updatedAt)")
    }
}


val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS discovery_runs (
                id TEXT NOT NULL, strategyId TEXT, objective TEXT NOT NULL, status TEXT NOT NULL,
                createdAt INTEGER NOT NULL, startedAt INTEGER, finishedAt INTEGER,
                cancellationRequestedAt INTEGER, lastError TEXT, PRIMARY KEY(id),
                FOREIGN KEY(strategyId) REFERENCES discovery_strategies(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_runs_strategyId ON discovery_runs (strategyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_runs_status ON discovery_runs (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_runs_createdAt ON discovery_runs (createdAt)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS source_runs (
                id TEXT NOT NULL, discoveryRunId TEXT NOT NULL, sourceId TEXT NOT NULL, adapterId TEXT NOT NULL,
                status TEXT NOT NULL, startedAt INTEGER, finishedAt INTEGER, resultCount INTEGER NOT NULL,
                errorCode TEXT, errorMessage TEXT, PRIMARY KEY(id),
                FOREIGN KEY(discoveryRunId) REFERENCES discovery_runs(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_runs_discoveryRunId ON source_runs (discoveryRunId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_runs_sourceId ON source_runs (sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_source_runs_status ON source_runs (status)")
    }
}


val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS discovery_documents (id TEXT NOT NULL, discoveryRunId TEXT NOT NULL, sourceId TEXT NOT NULL, sourceName TEXT NOT NULL, nativeId TEXT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, summary TEXT NOT NULL, publishedAt INTEGER, retrievedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(discoveryRunId) REFERENCES discovery_runs(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_documents_discoveryRunId ON discovery_documents (discoveryRunId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_documents_sourceId ON discovery_documents (sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_documents_retrievedAt ON discovery_documents (retrievedAt)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS organizations (
            id TEXT NOT NULL, canonicalName TEXT NOT NULL, websiteUrl TEXT, domain TEXT,
            country TEXT, city TEXT, industry TEXT, notes TEXT, createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL, PRIMARY KEY(id)
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organizations_updatedAt ON organizations (updatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organizations_domain ON organizations (domain)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS organization_sources (
            id TEXT NOT NULL, organizationId TEXT NOT NULL, sourceKind TEXT NOT NULL,
            title TEXT NOT NULL, url TEXT NOT NULL, excerpt TEXT NOT NULL, capturedAt INTEGER NOT NULL,
            PRIMARY KEY(id), FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_sources_organizationId ON organization_sources (organizationId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_organization_sources_organizationId_url ON organization_sources (organizationId, url)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_sources_capturedAt ON organization_sources (capturedAt)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS organization_claims (
            id TEXT NOT NULL, organizationId TEXT NOT NULL, sourceId TEXT NOT NULL,
            field TEXT NOT NULL, statement TEXT NOT NULL, reviewState TEXT NOT NULL,
            reviewNote TEXT, reviewedAt INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(sourceId) REFERENCES organization_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_claims_organizationId ON organization_claims (organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_claims_sourceId ON organization_claims (sourceId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_claims_reviewState ON organization_claims (reviewState)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
 override fun migrate(db: SupportSQLiteDatabase) {
  db.execSQL("""CREATE TABLE IF NOT EXISTS organization_people (id TEXT NOT NULL, organizationId TEXT NOT NULL, fullName TEXT NOT NULL, role TEXT, email TEXT, phone TEXT, profileUrl TEXT, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_people_organizationId ON organization_people (organizationId)")
  db.execSQL("CREATE INDEX IF NOT EXISTS index_organization_people_email ON organization_people (email)")
 }
}


/** Adds supplier offers and reviewable buyer-to-supplier shortlist decisions. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS supplier_offers (id TEXT NOT NULL, organizationId TEXT NOT NULL, title TEXT NOT NULL, description TEXT, status TEXT NOT NULL, currency TEXT, originCountry TEXT, originCity TEXT, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offers_organizationId ON supplier_offers (organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offers_status ON supplier_offers (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offers_updatedAt ON supplier_offers (updatedAt)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS supplier_offer_items (id TEXT NOT NULL, offerId TEXT NOT NULL, productName TEXT NOT NULL, specification TEXT, material TEXT, quantityAvailable TEXT, quantityUnit TEXT, minimumOrderQuantity TEXT, unitPriceMinorUnits INTEGER, currency TEXT, leadTimeDays INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(offerId) REFERENCES supplier_offers(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offer_items_offerId ON supplier_offer_items (offerId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS supplier_offer_evidence (id TEXT NOT NULL, offerId TEXT NOT NULL, sourceId TEXT NOT NULL, linkedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(offerId) REFERENCES supplier_offers(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceId) REFERENCES organization_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offer_evidence_offerId ON supplier_offer_evidence (offerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_offer_evidence_sourceId ON supplier_offer_evidence (sourceId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_supplier_offer_evidence_offerId_sourceId ON supplier_offer_evidence (offerId, sourceId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS sourcing_reviews (id TEXT NOT NULL, buyerRequirementId TEXT NOT NULL, supplierOfferId TEXT NOT NULL, status TEXT NOT NULL, rationale TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(buyerRequirementId) REFERENCES buyer_requirements(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(supplierOfferId) REFERENCES supplier_offers(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sourcing_reviews_buyerRequirementId ON sourcing_reviews (buyerRequirementId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sourcing_reviews_supplierOfferId ON sourcing_reviews (supplierOfferId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sourcing_reviews_buyerRequirementId_supplierOfferId ON sourcing_reviews (buyerRequirementId, supplierOfferId)")
    }
}


/** Adds durable, separately reviewable company-to-proof fit assessments. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS fit_assessments (
            id TEXT NOT NULL, organizationId TEXT NOT NULL, proofAssetId TEXT NOT NULL,
            computedOutcome TEXT NOT NULL, evaluatedAt INTEGER NOT NULL,
            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id),
            FOREIGN KEY(organizationId) REFERENCES organizations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(proofAssetId) REFERENCES proof_assets(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessments_organizationId ON fit_assessments (organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessments_proofAssetId ON fit_assessments (proofAssetId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_fit_assessments_organizationId_proofAssetId ON fit_assessments (organizationId, proofAssetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessments_updatedAt ON fit_assessments (updatedAt)")

        db.execSQL("""CREATE TABLE IF NOT EXISTS fit_assessment_evidence (
            id TEXT NOT NULL, assessmentId TEXT NOT NULL, organizationClaimId TEXT NOT NULL,
            proofClaimId TEXT NOT NULL, relationship TEXT NOT NULL, rationale TEXT NOT NULL,
            createdAt INTEGER NOT NULL, PRIMARY KEY(id),
            FOREIGN KEY(assessmentId) REFERENCES fit_assessments(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(organizationClaimId) REFERENCES organization_claims(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessment_evidence_assessmentId ON fit_assessment_evidence (assessmentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessment_evidence_organizationClaimId ON fit_assessment_evidence (organizationClaimId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_fit_assessment_evidence_assessmentId_organizationClaimId_proofClaimId ON fit_assessment_evidence (assessmentId, organizationClaimId, proofClaimId)")

        db.execSQL("""CREATE TABLE IF NOT EXISTS fit_assessment_overrides (
            id TEXT NOT NULL, assessmentId TEXT NOT NULL, decision TEXT NOT NULL,
            rationale TEXT NOT NULL, reviewerLabel TEXT, reviewedAt INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(assessmentId) REFERENCES fit_assessments(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessment_overrides_assessmentId ON fit_assessment_overrides (assessmentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_fit_assessment_overrides_assessmentId_reviewedAt ON fit_assessment_overrides (assessmentId, reviewedAt)")
    }
}


/** Stores the deterministic computed result separately from append-only reviewer overrides. */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE fit_assessments ADD COLUMN computedSnapshotJson TEXT")
    }
}
