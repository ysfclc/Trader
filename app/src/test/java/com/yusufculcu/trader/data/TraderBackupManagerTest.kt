package com.yusufculcu.trader.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject
import com.yusufculcu.trader.discovery.DiscoverySourceStore

@RunWith(RobolectricTestRunner::class)
class TraderBackupManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun encryptedBackupRoundTripRestoresEveryRecordGroupAndReplacesExistingData() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            seedCompleteWorkspace(source)
            target.problemDao().insert(problem("local-only", "This must be replaced"))

            val output = ByteArrayOutputStream()
            TraderBackupManager(source).exportEncrypted(output, "correct horse battery".toCharArray())
            val bytes = output.toByteArray()

            assertTrue(bytes.size > 64)
            assertFalse(String(bytes, StandardCharsets.ISO_8859_1).contains("CONFIDENTIAL_CASE_TITLE"))
            TraderBackupManager(target).restoreEncrypted(
                ByteArrayInputStream(bytes),
                "correct horse battery".toCharArray()
            )

            assertEquals(listOf("case-1"), target.problemDao().getAll().map { it.id })
            assertEquals("CONFIDENTIAL_CASE_TITLE", target.problemDao().getById("case-1")?.title)
            assertEquals(1, target.planStepDao().getAll().size)
            assertEquals("Expected result", target.planStepDao().getAll().single().expectedResult)
            assertEquals(1, target.activityDao().getAll().size)
            assertEquals(1, target.evidenceDao().getAll().size)
            assertEquals("VERIFIED", target.evidenceDao().getAll().single().verificationState)
            assertEquals(1, target.opportunityDao().getAll().size)
            assertEquals("Industrial tooling introduction", target.opportunityDao().getAll().single().opportunityName)
            assertEquals("CONTACTED", target.opportunityDao().getAll().single().stage)
            assertEquals(1, target.workflowRunDao().getAll().size)
            assertEquals(1, target.workflowStepDao().getAll().size)
            assertEquals("approval-hash", target.workflowStepDao().getAll().single().approvedPayloadHash)
            assertEquals(1, target.workflowEventDao().getAll().size)
            assertEquals(1, target.discoveryRunDao().getAll().size)
            assertEquals("COMPLETED", target.discoveryRunDao().getAll().single().status)
            assertEquals(1, target.sourceRunDao().getForRun("discovery-run-1").size)
            assertEquals(3, target.sourceRunDao().getForRun("discovery-run-1").single().resultCount)
            assertEquals(1, target.discoveryDocumentDao().getAll().size)
            assertEquals("Buyer requirement", target.discoveryDocumentDao().getAll().single().title)
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun encryptedBackupRoundTripPreservesDiscoverySourceSettingsAndReplacesTargetSources() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val sourceStore = DiscoverySourceStore(context, "backup_source_preferences")
        val targetStore = DiscoverySourceStore(context, "backup_target_preferences")
        try {
            sourceStore.replaceAll(emptyList())
            targetStore.replaceAll(emptyList())
            val sourceConfig = sourceStore.add("Hacker News", "https://news.ycombinator.com/rss")
            sourceStore.setEnabled(sourceConfig.id, false)
            sourceStore.markChecked(sourceConfig.id, 1_700_000_000_000L, "temporary refresh error")
            targetStore.add("Local only", "https://example.test/feed.xml")

            val output = ByteArrayOutputStream()
            TraderBackupManager(source, sourceStore).exportEncrypted(output, "correct horse battery".toCharArray())
            val bytes = output.toByteArray()
            assertFalse(String(bytes, StandardCharsets.ISO_8859_1).contains("news.ycombinator.com"))

            TraderBackupManager(target, targetStore).restoreEncrypted(
                ByteArrayInputStream(bytes),
                "correct horse battery".toCharArray()
            )

            assertEquals(sourceStore.list(), targetStore.list())
            assertEquals(1, targetStore.list().size)
            assertEquals(false, targetStore.list().single().enabled)
            assertEquals("temporary refresh error", targetStore.list().single().lastError)
            assertEquals(1_700_000_000_000L, targetStore.list().single().lastCheckedAt)
        } finally {
            sourceStore.replaceAll(emptyList())
            targetStore.replaceAll(emptyList())
            source.close()
            target.close()
        }
    }

    @Test
    fun schemaThreeBackupSnapshotRemainsReadableWithoutOpportunityRows() {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val legacy = JSONObject()
                .put("formatVersion", 1)
                .put("databaseSchemaVersion", 3)
                .put("problems", JSONArray())
                .put("planSteps", JSONArray())
                .put("activityEvents", JSONArray())
                .put("evidence", JSONArray())
                .put("workflowRuns", JSONArray())
                .put("workflowSteps", JSONArray())
                .put("workflowEvents", JSONArray())

            val decoded = TraderBackupManager(database).decodeSnapshot(legacy.toString())
            assertTrue(decoded.opportunities.isEmpty())
            assertTrue(decoded.discoverySources.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun wrongPassphraseDoesNotModifyExistingWorkspace() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            seedCompleteWorkspace(source)
            target.problemDao().insert(problem("keep-me", "Existing local data"))
            val output = ByteArrayOutputStream()
            TraderBackupManager(source).exportEncrypted(output, "correct horse battery".toCharArray())

            var rejected = false
            try {
                TraderBackupManager(target).restoreEncrypted(
                    ByteArrayInputStream(output.toByteArray()),
                    "wrong password value".toCharArray()
                )
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertTrue(rejected)
            assertEquals(listOf("keep-me"), target.problemDao().getAll().map { it.id })
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun tamperedBackupDoesNotModifyExistingWorkspace() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            seedCompleteWorkspace(source)
            target.problemDao().insert(problem("keep-me", "Existing local data"))
            val output = ByteArrayOutputStream()
            TraderBackupManager(source).exportEncrypted(output, "correct horse battery".toCharArray())
            val corrupted = output.toByteArray().also { it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte() }

            var rejected = false
            try {
                TraderBackupManager(target).restoreEncrypted(
                    ByteArrayInputStream(corrupted),
                    "correct horse battery".toCharArray()
                )
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertTrue(rejected)
            assertEquals(listOf("keep-me"), target.problemDao().getAll().map { it.id })
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun backupRequiresAtLeastTwelvePassphraseCharacters() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            var rejected = false
            try {
                TraderBackupManager(database).exportEncrypted(ByteArrayOutputStream(), "short".toCharArray())
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
        } finally {
            database.close()
        }
    }

    private suspend fun seedCompleteWorkspace(database: TraderDatabase) {
        val now = 1_700_000_000_000L
        database.problemDao().insert(problem("case-1", "CONFIDENTIAL_CASE_TITLE"))
        database.planStepDao().insert(
            PlanStepEntity("plan-1", "case-1", "Research", "Reason", "manual.task", "Expected result", "DONE", 0, now, now + 1)
        )
        database.activityDao().insert(
            ActivityEventEntity("activity-1", "case-1", "details_updated", "Changed", "Private details", now + 2)
        )
        database.evidenceDao().insert(
            EvidenceEntity("evidence-1", "case-1", "Source", "Private evidence", "https://example.test/source", "url", "VERIFIED", now + 3)
        )
        database.opportunityDao().insert(
            OpportunityEntity(
                id = "opportunity-1",
                problemId = "case-1",
                lane = "TRADE",
                organizationName = "Example Manufacturing",
                opportunityName = "Industrial tooling introduction",
                contactName = "Buyer contact",
                contactRoute = "buyer@example.test",
                fitNotes = "Buyer requirement is supported by the attached technical evidence.",
                stage = "CONTACTED",
                nextAction = "Follow up on the technical specification",
                followUpAt = now + 86_400_000L,
                createdAt = now + 3,
                updatedAt = now + 4
            )
        )
        database.workflowRunDao().insert(
            WorkflowRunEntity("run-1", "case-1", "Review", "DRAFT", "idempotency-1", now, now + 4, null, null, null)
        )
        database.workflowStepDao().insert(
            WorkflowStepEntity(
                id = "workflow-step-1",
                workflowRunId = "run-1",
                title = "Review source",
                capabilityId = "manual.task",
                inputJson = """{"source":"https://example.test"}""",
                expectedPostcondition = "Evidence reviewed",
                status = "PENDING",
                position = 0,
                retrySafe = true,
                requiresApproval = false,
                payloadHash = "payload-hash",
                approvedPayloadHash = "approval-hash",
                approvedAt = now + 5,
                attemptCount = 1,
                lastError = null,
                createdAt = now,
                updatedAt = now + 5,
                startedAt = null,
                finishedAt = null
            )
        )
        database.workflowEventDao().insert(
            WorkflowEventEntity("workflow-event-1", "run-1", "workflow-step-1", "approval_granted", null, null, "Approved", "Exact payload", now + 6)
        )
        database.discoveryRunDao().insert(DiscoveryRunEntity("discovery-run-1", null, "Find buyer requirements", "COMPLETED", now + 7, now + 8, now + 10, null, null))
        database.sourceRunDao().insert(SourceRunEntity("source-run-1", "discovery-run-1", "source-1", "rss.atom", "SUCCEEDED", now + 8, now + 9, 3, null, null))
        database.discoveryDocumentDao().insert(DiscoveryDocumentEntity("finding-1", "discovery-run-1", "source-1", "Example Feed", "native-1", "Buyer requirement", "https://example.test/requirement", "CNC machining requirement", now + 7, now + 9))
    }

    private fun problem(id: String, title: String) = ProblemEntity(
        id = id,
        title = title,
        description = "Description",
        desiredOutcome = "Outcome",
        constraintsText = "Constraints",
        category = "General",
        status = "OPEN",
        inputKind = "manual",
        originalContent = "Original content",
        sourceUri = null,
        outcomeNote = "",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_001L,
        archivedAt = null
    )
}
