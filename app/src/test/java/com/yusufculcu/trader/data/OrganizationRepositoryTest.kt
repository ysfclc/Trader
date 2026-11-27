package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.discovery.DiscoveryDocument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
    fun correctingReviewedClaimPreservesIdentityInvalidatesReviewAndAppendsActivity() = runBlocking {
        val company = repository.createOrganization(OrganizationDraft("Claim Correction Works"))
        val source = repository.addSource(
            company.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Original source", "https://claims.example/original", "Original excerpt", 10)
        )
        val correctedSource = repository.addSource(
            company.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Corrected source", "https://claims.example/corrected", "Corrected excerpt", 11)
        )
        val claim = repository.addClaim(
            company.id,
            OrganizationClaimDraft(source.id, "Material", "The company uses aluminium.")
        )
        assertTrue(repository.reviewClaim(claim.id, OrganizationClaimState.SUPPORTED, "Confirmed from source."))
        val reviewed = repository.observeClaims(company.id).first().single()
        val activityBefore = database.businessActivityDao().getAll().count { it.entityType == BusinessActivityEntityType.CLAIM && it.entityId == claim.id }

        val corrected = repository.updateClaim(
            claim.id,
            OrganizationClaimDraft(correctedSource.id, "Material specification", "The company uses certified 6061 aluminium.")
        )

        assertEquals(claim.id, corrected.id)
        assertEquals(claim.createdAt, corrected.createdAt)
        assertEquals(correctedSource.id, corrected.sourceId)
        assertEquals("Material specification", corrected.field)
        assertEquals("The company uses certified 6061 aluminium.", corrected.statement)
        assertEquals(OrganizationClaimState.UNREVIEWED, corrected.reviewState)
        assertEquals(null, corrected.reviewNote)
        assertEquals(null, corrected.reviewedAt)
        assertTrue(corrected.updatedAt >= reviewed.updatedAt)

        val events = database.businessActivityDao().getAll()
            .filter { it.entityType == BusinessActivityEntityType.CLAIM && it.entityId == claim.id }
        assertEquals(activityBefore + 1, events.size)
        val correctionEvent = events.last()
        assertEquals(BusinessActivityAction.UPDATED, correctionEvent.eventType)
        assertEquals(OrganizationClaimState.SUPPORTED, correctionEvent.fromStatus)
        assertEquals(OrganizationClaimState.UNREVIEWED, correctionEvent.toStatus)

        val noOp = repository.updateClaim(
            claim.id,
            OrganizationClaimDraft(correctedSource.id, corrected.field, corrected.statement)
        )
        assertEquals(corrected, noOp)
        assertEquals(activityBefore + 1, database.businessActivityDao().getAll().count { it.entityType == BusinessActivityEntityType.CLAIM && it.entityId == claim.id })

        val otherCompany = repository.createOrganization(OrganizationDraft("Other Claim Company"))
        val otherSource = repository.addSource(
            otherCompany.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Other source", "https://other-claims.example", "", 12)
        )
        var crossCompanySourceRejected = false
        try {
            repository.updateClaim(claim.id, OrganizationClaimDraft(otherSource.id, "Wrong", "Wrong company source."))
        } catch (_: IllegalArgumentException) {
            crossCompanySourceRejected = true
        }
        assertTrue(crossCompanySourceRejected)
        assertEquals(corrected, repository.observeClaims(company.id).first().single())
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
    fun companyContactsAreValidatedEditableAndNeverAutoMerged() = runBlocking {
        val company = repository.createOrganization(OrganizationDraft("Northstar Precision"))
        val person = repository.addPerson(company.id, OrganizationPersonDraft("Ada Example", "Purchasing Manager", "ada@example.com", "+48 600 000 000", "https://www.linkedin.com/in/ada-example", "Met at trade fair."))
        assertEquals(company.id, person.organizationId); assertEquals(1, repository.observePeople(company.id).first().size)
        val updated = repository.updatePerson(person.id, OrganizationPersonDraft("Ada Example", "Head of Procurement"))
        assertEquals("Head of Procurement", updated.role); assertEquals(null, updated.email)
        assertTrue(repository.deletePerson(person.id)); assertTrue(repository.observePeople(company.id).first().isEmpty())
        var invalidEmail = false
        try { repository.addPerson(company.id, OrganizationPersonDraft("Bad", email = "not-an-email")) } catch (_: IllegalArgumentException) { invalidEmail = true }
        assertTrue(invalidEmail)
        var invalidProfile = false
        try { repository.addPerson(company.id, OrganizationPersonDraft("Bad", profileUrl = "https://example.com/person")) } catch (_: IllegalArgumentException) { invalidProfile = true }
        assertTrue(invalidProfile)
    }

    @Test
    fun aliasesAndIdentityDecisionsPersistWithProvenanceAndReversibleHistory() = runBlocking {
        val company = repository.createOrganization(OrganizationDraft("Identity Works"))
        val source = repository.addSource(company.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Team page", "https://identity.example/team", "", 10))
        val first = repository.addPerson(company.id, OrganizationPersonDraft("Ada Example"))
        val second = repository.addPerson(company.id, OrganizationPersonDraft("Ada E."))
        val alias = repository.addPersonAlias(first.id, OrganizationPersonAliasDraft(PersonAliasType.EMAIL, " ADA@EXAMPLE.COM ", source.id))
        assertEquals("ada@example.com", alias.normalizedValue)
        assertEquals(PersonAliasProvenance.SOURCE_LINKED, alias.provenanceKind)
        repository.reviewPersonIdentity(first.id, second.id, PersonIdentityDecisionKind.SAME_PERSON, "Confirmed from company team page", source.id)
        repository.reviewPersonIdentity(first.id, second.id, PersonIdentityDecisionKind.REOPENED, "New evidence needed")
        val decisions = repository.observeIdentityDecisions(company.id).first()
        assertEquals(listOf(PersonIdentityDecisionKind.SAME_PERSON, PersonIdentityDecisionKind.REOPENED), decisions.map { it.decision })
        assertEquals(1, repository.observePersonAliases(company.id).first().size)
        val other = repository.createOrganization(OrganizationDraft("Other Works"))
        val otherSource = repository.addSource(other.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Other", "https://other.example", "", 11))
        var crossCompanyAliasRejected = false
        try { repository.addPersonAlias(first.id, OrganizationPersonAliasDraft(PersonAliasType.NAME, "A. Example", otherSource.id)) } catch (_: IllegalArgumentException) { crossCompanyAliasRejected = true }
        assertTrue(crossCompanyAliasRejected)
        var crossCompanyDecisionRejected = false
        val otherPerson = repository.addPerson(other.id, OrganizationPersonDraft("Ada Example"))
        try { repository.reviewPersonIdentity(first.id, otherPerson.id, PersonIdentityDecisionKind.SAME_PERSON) } catch (_: IllegalArgumentException) { crossCompanyDecisionRejected = true }
        assertTrue(crossCompanyDecisionRejected)
    }

    @Test
    fun aliasesAndAppendOnlyIdentityHistorySurviveEncryptedBackupRestore() = runBlocking {
        val company = repository.createOrganization(OrganizationDraft("Backup Identity"))
        val source = repository.addSource(company.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Source", "https://backup-identity.example", "", 10))
        val first = repository.addPerson(company.id, OrganizationPersonDraft("Ada Example"))
        val second = repository.addPerson(company.id, OrganizationPersonDraft("A. Example"))
        repository.addPersonAlias(first.id, OrganizationPersonAliasDraft(PersonAliasType.NAME, "Ada E.", source.id))
        repository.reviewPersonIdentity(first.id, second.id, PersonIdentityDecisionKind.SAME_PERSON, "Reviewed", source.id)
        val output = ByteArrayOutputStream()
        TraderBackupManager(database).exportEncrypted(output, "long-enough-test-passphrase".toCharArray())
        val restored = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            TraderBackupManager(restored).restoreEncrypted(ByteArrayInputStream(output.toByteArray()), "long-enough-test-passphrase".toCharArray())
            assertEquals(1, restored.organizationPersonAliasDao().getAll().size)
            assertEquals(PersonIdentityDecisionKind.SAME_PERSON, restored.organizationPersonIdentityDecisionDao().getAll().single().decision)
        } finally { restored.close() }
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
