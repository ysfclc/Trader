package com.yusufculcu.trader.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityRulesTest {
    @Test
    fun acceptsOnlyDeclaredOpportunityLanesAndStages() {
        assertTrue(OpportunityRules.isValidLane("TRADE"))
        assertTrue(OpportunityRules.isValidLane("SERVICE"))
        assertTrue(OpportunityRules.isValidLane("CAREER"))
        assertTrue(OpportunityRules.isValidLane("OTHER"))
        assertFalse(OpportunityRules.isValidLane("LEAD"))
        assertFalse(OpportunityRules.isValidStage("UNKNOWN"))
    }

    @Test
    fun onlyWonAndLostAreClosedStages() {
        assertTrue(OpportunityRules.isClosed("WON"))
        assertTrue(OpportunityRules.isClosed("LOST"))
        assertFalse(OpportunityRules.isClosed("WAITING"))
        assertFalse(OpportunityRules.isClosed("CONTACTED"))
    }
}
