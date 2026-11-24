package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.discovery.DiscoveryDocument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrganizationRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = OrganizationRepository(database)

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun discoverySourceAndClaimRemainUnreviewedUntilExplicitHumanReview() = runBlocking {
        val document = DiscoveryDocument(
            id = "finding-1", sourceId = "public-search", sourceName = "Public search",
            nativeId = "https://news.example/story", title = "Acme is looking for a machining supplier",
            url = "https://news.example/story#result",
            summary = "The article reports a quarterly requirement for machined components.",
            publishedAt = null, retrievedAt = 100
        )
        val organization = repository.createFromDiscovery(
            OrganizationDraft("Acme Industrial", "https://www.acme.example/about", "Poland", "Kraków", "Industrial machining", "Introduced by the operator."),
            document
        )
        assertEquals("acme.example", organization.domain)
        assertEquals("https://www.acme.example/about", organization.websiteUrl)
        val source = repository.observeSources(organization.id).first().single()
        assertEquals("https://news.example/story", source.url)
        val claim = repository.addClaim(
            organization.id,
            OrganizationClaimDraft(source.id, "Buyer requirement", "Requests machined components each quarter.")
        )
        assertEquals(OrganizationClaimState.UNREVIEWED, claim.reviewState)
        assertEquals(null, claim.reviewedAt)
        assertTrue(repository.reviewClaim(claim.id, OrganizationClaimState.SUPPORTED))
        val reviewed = repository.observeClaims(organization.id).first().single()
        assertEquals(OrganizationClaimState.SUPPORTED, reviewed.reviewState)
        assertNotNull(reviewed.reviewedAt)
        assertTrue(repository.reviewClaim(claim.id, OrganizationClaimState.UNREVIEWED))
        val reset = repository.observeClaims(organization.id).first().single()
        assertEquals(OrganizationClaimState.UNREVIEWED, reset.reviewState)
        assertEquals(null, reset.reviewedAt)
    }

    @Test
    fun duplicateSourceUrlIsIdempotentAndDeletingOrganizationCascades() = runBlocking {
        val organization = repository.createOrganization(OrganizationDraft("Example Works"))
        val first = repository.addSource(
            organization.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Company page", "https://example.com/company#top", "About the company", 10)
        )
        val second = repository.addSource(
            organization.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Duplicate page", "https://example.com/company", "Different excerpt", 11)
        )
        assertEquals(first.id, second.id)
        repository.addClaim(organization.id, OrganizationClaimDraft(first.id, "Supplier capability", "Produces precision-machined parts."))
        assertEquals(1, repository.observeSources(organization.id).first().size)
        assertEquals(1, repository.observeClaims(organization.id).first().size)
        assertTrue(repository.deleteOrganization(organization.id))
        assertTrue(database.organizationSourceDao().getAll().isEmpty())
        assertTrue(database.organizationClaimDao().getAll().isEmpty())
        assertFalse(repository.deleteOrganization(organization.id))
    }

    @Test
    fun claimCannotBorrowAnotherOrganizationSourceAndUnsafeUrlsAreRejected() = runBlocking {
        val first = repository.createOrganization(OrganizationDraft("First Company"))
        val second = repository.createOrganization(OrganizationDraft("Second Company"))
        val source = repository.addSource(
            first.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Reference", "https://reference.example/page", "", 1)
        )
        var crossLinkRejected = false
        try {
            repository.addClaim(second.id, OrganizationClaimDraft(source.id, "Capability", "Makes components."))
        } catch (_: IllegalArgumentException) { crossLinkRejected = true }
        assertTrue(crossLinkRejected)

        var unsafeWebsiteRejected = false
        try {
            repository.createOrganization(OrganizationDraft("Unsafe", "http://example.com"))
        } catch (_: IllegalArgumentException) { unsafeWebsiteRejected = true }
        assertTrue(unsafeWebsiteRejected)

        var unsafeSourceRejected = false
        try {
            repository.addSource(second.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Local", "https://127.0.0.1/private", "", 2))
        } catch (_: IllegalArgumentException) { unsafeSourceRejected = true }
        assertTrue(unsafeSourceRejected)
    }
}
