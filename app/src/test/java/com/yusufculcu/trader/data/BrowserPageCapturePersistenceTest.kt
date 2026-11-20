package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class BrowserPageCapturePersistenceTest {
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
    fun newBrowserCaptureCreatesResearchCaseAndUnreviewedSourceEvidenceAtomically() = runBlocking {
        val url = "https://example.com/company/about"
        val content = "Acme manufactures industrial cutting tools and serves customers across Poland."

        val problemId = repository.captureBrowserPage(
            problemId = null,
            title = "Acme | Company profile",
            sourceUrl = url,
            content = content
        )

        assertNotNull(problemId)
        val problem = database.problemDao().getById(problemId!!)
        val evidence = database.evidenceDao().getAll().single()
        assertEquals("Acme | Company profile", problem?.title)
        assertEquals("Research", problem?.category)
        assertEquals("browser_capture", problem?.inputKind)
        assertEquals(url, problem?.sourceUri)
        assertEquals(problemId, evidence.problemId)
        assertEquals("BROWSER_PAGE", evidence.sourceKind)
        assertEquals(url, evidence.sourceUri)
        assertEquals(content, evidence.content)
        assertEquals("UNREVIEWED", evidence.verificationState)
        assertTrue(database.activityDao().getAll().any { it.eventType == "browser_page_captured" })
    }

    @Test
    fun existingCaseReceivesEvidenceWithoutCreatingAnotherCase() = runBlocking {
        val existingId = repository.createProblem("Qualify this supplier")
        val content = "The supplier lists CNC turning, milling, inspection and short-run production services."

        val savedId = repository.captureBrowserPage(
            problemId = existingId,
            title = "Supplier capabilities",
            sourceUrl = "https://supplier.example/capabilities",
            content = content
        )

        assertEquals(existingId, savedId)
        assertEquals(1, database.problemDao().getAll().size)
        val evidence = database.evidenceDao().getAll().single()
        assertEquals(existingId, evidence.problemId)
        assertEquals("BROWSER_PAGE", evidence.sourceKind)
    }

    @Test
    fun rejectsInvalidUrlShortContentAndArchivedDestination() = runBlocking {
        val existingId = repository.createProblem("Archived lead")
        assertTrue(repository.archiveProblem(existingId))

        assertNull(repository.captureBrowserPage(null, "Unsafe", "javascript:alert(1)", "Enough visible text for this test."))
        assertNull(repository.captureBrowserPage(null, "Too short", "https://example.com", "short"))
        assertNull(repository.captureBrowserPage(existingId, "Archived", "https://example.com", "Enough visible text for this test."))

        assertTrue(database.evidenceDao().getAll().isEmpty())
        assertEquals(1, database.problemDao().getAll().size)
    }
}
