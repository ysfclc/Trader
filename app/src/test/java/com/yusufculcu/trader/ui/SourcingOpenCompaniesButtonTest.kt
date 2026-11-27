package com.yusufculcu.trader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SourcingOpenCompaniesButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryEmptyRequirementActionInvokesCompaniesNavigation() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                SourcingOpenCompaniesButton(onClick = { clicked = true }, primary = true)
            }
        }

        composeRule.onNodeWithText("Open Companies").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun secondaryEmptyOffersActionInvokesCompaniesNavigation() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                SourcingOpenCompaniesButton(onClick = { clicked = true }, primary = false)
            }
        }

        composeRule.onNodeWithText("Open Companies").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}
