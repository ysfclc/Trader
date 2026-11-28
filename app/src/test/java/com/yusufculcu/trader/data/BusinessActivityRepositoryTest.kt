package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BusinessActivityRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries().build()
    private val organizations = OrganizationRepository(database)
    private val buyers = BuyerRequirementRepository(database)
    private val suppliers = SupplierOfferRepository(database)

    @After
    fun closeDatabase() = database.close()

    @Test
    fun requirementOfferAndHumanDecisionChangesAppendImmutableStatusHistory() = runBlocking {
        val buyer = organizations.createOrganization(OrganizationDraft("Buyer Works"))
        val requirement = buyers.create(
            buyer.id,
            BuyerRequirementDraft("Precision housings", status = BuyerRequirementState.OPEN)
        )
        assertTrue(buyers.setStatus(requirement.id, BuyerRequirementState.PAUSED))
        assertTrue(buyers.setStatus(requirement.id, BuyerRequirementState.PAUSED))

        val supplier = organizations.createOrganization(OrganizationDraft("Supplier Works"))
        val offer = suppliers.create(
            supplier.id,
            SupplierOfferDraft("Machining offer", status = SupplierOfferStatus.DRAFT),
            SupplierOfferLineItemDraft("Aluminium housing")
        )
        assertTrue(suppliers.setStatus(offer.id, SupplierOfferStatus.AVAILABLE))
        val firstDecision = suppliers.review(
            requirement.id, offer.id, SourcingReviewStatus.NEEDS_INFORMATION, "Confirm current capacity."
        )
        val secondDecision = suppliers.review(
            requirement.id, offer.id, SourcingReviewStatus.SHORTLISTED, "Capacity statement received for review."
        )

        val events = database.businessActivityDao().getAll()
        val requirementHistory = events.filter {
            it.entityType == BusinessActivityEntityType.BUYER_REQUIREMENT &&
                it.entityId == requirement.id && it.eventType == BusinessActivityAction.STATUS_CHANGED
        }
        assertEquals(1, requirementHistory.size)
        assertEquals(BuyerRequirementState.OPEN, requirementHistory.single().fromStatus)
        assertEquals(BuyerRequirementState.PAUSED, requirementHistory.single().toStatus)

        val offerHistory = events.filter {
            it.entityType == BusinessActivityEntityType.SUPPLIER_OFFER &&
                it.entityId == offer.id && it.eventType == BusinessActivityAction.STATUS_CHANGED
        }
        assertEquals(1, offerHistory.size)
        assertEquals(SupplierOfferStatus.DRAFT, offerHistory.single().fromStatus)
        assertEquals(SupplierOfferStatus.AVAILABLE, offerHistory.single().toStatus)

        val decisionHistory = events.filter {
            it.entityType == BusinessActivityEntityType.SOURCING_REVIEW &&
                it.entityId == firstDecision.id
        }.sortedBy { it.occurredAt }
        assertEquals(2, decisionHistory.size)
        assertEquals(firstDecision.id, secondDecision.id)
        assertNull(decisionHistory.first().fromStatus)
        assertEquals(SourcingReviewStatus.NEEDS_INFORMATION, decisionHistory.first().toStatus)
        assertEquals(SourcingReviewStatus.NEEDS_INFORMATION, decisionHistory.last().fromStatus)
        assertEquals(SourcingReviewStatus.SHORTLISTED, decisionHistory.last().toStatus)
        assertTrue(database.businessActivityDao().observeRecent().first().any { it.id == decisionHistory.last().id })
    }

    @Test
    fun companyAndClaimLifecycleEventsRemainAfterDeletionWithoutCopyingContactPii() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Northstar Precision"))
        val source = organizations.addSource(
            company.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Capabilities", "https://northstar.example/capabilities", "We manufacture precision-machined components for industrial buyers.")
        )
        val claim = organizations.addClaim(
            company.id,
            OrganizationClaimDraft(source.id, "Capability", "Produces precision-machined components.")
        )
        assertTrue(organizations.reviewClaim(claim.id, OrganizationClaimState.SUPPORTED, "Reviewed against the saved source."))
        assertTrue(organizations.reviewClaim(claim.id, OrganizationClaimState.CONTRADICTED, "A later source conflicts with this claim."))

        val person = organizations.addPerson(
            company.id,
            OrganizationPersonDraft("Private Contact Name", "Buyer", "private@example.test", "+48 600 000 000")
        )
        assertTrue(organizations.deletePerson(person.id))
        val beforeDelete = database.businessActivityDao().getAll()
        assertFalse(beforeDelete.any { it.details.contains("private@example.test") || it.details.contains("+48 600 000 000") || it.entityLabel.contains("Private Contact Name") })

        assertTrue(organizations.deleteOrganization(company.id))
        val afterDelete = database.businessActivityDao().getAll()
        assertTrue(afterDelete.any {
            it.organizationId == company.id && it.entityType == BusinessActivityEntityType.ORGANIZATION &&
                it.eventType == BusinessActivityAction.DELETED
        })
        assertTrue(afterDelete.any {
            it.entityId == claim.id && it.eventType == BusinessActivityAction.CLAIM_REVIEWED &&
                it.fromStatus == OrganizationClaimState.SUPPORTED && it.toStatus == OrganizationClaimState.CONTRADICTED
        })
    }
}
