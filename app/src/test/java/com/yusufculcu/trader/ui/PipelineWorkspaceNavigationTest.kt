package com.yusufculcu.trader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineWorkspaceNavigationTest {

    @Test
    fun sourcingOpenCompaniesActionSelectsCompaniesWorkspace() {
        assertEquals(
            PipelineWorkspaceSection.COMPANIES,
            reducePipelineWorkspaceSection(PipelineWorkspaceAction.OPEN_COMPANIES)
        )
    }

    @Test
    fun companiesActionAlwaysTargetsCompaniesWorkspace() {
        assertEquals(
            PipelineWorkspaceSection.COMPANIES,
            reducePipelineWorkspaceSection(PipelineWorkspaceAction.OPEN_COMPANIES)
        )
    }

    @Test
    fun opportunitiesActionReturnsToOpportunityWorkspace() {
        assertEquals(
            PipelineWorkspaceSection.OPPORTUNITIES,
            reducePipelineWorkspaceSection(PipelineWorkspaceAction.OPEN_OPPORTUNITIES)
        )
    }
}
