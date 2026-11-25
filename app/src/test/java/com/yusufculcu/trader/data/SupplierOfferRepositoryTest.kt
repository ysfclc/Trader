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
        val buyer = BuyerRequirementLineItemEntity("b", "r", "Aluminium housing", "6082-T6 ±0.05mm", "100", "pcs", 2500, 1)
        val matching = SupplierOfferLineItemEntity("s", "o", "Aluminium housing", "6082-T6 ±0.05mm", "Aluminium", "150", "pcs", "50", 2200, "EUR", 21, 1)
        val result = SourcingMatchEngine.compare(buyer, matching)
        assertEquals("REVIEWABLE", result.disposition)
        assertTrue(result.checks.all { it.result == SourcingMatchEngine.SUPPORTED })
        val missing = matching.copy(specification = null)
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(buyer, missing).disposition)
        val insufficient = matching.copy(quantityAvailable = "20")
        assertEquals("CONSTRAINT_CONFLICT", SourcingMatchEngine.compare(buyer, insufficient).disposition)
        val differentUnit = matching.copy(quantityUnit = "kg")
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(buyer, differentUnit).disposition)
        val differentCurrency = matching.copy(currency = "TRY")
        assertEquals("MORE_INFORMATION_NEEDED", SourcingMatchEngine.compare(buyer, differentCurrency).disposition)
    }
}
