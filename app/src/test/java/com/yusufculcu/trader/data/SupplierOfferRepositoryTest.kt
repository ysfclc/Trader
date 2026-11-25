package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupplierOfferRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
    private val organizations = OrganizationRepository(db)
    private val buyers = BuyerRequirementRepository(db)
    private val suppliers = SupplierOfferRepository(db)

    @After fun close() = db.close()

    @Test fun createsSupplierOfferWithEvidenceAndStoresHumanReview() = runBlocking {
        val buyer = organizations.createOrganization(OrganizationDraft("Buyer Ltd"))
        val supplier = organizations.createOrganization(OrganizationDraft("Supplier Ltd"))
        val source = organizations.addSource(supplier.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Capabilities", "https://supplier.example/capabilities"))
        val request = buyers.create(buyer.id, BuyerRequirementDraft("Precision housing", currency = "EUR"))
        buyers.addLineItem(request.id, BuyerRequirementLineItemDraft("Aluminium housing", "6082-T6, ±0.05 mm", "100", "pcs", 2500))
        val offer = suppliers.create(
            supplier.id,
            SupplierOfferDraft("Machined housing offer", "Supplier-stated offer", currency = "EUR", originCountry = "Türkiye"),
            SupplierOfferLineItemDraft("Aluminium housing", "6082-T6, ±0.05 mm", "Aluminium", "150", "pcs", "50", 2200, "EUR", 21),
            listOf(source.id)
        )
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
        assertEquals(source.id, suppliers.observeEvidence(offer.id).first().single().sourceId)
        val review = suppliers.review(request.id, offer.id, SourcingReviewStatus.SHORTLISTED, "Request a drawing review and formal quote.")
        assertEquals(SourcingReviewStatus.SHORTLISTED, review.status)
        assertEquals(review.id, suppliers.review(request.id, offer.id, SourcingReviewStatus.NEEDS_INFORMATION, "Need current capacity confirmation.").id)
        assertEquals(SourcingReviewStatus.NEEDS_INFORMATION, suppliers.observeReviews(request.id).first().single().status)
    }

    @Test fun rejectsCrossCompanyEvidenceAndInvalidCommercialQuantities() = runBlocking {
        val supplier = organizations.createOrganization(OrganizationDraft("Supplier"))
        val other = organizations.createOrganization(OrganizationDraft("Other"))
        val source = organizations.addSource(other.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Other source", "https://other.example"))
        var rejected = false
        try {
            suppliers.create(supplier.id, SupplierOfferDraft("Offer"), SupplierOfferLineItemDraft("Part"), listOf(source.id))
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        rejected = false
        try {
            suppliers.create(supplier.id, SupplierOfferDraft("Offer"), SupplierOfferLineItemDraft("Part", quantityAvailable = "-2"))
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        rejected = false
        try {
            suppliers.create(supplier.id, SupplierOfferDraft("Offer"), SupplierOfferLineItemDraft("Part", unitPriceMinorUnits = 100))
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun comparisonNeverTreatsMissingOrDifferentSpecificationsAsPass() {
        val request = BuyerRequirementEntity("r", "buyer", "RFQ", null, "OPEN", null, null, null, "EUR", null, null, 1, 1)
        val buyer = BuyerRequirementLineItemEntity("b", "r", "Aluminium housing", "6082-T6 ±0.05mm", "100", "pcs", 2500, 1)
        val matching = SupplierOfferLineItemEntity("s", "o", "Aluminium housing", "6082-T6 ±0.05mm", "Aluminium", "150", "pcs", "50", 2200, "EUR", 21, 1)
        val result = SourcingMatchEngine.compare(request, buyer, matching)
        assertEquals("REVIEWABLE", result.disposition)
        assertTrue(result.checks.all { it.result == SourcingMatchEngine.SUPPORTED })
        val missing = matching.copy(specification = null)
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(request, buyer, missing).disposition)
        val insufficient = matching.copy(quantityAvailable = "20")
        assertEquals("CONSTRAINT_CONFLICT", SourcingMatchEngine.compare(request, buyer, insufficient).disposition)
        val differentUnit = matching.copy(quantityUnit = "kg")
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(request, buyer, differentUnit).disposition)
        val differentCurrency = matching.copy(currency = "TRY")
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(request, buyer, differentCurrency).disposition)
    }

    @Test fun whitespaceOnlyOptionalOfferAndLineItemFieldsAreStoredAsNull() = runBlocking {
        val supplier = organizations.createOrganization(OrganizationDraft("Optional Supplier Ltd"))
        val offer = suppliers.create(
            supplier.id,
            SupplierOfferDraft(title = "Machining offer", description = " ", currency = " ", originCountry = "  ", originCity = " ", notes = "  "),
            SupplierOfferLineItemDraft(productName = "Precision spacer", specification = " ", material = "  ", quantityAvailable = " ", quantityUnit = " ", minimumOrderQuantity = "  ", currency = " ")
        )
        assertNull(offer.description); assertNull(offer.currency); assertNull(offer.originCountry); assertNull(offer.originCity); assertNull(offer.notes)
        val item = suppliers.observeItems(offer.id).first().single()
        assertNull(item.specification); assertNull(item.material); assertNull(item.quantityAvailable); assertNull(item.quantityUnit); assertNull(item.minimumOrderQuantity); assertNull(item.currency)
    }
    @Test fun offerHeaderAndLineItemCorrectionsPreserveIdentityAndAppendHistory() = runBlocking {
        val supplier = organizations.createOrganization(OrganizationDraft("Correctable Supplier"))
        val offer = suppliers.create(
            supplier.id,
            SupplierOfferDraft("Original offer", status = SupplierOfferStatus.AVAILABLE, currency = "EUR"),
            SupplierOfferLineItemDraft("Original spacer", quantityAvailable = "20", quantityUnit = "pcs", currency = "EUR")
        )
        val originalItem = suppliers.observeItems(offer.id).first().single()
        val extraItem = suppliers.addLineItem(offer.id, SupplierOfferLineItemDraft("Washer", quantityAvailable = "100", quantityUnit = "pcs"))
        val corrected = suppliers.updateOffer(
            offer.id,
            SupplierOfferDraft("Updated offer", description = "Revision B", status = SupplierOfferStatus.ON_HOLD, currency = "EUR", originCountry = "Türkiye", notes = "Confirm new capacity")
        )
        assertEquals(offer.id, corrected.id)
        assertEquals(offer.createdAt, corrected.createdAt)
        assertEquals("Updated offer", corrected.title)
        assertEquals(SupplierOfferStatus.ON_HOLD, corrected.status)
        val correctedItem = suppliers.updateLineItem(
            originalItem.id,
            SupplierOfferLineItemDraft("Revised spacer", "Drawing B", "Steel", "25", "pcs", "10", 1250, "EUR", 14)
        )
        assertEquals(originalItem.id, correctedItem.id)
        assertEquals(originalItem.createdAt, correctedItem.createdAt)
        assertEquals("Revised spacer", correctedItem.productName)
        assertEquals("25", correctedItem.quantityAvailable)
        val events = db.businessActivityDao().getAll()
        val headerEvent = events.single {
            it.entityId == offer.id && it.eventType == BusinessActivityAction.STATUS_CHANGED && it.entityLabel == "Updated offer"
        }
        assertEquals(SupplierOfferStatus.AVAILABLE, headerEvent.fromStatus)
        assertEquals(SupplierOfferStatus.ON_HOLD, headerEvent.toStatus)
        assertTrue(events.any {
            it.entityId == offer.id && it.eventType == BusinessActivityAction.LINE_UPDATED && it.details == "Revised spacer"
        })
        assertTrue(suppliers.deleteLineItem(extraItem.id))
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
        var rejected = false
        try { suppliers.deleteLineItem(originalItem.id) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
    }

    @Test fun supplierEvidenceLinksCanBeReconciledAndEveryChangeIsAudited() = runBlocking {
        val supplier = organizations.createOrganization(OrganizationDraft("Evidence Supplier"))
        val other = organizations.createOrganization(OrganizationDraft("Other Supplier"))
        val first = organizations.addSource(supplier.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Old catalogue", "https://supplier.example/old"))
        val second = organizations.addSource(supplier.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Current catalogue", "https://supplier.example/current"))
        val foreign = organizations.addSource(other.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Foreign source", "https://other.example/source"))
        val offer = suppliers.create(
            supplier.id, SupplierOfferDraft("Evidence-backed offer"),
            SupplierOfferLineItemDraft("Precision part"), listOf(first.id)
        )
        assertEquals(2, suppliers.setEvidenceLinks(offer.id, setOf(second.id)))
        assertEquals(setOf(second.id), suppliers.observeEvidence(offer.id).first().map { it.sourceId }.toSet())
        val eventCount = db.businessActivityDao().getAll().size
        assertEquals(0, suppliers.setEvidenceLinks(offer.id, setOf(second.id)))
        assertEquals(eventCount, db.businessActivityDao().getAll().size)
        var rejected = false
        try { suppliers.setEvidenceLinks(offer.id, setOf(foreign.id)) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(setOf(second.id), suppliers.observeEvidence(offer.id).first().map { it.sourceId }.toSet())
        val events = db.businessActivityDao().getAll()
        assertTrue(events.any { it.entityId == offer.id && it.eventType == BusinessActivityAction.EVIDENCE_UNLINKED && it.details == "Old catalogue" })
        assertTrue(events.any { it.entityId == offer.id && it.eventType == BusinessActivityAction.EVIDENCE_LINKED && it.details == "Current catalogue" })
    }


    @Test fun deletedOfferCanBeRestoredWithoutLosingItemsEvidenceOrSourcingDecision() = runBlocking {
        val buyer = organizations.createOrganization(OrganizationDraft("Recovery Buyer"))
        val supplier = organizations.createOrganization(OrganizationDraft("Recovery Supplier"))
        val source = organizations.addSource(supplier.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Offer evidence", "https://supplier.example/offer"))
        val requirement = buyers.create(buyer.id, BuyerRequirementDraft("Recovery requirement"))
        val offer = suppliers.create(
            supplier.id,
            SupplierOfferDraft("Recoverable offer"),
            SupplierOfferLineItemDraft("Precision component"),
            listOf(source.id)
        )
        suppliers.review(requirement.id, offer.id, SourcingReviewStatus.SHORTLISTED, "Keep this decision.")
        assertTrue(suppliers.deleteOffer(offer.id))
        assertTrue(suppliers.observeOffers().first().isEmpty())
        assertEquals(offer.id, suppliers.observeDeletedOffers().first().single().id)
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
        assertEquals(source.id, suppliers.observeEvidence(offer.id).first().single().sourceId)
        assertEquals(SourcingReviewStatus.SHORTLISTED, suppliers.observeReviews(requirement.id).first().single().status)
        assertTrue(suppliers.restoreOffer(offer.id))
        assertEquals(offer.id, suppliers.observeOffers().first().single().id)
        assertTrue(suppliers.observeDeletedOffers().first().isEmpty())
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
        assertEquals(source.id, suppliers.observeEvidence(offer.id).first().single().sourceId)
        assertEquals(SourcingReviewStatus.SHORTLISTED, suppliers.observeReviews(requirement.id).first().single().status)
        val actions = db.businessActivityDao().getAll().filter { it.entityId == offer.id }.map { it.eventType }
        assertTrue(actions.contains(BusinessActivityAction.DELETED))
        assertTrue(actions.contains(BusinessActivityAction.RESTORED))
    }


    @Test fun companyDeletionCannotCascadeSupplierOffers() = runBlocking {
        val supplier = organizations.createOrganization(OrganizationDraft("Protected supplier"))
        val offer = suppliers.create(supplier.id, SupplierOfferDraft("Protected offer"), SupplierOfferLineItemDraft("Part"))
        var rejected = false
        try { organizations.deleteOrganization(supplier.id) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(offer.id, suppliers.observeOffers().first().single().id)
        assertEquals(1, suppliers.observeItems(offer.id).first().size)
    }

}
