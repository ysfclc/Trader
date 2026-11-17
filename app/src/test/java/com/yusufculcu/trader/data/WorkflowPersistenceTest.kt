package com.yusufculcu.trader.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class WorkflowPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = TraderRepository(database)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun workflowCreationIsIdempotentAndEventsAreAppendOnly() = runBlocking {
        val problemId = repository.createProblem("Prepare a supplier introduction")
        val draft = listOf(
            WorkflowStepDraft("Collect buyer requirements", expectedPostcondition = "Requirements are recorded"),
            WorkflowStepDraft("Prepare introduction", expectedPostcondition = "Draft is ready for review")
        )

        val firstId = repository.createWorkflow(problemId, "Supplier introduction", draft, "workflow-request-1")
        val secondId = repository.createWorkflow(problemId, "Supplier introduction", draft, "workflow-request-1")

        assertNotNull(firstId)
        assertEquals(firstId, secondId)
        val run = database.workflowRunDao().getById(firstId!!)
        assertEquals(WorkflowRunStatus.DRAFT.name, run?.status)
        assertEquals(2, database.workflowStepDao().getForRun(firstId).size)
        assertEquals(1, database.workflowEventDao().getForRun(firstId).count { it.eventType == "workflow_created" })
        val eventCount = database.workflowEventDao().getForRun(firstId).size
        assertTrue(repository.transitionWorkflowRun(firstId, WorkflowRunStatus.DRAFT))
        assertEquals(eventCount, database.workflowEventDao().getForRun(firstId).size)
    }

    @Test
    fun completedStepsRequireSeparateOutcomeVerification() = runBlocking {
        val problemId = repository.createProblem("Resolve a technical issue")
        val runId = repository.createWorkflow(
            problemId,
            "Resolve issue",
            listOf(WorkflowStepDraft("Inspect logs"), WorkflowStepDraft("Apply fix")),
            "workflow-request-2"
        )!!
        val steps = database.workflowStepDao().getForRun(runId)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(steps[0].id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(steps[0].id, WorkflowStepStatus.COMPLETED))
        assertEquals(WorkflowRunStatus.RUNNING.name, database.workflowRunDao().getById(runId)?.status)

        assertTrue(repository.transitionWorkflowStep(steps[1].id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(steps[1].id, WorkflowStepStatus.COMPLETED))
        assertEquals(WorkflowRunStatus.AWAITING_VERIFICATION.name, database.workflowRunDao().getById(runId)?.status)
        assertFalse(repository.transitionWorkflowRun(runId, WorkflowRunStatus.VERIFIED, outcomeVerified = true))
        assertTrue(repository.transitionWorkflowRun(
            runId,
            WorkflowRunStatus.VERIFIED,
            outcomeVerified = true,
            details = "The issue is no longer reproducible"
        ))
        assertEquals(WorkflowRunStatus.VERIFIED.name, database.workflowRunDao().getById(runId)?.status)
    }

    @Test
    fun approvalIsBoundToTheExactStepPayload() = runBlocking {
        val problemId = repository.createProblem("Send an approved introduction")
        val runId = repository.createWorkflow(
            problemId,
            "Prepare message",
            listOf(WorkflowStepDraft(
                title = "Send message",
                capabilityId = "communication.send",
                inputJson = """{"recipient":"buyer@example.test","body":"Hello"}""",
                requiresApproval = true
            )),
            "workflow-request-3"
        )!!
        val step = database.workflowStepDao().getForRun(runId).single()

        assertTrue(repository.approveWorkflowStep(step.id, "Reviewed exact recipient and content"))
        assertTrue(repository.updateWorkflowStepInput(step.id, """{"recipient":"other@example.test","body":"Hello"}"""))
        assertNull(database.workflowStepDao().getById(step.id)?.approvedPayloadHash)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertFalse(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.approveWorkflowStep(step.id, "Reviewed updated recipient and content"))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
        assertEquals(1, database.workflowStepDao().getById(step.id)?.attemptCount)
    }

    @Test
    fun ambiguousStepRequiresReviewAndCannotBeBlindlyRetried() = runBlocking {
        val problemId = repository.createProblem("Submit an external form")
        val runId = repository.createWorkflow(
            problemId,
            "Submit form",
            listOf(WorkflowStepDraft("Submit form", capabilityId = "browser.submit", retrySafe = false)),
            "workflow-request-4"
        )!!
        val step = database.workflowStepDao().getForRun(runId).single()

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(
            step.id,
            WorkflowStepStatus.NEEDS_REVIEW,
            errorMessage = "The browser did not confirm whether submission completed"
        ))
        assertEquals(WorkflowRunStatus.NEEDS_REVIEW.name, database.workflowRunDao().getById(runId)?.status)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY, reviewResolved = true))
        assertFalse(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.PENDING, reviewResolved = true))
    }

    @Test
    fun safeFailedStepCanBeResetOnlyAfterRunRetryAuthorization() = runBlocking {
        val problemId = repository.createProblem("Retry a read-only lookup")
        val runId = repository.createWorkflow(
            problemId,
            "Read-only lookup",
            listOf(WorkflowStepDraft("Fetch public page", capabilityId = "web.fetch", retrySafe = true)),
            "workflow-request-5"
        )!!
        val step = database.workflowStepDao().getForRun(runId).single()

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.FAILED, errorMessage = "Temporary network error"))
        assertEquals(WorkflowRunStatus.FAILED.name, database.workflowRunDao().getById(runId)?.status)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY, retryAuthorized = true))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.PENDING))
        assertEquals(WorkflowStepStatus.PENDING.name, database.workflowStepDao().getById(step.id)?.status)
    }

    @Test
    fun cancellingRunCancelsEveryNonTerminalStep() = runBlocking {
        val problemId = repository.createProblem("Stop a multi-step process")
        val runId = repository.createWorkflow(
            problemId,
            "Cancelable workflow",
            listOf(WorkflowStepDraft("First step"), WorkflowStepDraft("Second step")),
            "workflow-request-6"
        )!!
        val steps = database.workflowStepDao().getForRun(runId)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(steps.first().id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.CANCELLED))

        assertEquals(WorkflowRunStatus.CANCELLED.name, database.workflowRunDao().getById(runId)?.status)
        assertTrue(database.workflowStepDao().getForRun(runId).all {
            WorkflowStepStatus.from(it.status) == WorkflowStepStatus.CANCELLED
        })
    }

    @Test
    fun migrationFromRoomVersionOnePreservesProblemDataAndAddsWorkflowTables() = runBlocking {
        val dbName = "trader-v1-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        old.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `problem_cases` (
                `id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL,
                `desiredOutcome` TEXT NOT NULL, `constraintsText` TEXT NOT NULL, `category` TEXT NOT NULL,
                `status` TEXT NOT NULL, `inputKind` TEXT NOT NULL, `originalContent` TEXT NOT NULL,
                `sourceUri` TEXT, `outcomeNote` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_problem_cases_updatedAt` ON `problem_cases` (`updatedAt`)")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_problem_cases_status` ON `problem_cases` (`status`)")
        old.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plan_steps` (
                `id` TEXT NOT NULL, `problemId` TEXT NOT NULL, `title` TEXT NOT NULL,
                `rationale` TEXT NOT NULL, `requiredCapability` TEXT NOT NULL, `expectedResult` TEXT NOT NULL,
                `status` TEXT NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`problemId`) REFERENCES `problem_cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_steps_problemId` ON `plan_steps` (`problemId`)")
        old.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plan_steps_problemId_position` ON `plan_steps` (`problemId`, `position`)")
        old.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activity_events` (
                `id` TEXT NOT NULL, `problemId` TEXT NOT NULL, `eventType` TEXT NOT NULL,
                `summary` TEXT NOT NULL, `details` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`problemId`) REFERENCES `problem_cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_problemId` ON `activity_events` (`problemId`)")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_occurredAt` ON `activity_events` (`occurredAt`)")
        old.execSQL(
            "INSERT INTO problem_cases VALUES ('problem-1','Existing problem','','','','General','OPEN','manual','','', '',1,1)"
        )
        old.execSQL(
            "INSERT INTO plan_steps VALUES ('step-1','problem-1','Existing step','','','','TODO',0,1,1)"
        )
        old.execSQL(
            "INSERT INTO activity_events VALUES ('event-1','problem-1','created','Captured','',1)"
        )
        old.version = 1
        old.close()

        val migrated = Room.databaseBuilder(context, TraderDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Existing problem", migrated.problemDao().getById("problem-1")?.title)
            val preservedRows = migrated.openHelper.readableDatabase.query(
                "SELECT (SELECT COUNT(*) FROM plan_steps WHERE id='step-1') + (SELECT COUNT(*) FROM activity_events WHERE id='event-1')"
            )
            try {
                assertTrue(preservedRows.moveToFirst())
                assertEquals(2, preservedRows.getInt(0))
            } finally {
                preservedRows.close()
            }
            val table = migrated.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='workflow_runs'"
            )
            try {
                assertTrue(table.moveToFirst())
            } finally {
                table.close()
            }
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }
}
