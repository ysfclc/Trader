package com.yusufculcu.trader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yusufculcu.trader.data.OrganizationClaimEntity
import com.yusufculcu.trader.data.OrganizationClaimState
import com.yusufculcu.trader.data.OrganizationSourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrganizationClaimDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingClaimCanReassignItsEvidenceSourceBeforeSaving() {
        val originalSource = OrganizationSourceEntity(
            id = "source-original",
            organizationId = "company-1",
            sourceKind = "MANUAL_REFERENCE",
            title = "Original source",
            url = "https://evidence.example/original",
            excerpt = "Original evidence",
            capturedAt = 1L
        )
        val correctedSource = OrganizationSourceEntity(
            id = "source-corrected",
            organizationId = "company-1",
            sourceKind = "MANUAL_REFERENCE",
            title = "Corrected source",
            url = "https://evidence.example/corrected",
            excerpt = "Corrected evidence",
            capturedAt = 2L
        )
        val claim = OrganizationClaimEntity(
            id = "claim-1",
            organizationId = "company-1",
            sourceId = originalSource.id,
            field = "Material",
            statement = "Uses aluminium",
            reviewState = OrganizationClaimState.SUPPORTED,
            reviewNote = "Verified",
            reviewedAt = 3L,
            createdAt = 1L,
            updatedAt = 3L
        )
        var savedSourceId: String? = null
        var savedField: String? = null
        var savedStatement: String? = null
        var saved = false

        composeRule.setContent {
            MaterialTheme {
                OrganizationClaimDialog(
                    source = originalSource,
                    sources = listOf(originalSource, correctedSource),
                    initial = claim,
                    onDismiss = {},
                    onSave = { sourceId, field, statement ->
                        savedSourceId = sourceId
                        savedField = field
                        savedStatement = statement
                        claim.id
                    },
                    onSaved = { saved = true }
                )
            }
        }

        composeRule.onNodeWithText("Source: Original source").performClick()
        composeRule.onNodeWithText("Corrected source").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(saved)
            assertEquals(correctedSource.id, savedSourceId)
            assertEquals("Material", savedField)
            assertEquals("Uses aluminium", savedStatement)
        }
    }
}
