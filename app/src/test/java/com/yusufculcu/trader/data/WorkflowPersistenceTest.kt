package com.yusufculcu.trader.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import kotlinx.coroutines.flow.first
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
                capabilityId = "manual.task",
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
            listOf(WorkflowStepDraft("Submit form", capabilityId = "manual.task", retrySafe = false, requiresApproval = true)),
            "workflow-request-4"
        )!!
        val step = database.workflowStepDao().getForRun(runId).single()

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertTrue(repository.approveWorkflowStep(step.id, "Confirmed the manual action is authorized"))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
        assertTrue(repository.transitionWorkflowStep(
            step.id,
            WorkflowStepStatus.NEEDS_REVIEW,
            errorMessage = "The browser did not confirm whether submission completed"
        ))
        assertEquals(WorkflowRunStatus.NEEDS_REVIEW.name, database.workflowRunDao().getById(runId)?.status)

        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY, reviewResolved = true))
        assertFalse(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.PENDING))
        assertTrue(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.PENDING, reviewResolved = true))
        assertEquals(WorkflowStepStatus.PENDING.name, database.workflowStepDao().getById(step.id)?.status)
    }

    @Test
    fun safeFailedStepCanBeResetOnlyAfterRunRetryAuthorization() = runBlocking {
        val problemId = repository.createProblem("Retry a read-only lookup")
        val runId = repository.createWorkflow(
            problemId,
            "Read-only lookup",
            listOf(WorkflowStepDraft("Fetch public page", capabilityId = "manual.task", retrySafe = true)),
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
    fun unimplementedExternalCapabilityIsPlannableButCannotStart() = runBlocking {
        val problemId = repository.createProblem("Prepare an external submission")
        val runId = repository.createWorkflow(
            problemId,
            "External submission",
            listOf(WorkflowStepDraft(
                title = "Submit form",
                capabilityId = "browser.submit",
                retrySafe = true,
                requiresApproval = false
            )),
            "workflow-request-unimplemented"
        )!!
        val step = database.workflowStepDao().getForRun(runId).single()

        assertTrue(step.requiresApproval)
        assertFalse(step.retrySafe)
        assertTrue(repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY))
        assertFalse(repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING))
        assertFalse(repository.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING))
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
    fun caseDetailsCanBeEditedAndArchivedWithoutDeletingItsHistory() = runBlocking {
        val problemId = repository.createProblem(
            "Old title",
            description = "Original context",
            desiredOutcome = "Old outcome",
            constraints = "Old constraint"
        )

        assertTrue(repository.updateProblemDetails(
            problemId,
            "Updated title",
            "Updated context",
            "Updated outcome",
            "Updated constraint"
        ))
        assertEquals("Updated title", database.problemDao().getById(problemId)?.title)
        assertEquals("Updated context", database.problemDao().getById(problemId)?.description)

        assertTrue(repository.archiveProblem(problemId))
        assertTrue(database.problemDao().observeArchived().firstForTest().any { it.id == problemId })
        assertFalse(database.problemDao().observeAll().firstForTest().any { it.id == problemId })
        assertNotNull(database.problemDao().getById(problemId))
        assertTrue(repository.restoreProblem(problemId))
        assertTrue(database.problemDao().observeAll().firstForTest().any { it.id == problemId })
        assertNull(database.problemDao().getById(problemId)?.archivedAt)
    }

    @Test
    fun evidenceIsStoredWithSourceAndUserVerificationState() = runBlocking {
        val problemId = repository.createProblem("Compare a supplier")
        assertTrue(repository.addEvidence(
            problemId,
            "Supplier capability page",
            "The page lists five-axis machining and ISO certification.",
            "https://supplier.example.test/capabilities"
        ))
        val first = database.evidenceDao().observeForProblem(problemId).firstForTest().single()
        assertEquals("URL", first.sourceKind)
        assertEquals("UNREVIEWED", first.verificationState)
        assertEquals("https://supplier.example.test/capabilities", first.sourceUri)

        assertTrue(repository.setEvidenceVerification(first, true))
        assertEquals("VERIFIED", database.evidenceDao().observeForProblem(problemId).firstForTest().single().verificationState)
        assertTrue(repository.setEvidenceVerification(first.copy(verificationState = "VERIFIED"), false))
        assertEquals("UNREVIEWED", database.evidenceDao().observeForProblem(problemId).firstForTest().single().verificationState)
    }

    @Test
    fun processReopenRecoversRunningStepToReviewAndIdleRunToPaused() = runBlocking {
        val dbName = "trader-recovery-${UUID.randomUUID()}.db"
        val firstOpen = Room.databaseBuilder(context, TraderDatabase::class.java, dbName)
            .allowMainThreadQueries().build()
        val firstRepository = TraderRepository(firstOpen)
        val problemId = firstRepository.createProblem("Recover interrupted work")

        val activeRunId = firstRepository.createWorkflow(
            problemId, "Action interrupted mid-step",
            listOf(WorkflowStepDraft("Perform bounded action", capabilityId = "manual.task", requiresApproval = true)),
            "recovery-active-${UUID.randomUUID()}"
        )!!
        val activeStep = firstOpen.workflowStepDao().getForRun(activeRunId).single()
        assertTrue(firstRepository.approveWorkflowStep(activeStep.id, "Approved before simulated interruption"))
        assertTrue(firstRepository.transitionWorkflowRun(activeRunId, WorkflowRunStatus.READY))
        assertTrue(firstRepository.transitionWorkflowRun(activeRunId, WorkflowRunStatus.RUNNING))
        assertTrue(firstRepository.transitionWorkflowStep(activeStep.id, WorkflowStepStatus.RUNNING))

        val idleRunId = firstRepository.createWorkflow(
            problemId, "Interrupted between steps",
            listOf(WorkflowStepDraft("Next action", capabilityId = "manual.task")),
            "recovery-idle-${UUID.randomUUID()}"
        )!!
        assertTrue(firstRepository.transitionWorkflowRun(idleRunId, WorkflowRunStatus.READY))
        assertTrue(firstRepository.transitionWorkflowRun(idleRunId, WorkflowRunStatus.RUNNING))
        firstOpen.close()

        val reopened = Room.databaseBuilder(context, TraderDatabase::class.java, dbName)
            .allowMainThreadQueries().build()
        try {
            val recoveredRepository = TraderRepository(reopened)
            assertEquals(2, recoveredRepository.recoverInterruptedWorkflows())
            assertEquals(WorkflowRunStatus.NEEDS_REVIEW.name, reopened.workflowRunDao().getById(activeRunId)?.status)
            val recoveredStep = reopened.workflowStepDao().getForRun(activeRunId).single()
            assertEquals(WorkflowStepStatus.NEEDS_REVIEW.name, recoveredStep.status)
            assertNull(recoveredStep.approvedPayloadHash)
            assertTrue(recoveredStep.lastError.orEmpty().contains("Confirm whether its action completed"))
            assertEquals(WorkflowRunStatus.PAUSED.name, reopened.workflowRunDao().getById(idleRunId)?.status)
            assertEquals(1, reopened.workflowEventDao().getForRun(activeRunId).count {
                it.eventType == "step_interrupted_needs_review"
            })
            assertEquals(1, reopened.workflowEventDao().getForRun(idleRunId).count {
                it.eventType == "run_interrupted_paused"
            })
            val eventCount = reopened.workflowEventDao().getForRun(activeRunId).size +
                reopened.workflowEventDao().getForRun(idleRunId).size
            assertEquals(0, recoveredRepository.recoverInterruptedWorkflows())
            assertEquals(eventCount, reopened.workflowEventDao().getForRun(activeRunId).size +
                reopened.workflowEventDao().getForRun(idleRunId).size)
            assertTrue(recoveredRepository.transitionWorkflowRun(
                activeRunId, WorkflowRunStatus.READY, reviewResolved = true,
                details = "User reviewed the interrupted action"
            ))
            assertTrue(recoveredRepository.transitionWorkflowStep(
                recoveredStep.id, WorkflowStepStatus.PENDING, reviewResolved = true,
                details = "User confirmed the action did not complete"
            ))
            assertEquals(WorkflowRunStatus.READY.name, reopened.workflowRunDao().getById(activeRunId)?.status)
            assertEquals(WorkflowStepStatus.PENDING.name, reopened.workflowStepDao().getById(recoveredStep.id)?.status)
        } finally {
            reopened.close()
            context.deleteDatabase(dbName)
        }
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
            val workflowTable = migrated.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='workflow_runs'"
            )
            try {
                assertTrue(workflowTable.moveToFirst())
            } finally {
                workflowTable.close()
            }
            val evidenceTable = migrated.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='evidence_records'"
            )
            try {
                assertTrue(evidenceTable.moveToFirst())
            } finally {
                evidenceTable.close()
            }
            val archiveColumn = migrated.openHelper.readableDatabase.query("PRAGMA table_info(problem_cases)")
            try {
                var found = false
                while (archiveColumn.moveToNext()) {
                    if (archiveColumn.getString(1) == "archivedAt") found = true
                }
                assertTrue(found)
            } finally {
                archiveColumn.close()
            }
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }
}


private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstForTest(): T = first()
