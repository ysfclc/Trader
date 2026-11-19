package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.EvidenceEntity
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a transparent, user-shareable context packet from persisted Trader records.
 * It never invents claims; source text and the user's verification labels are kept explicit.
 */
object ContextPacketBuilder {
    fun build(
        problem: ProblemEntity,
        steps: List<PlanStepEntity>,
        evidence: List<EvidenceEntity>
    ): String = buildString {
        appendLine("# Trader context packet")
        appendLine()
        appendLine("Prepared: ${formatTimestamp(System.currentTimeMillis())}")
        appendLine("Case status: ${problem.status}")
        appendLine("Captured: ${formatTimestamp(problem.createdAt)}")
        appendLine()
        appendLine("## Problem")
        appendLine(problem.title)
        if (problem.description.isNotBlank()) {
            appendLine()
            appendLine("## Context supplied by the user")
            appendLine(problem.description)
        }
        if (problem.desiredOutcome.isNotBlank()) {
            appendLine()
            appendLine("## Desired outcome")
            appendLine(problem.desiredOutcome)
        }
        if (problem.constraintsText.isNotBlank()) {
            appendLine()
            appendLine("## Constraints")
            appendLine(problem.constraintsText)
        }
        if (problem.outcomeNote.isNotBlank()) {
            appendLine()
            appendLine("## Outcome note")
            appendLine(problem.outcomeNote)
        }
        if (problem.originalContent.isNotBlank()) {
            appendLine()
            appendLine("## Original capture")
            appendLine(problem.originalContent)
        }
        if (!problem.sourceUri.isNullOrBlank()) {
            appendLine()
            appendLine("Original source: ${problem.sourceUri}")
        }
        appendLine()
        appendLine("## Plan")
        if (steps.isEmpty()) appendLine("No plan steps have been added.")
        else steps.sortedBy { it.position }.forEachIndexed { index, step ->
            appendLine("${index + 1}. [${step.status}] ${step.title}")
            if (step.rationale.isNotBlank()) appendLine("   Rationale: ${step.rationale}")
            if (step.expectedResult.isNotBlank()) appendLine("   Expected result: ${step.expectedResult}")
            if (step.requiredCapability.isNotBlank()) appendLine("   Required capability: ${step.requiredCapability}")
        }
        appendLine()
        appendLine("## Evidence")
        if (evidence.isEmpty()) appendLine("No separate evidence records have been attached.")
        else evidence.sortedBy { it.capturedAt }.forEachIndexed { index, item ->
            appendLine("${index + 1}. ${item.title} — ${item.verificationState}")
            appendLine("   Captured: ${formatTimestamp(item.capturedAt)} · Type: ${item.sourceKind}")
            if (!item.sourceUri.isNullOrBlank()) appendLine("   Source: ${item.sourceUri}")
            if (item.content.isNotBlank()) appendLine(item.content.prependIndent("   "))
        }
        appendLine()
        appendLine("## Instructions for the receiving assistant")
        appendLine("Help the user make progress toward the desired outcome. First identify missing information and assumptions. Separate supplied facts from hypotheses and recommendations. Do not invent sources, contacts, prices, legal conclusions or capabilities. Propose concrete next actions, explain what evidence would confirm each action, and flag anything that requires the user's decision.")
    }

    private fun formatTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date(timestamp))
}
