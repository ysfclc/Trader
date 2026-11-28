package com.yusufculcu.trader.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import com.yusufculcu.trader.TraderApplication
import com.yusufculcu.trader.data.OrganizationDraft
import com.yusufculcu.trader.data.OrganizationSourceDraft
import com.yusufculcu.trader.data.OrganizationSourceKind
import com.yusufculcu.trader.data.BuyerRequirementState
import com.yusufculcu.trader.data.SourcingReviewStatus
import com.yusufculcu.trader.ui.theme.TraderTheme
import com.yusufculcu.trader.viewmodel.TraderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TraderApplication::class)
class SourcingVerticalSliceUiSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun operatorCanCreateRequirementOfferAndReachShortlistThroughRealAppNavigation() {
        val application = RuntimeEnvironment.getApplication() as TraderApplication
        val seeded = runBlocking(Dispatchers.IO) {
            application.database.clearAllTables()
            val organizations = application.organizationRepository
            val supplier = organizations.createOrganization(
                OrganizationDraft(
                    canonicalName = "Supplier Co",
                    websiteUrl = "https://supplier.example",
                    country = "Poland",
                    industry = "Industrial machining"
                )
            )
            val source = organizations.addSource(
                supplier.id,
                OrganizationSourceDraft(
                    sourceKind = OrganizationSourceKind.MANUAL_REFERENCE,
                    title = "Supplier specification sheet",
                    url = "https://supplier.example/specification",
                    excerpt = "Published supplier product specification."
                )
            )
            val buyer = organizations.createOrganization(
                OrganizationDraft(
                    canonicalName = "Buyer Co",
                    websiteUrl = "https://buyer.example",
                    country = "Germany",
                    industry = "Industrial equipment"
                )
            )
            buyer to (supplier to source)
        }
        val buyer = seeded.first
        val supplier = seeded.second.first
        val supplierSource = seeded.second.second
        val viewModel = TraderViewModel(application.repository)

        composeRule.setContent {
            TraderTheme {
                TraderApp(viewModel = viewModel)
            }
        }

        // Real bottom navigation -> Companies workspace -> buyer dossier.
        composeRule.onNodeWithTag("tab-pipeline").performClick()
        composeRule.onNodeWithTag("pipeline-companies").performClick()
        composeRule.onNodeWithTag("company-card-${buyer.id}").performClick()

        // Create a buyer requirement and a structured requirement line item.
        composeRule.onNodeWithTag("buyer-add-requirement").performScrollTo().performClick()
        composeRule.onNodeWithText("Requirement title").performTextInput("Milling tooling request")
        composeRule.onNodeWithText("Description / context").performTextInput("Need a source-backed supplier option.")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("buyer-add-line-item").performScrollTo().performClick()
        composeRule.onNodeWithText("Product or service").performTextInput("Steel plate")
        composeRule.onNodeWithText("Specification / tolerance").performTextInput("S355, 10 mm")
        composeRule.onNodeWithText("Quantity (decimal)").performTextInput("100")
        composeRule.onNodeWithText("Unit (pcs, kg, etc.)").performTextInput("pcs")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        // Return to the shared workspace and create an offer for the seeded supplier.
        composeRule.onNodeWithText("All companies").performScrollTo().performClick()
        composeRule.onNodeWithTag("tab-sourcing").performClick()
        composeRule.onNodeWithTag("sourcing-add-offer").performClick()
        composeRule.onNodeWithText("Buyer Co").performClick()
        composeRule.onNodeWithText("Supplier Co").performClick()
        composeRule.onNodeWithText("Offer title").performTextInput("Steel plate supply")
        composeRule.onNodeWithText("Product or service").performTextInput("Steel plate")
        composeRule.onNodeWithTag("offer-evidence-${supplierSource.id}").performClick()
        composeRule.onNodeWithText("Save supplier offer").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Record an explicit human shortlist decision; do not infer qualification.
        composeRule.onNodeWithTag("sourcing-review-decision").performScrollTo().performClick()
        composeRule.onNodeWithTag("sourcing-review-${SourcingReviewStatus.SHORTLISTED}").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Share shortlist").assertExists()

        val persisted = runBlocking(Dispatchers.IO) {
            val requirement = application.buyerRequirementRepository.observeAll().first().single()
            val buyerItems = application.buyerRequirementRepository.observeItems(requirement.id).first()
            val offer = application.supplierOfferRepository.observeOffers().first().single()
            val offerItems = application.supplierOfferRepository.observeItems(offer.id).first()
            val offerEvidence = application.supplierOfferRepository.observeEvidence(offer.id).first()
            val review = application.supplierOfferRepository.observeReviews(requirement.id).first().single()
            listOf(requirement, buyerItems.single(), offer, offerItems.single(), offerEvidence.single(), review)
        }

        val requirement = persisted[0] as com.yusufculcu.trader.data.BuyerRequirementEntity
        val buyerItem = persisted[1] as com.yusufculcu.trader.data.BuyerRequirementLineItemEntity
        val offer = persisted[2] as com.yusufculcu.trader.data.SupplierOfferEntity
        val offerItem = persisted[3] as com.yusufculcu.trader.data.SupplierOfferLineItemEntity
        val evidence = persisted[4] as com.yusufculcu.trader.data.SupplierOfferEvidenceEntity
        val review = persisted[5] as com.yusufculcu.trader.data.SourcingReviewEntity

        assertEquals(buyer.id, requirement.organizationId)
        assertEquals("Milling tooling request", requirement.title)
        assertEquals(BuyerRequirementState.OPEN, requirement.status)
        assertEquals("Steel plate", buyerItem.productName)
        assertEquals("S355, 10 mm", buyerItem.specification)
        assertEquals(supplier.id, offer.organizationId)
        assertEquals("Steel plate supply", offer.title)
        assertEquals("Steel plate", offerItem.productName)
        assertEquals(supplierSource.id, evidence.sourceId)
        assertEquals(SourcingReviewStatus.SHORTLISTED, review.status)
        assertTrue(review.rationale.isNullOrBlank())
    }
}
