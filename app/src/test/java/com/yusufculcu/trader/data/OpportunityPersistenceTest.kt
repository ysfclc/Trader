package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
class OpportunityPersistenceTest {
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
    fun opportunityIsLinkedToCaseAndStageChangesAreRecorded() = runBlocking {
        val problemId = repository.createProblem(
            title = "Company needs outbound process repair",
            description = "Public job post and product pages indicate a specific operations problem"
        )
        val firstDraft = OpportunityDraft(
            lane = "SERVICE",
            organizationName = "Example SaaS",
            opportunityName = "Repair fragmented sales operations",
            contactName = "Operations lead",
            contactRoute = "LinkedIn profile URL",
            fitNotes = "Relevant proof: EBO workflow and automation implementation; validate the stated pain before pitching.",
            stage = "QUALIFYING",
            nextAction = "Compare the stated requirements with the working EBO proof",
            followUpAt = 1_800_000_000_000L
        )

        assertTrue(repository.saveOpportunity(problemId, firstDraft))
        val created = database.opportunityDao().getByProblemId(problemId)
        assertNotNull(created)
        assertEquals("SERVICE", created?.lane)
        assertEquals("QUALIFYING", created?.stage)
        assertEquals("Example SaaS", created?.organizationName)
        assertEquals(1, database.opportunityDao().getAll().size)

        val changed = firstDraft.copy(
            stage = "CONTACTED",
            nextAction = "Record the reply and agree a discovery call"
        )
        assertTrue(repository.saveOpportunity(problemId, changed))

        val saved = database.opportunityDao().getByProblemId(problemId)
        assertEquals(created?.id, saved?.id)
        assertEquals("CONTACTED", saved?.stage)
        assertEquals(1, database.opportunityDao().getAll().size)
        assertTrue(database.activityDao().getAll().any { it.eventType == "opportunity_created" })
        assertTrue(database.activityDao().getAll().any { it.eventType == "opportunity_stage_changed" })
        assertEquals(problemId, repository.observeOpportunity(problemId).first()?.problemId)
    }

    @Test
    fun archivedCaseCannotBeAddedToOpportunityPipeline() = runBlocking {
        val problemId = repository.createProblem("Review a potential job")
        assertTrue(repository.archiveProblem(problemId))

        val accepted = repository.saveOpportunity(
            problemId,
            OpportunityDraft(
                lane = "CAREER",
                organizationName = "Example Employer",
                opportunityName = "Operations role",
                contactName = "",
                contactRoute = "",
                fitNotes = "",
                stage = "DISCOVERED",
                nextAction = "",
                followUpAt = null
            )
        )

        assertFalse(accepted)
        assertNull(database.opportunityDao().getByProblemId(problemId))
    }

    @Test
    fun invalidStageLaneAndMissingOpportunityNameAreRejected() = runBlocking {
        val problemId = repository.createProblem("Keep a generic problem")

        val invalidLane = repository.saveOpportunity(
            problemId,
            validDraft().copy(lane = "UNRECOGNIZED")
        )
        val invalidStage = repository.saveOpportunity(
            problemId,
            validDraft().copy(stage = "SOMEDAY")
        )
        val missingName = repository.saveOpportunity(
            problemId,
            validDraft().copy(opportunityName = "   ")
        )

        assertFalse(invalidLane)
        assertFalse(invalidStage)
        assertFalse(missingName)
        assertTrue(database.opportunityDao().getAll().isEmpty())
    }

    private fun validDraft() = OpportunityDraft(
        lane = "TRADE",
        organizationName = "Example Buyer",
        opportunityName = "Cutting tool requirement",
        contactName = "",
        contactRoute = "",
        fitNotes = "",
        stage = "DISCOVERED",
        nextAction = "",
        followUpAt = null
    )
}
