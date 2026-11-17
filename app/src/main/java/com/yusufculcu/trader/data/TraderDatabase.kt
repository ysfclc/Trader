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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "problem_cases", indices = [Index(value = ["updatedAt"]), Index(value = ["status"])])
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
    val updatedAt: Long
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

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problem_cases ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProblemEntity>>
    @Query("SELECT * FROM problem_cases WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ProblemEntity?>
    @Query("SELECT * FROM problem_cases WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProblemEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(problem: ProblemEntity)
    @Query("UPDATE problem_cases SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)
    @Query("UPDATE problem_cases SET status = :status, outcomeNote = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, note: String, updatedAt: Long)
    @Query("SELECT COUNT(*) FROM problem_cases")
    fun observeCount(): Flow<Int>
}

@Dao
interface PlanStepDao {
    @Query("SELECT * FROM plan_steps WHERE problemId = :problemId ORDER BY position ASC")
    fun observeForProblem(problemId: String): Flow<List<PlanStepEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: PlanStepEntity)
    @Query("UPDATE plan_steps SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM plan_steps WHERE problemId = :problemId")
    suspend fun nextPosition(problemId: String): Int
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_events ORDER BY occurredAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<ActivityEventEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ActivityEventEntity)
}

@Database(entities = [ProblemEntity::class, PlanStepEntity::class, ActivityEventEntity::class], version = 1, exportSchema = true)
abstract class TraderDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun planStepDao(): PlanStepDao
    abstract fun activityDao(): ActivityDao
}
