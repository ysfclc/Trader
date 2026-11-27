package com.yusufculcu.trader.ui

internal enum class PipelineWorkspaceSection {
    OPPORTUNITIES,
    COMPANIES
}

internal enum class PipelineWorkspaceAction {
    OPEN_OPPORTUNITIES,
    OPEN_COMPANIES
}

internal fun reducePipelineWorkspaceSection(
    action: PipelineWorkspaceAction
): PipelineWorkspaceSection = when (action) {
    PipelineWorkspaceAction.OPEN_OPPORTUNITIES -> PipelineWorkspaceSection.OPPORTUNITIES
    PipelineWorkspaceAction.OPEN_COMPANIES -> PipelineWorkspaceSection.COMPANIES
}
