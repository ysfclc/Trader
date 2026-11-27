package com.yusufculcu.trader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineWorkspaceNavigationTest {

    @Test
    fun openingCompaniesFromSourcingSelectsCompaniesWorkspace() {
        val section = reducePipelineWorkspaceSection(
            current = PipelineWorkspaceSection.OPPORTUNITIES,
            action = PipelineWorkspaceAction.OPEN_COMPANIES
        )

        assertEquals(PipelineWorkspaceSection.COMPANIES, section)
    }

    @Test
    fun openingCompaniesFromOpportunitiesDoesNotDependOnPreviousSelection() {
        val section = reducePipelineWorkspaceSection(
            current = PipelineWorkspaceSection.OPPORTUNITIES,
            action = PipelineWorkspaceAction.OPEN_COMPANIES
        )

        assertEquals(PipelineWorkspaceSection.COMPANIES, section)
    }

    @Test
    fun selectingOpportunitiesReturnsToOpportunityWorkspace() {
        val section = reducePipelineWorkspaceSection(
            current = PipelineWorkspaceSection.COMPANIES,
            action = PipelineWorkspaceAction.OPEN_OPPORTUNITIES
        )

        assertEquals(PipelineWorkspaceSection.OPPORTUNITIES, section)
    }
}
