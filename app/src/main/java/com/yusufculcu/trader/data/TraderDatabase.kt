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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: EvidenceEntity)

    @Query("UPDATE evidence_records SET verificationState = :state WHERE id = :id")
    suspend fun updateVerificationState(id: String, state: String)
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

@Database(
    entities = [
        ProblemEntity::class,
        PlanStepEntity::class,
        ActivityEventEntity::class,
        EvidenceEntity::class,
        WorkflowRunEntity::class,
        WorkflowStepEntity::class,
        WorkflowEventEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TraderDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun planStepDao(): PlanStepDao
    abstract fun activityDao(): ActivityDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun workflowRunDao(): WorkflowRunDao
    abstract fun workflowStepDao(): WorkflowStepDao
    abstract fun workflowEventDao(): WorkflowEventDao
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
