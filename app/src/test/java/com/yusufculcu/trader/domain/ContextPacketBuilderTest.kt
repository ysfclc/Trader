package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.EvidenceEntity
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextPacketBuilderTest {
    @Test
    fun packetContainsUserContextPlanSourcesAndEvidenceState() {
        val problem = ProblemEntity(
            id = "case-1",
            title = "Find a replacement supplier",
            description = "Existing supplier missed two delivery dates.",
            desiredOutcome = "Qualify one reliable alternative.",
            constraintsText = "Must deliver to Poland; budget not yet confirmed.",
            category = "General",
            status = "IN_PROGRESS",
            inputKind = "manual",
            originalContent = "Supplier is late again.",
            sourceUri = null,
            outcomeNote = "",
            createdAt = 1L,
            updatedAt = 2L
        )
        val steps = listOf(
            PlanStepEntity("step-1", "case-1", "Collect requirements", "", "", "A written specification", "TODO", 0, 1L, 1L)
        )
        val evidence = listOf(
            EvidenceEntity(
                "evidence-1",
                "case-1",
                "Supplier capability page",
                "Claims five-axis machining.",
                "https://example.test/capabilities",
                "URL",
                "UNREVIEWED",
                3L
            )
        )

        val packet = ContextPacketBuilder.build(problem, steps, evidence)

        assertTrue(packet.contains("Find a replacement supplier"))
        assertTrue(packet.contains("Must deliver to Poland"))
        assertTrue(packet.contains("Collect requirements"))
        assertTrue(packet.contains("A written specification"))
        assertTrue(packet.contains("Supplier capability page — UNREVIEWED"))
        assertTrue(packet.contains("https://example.test/capabilities"))
        assertTrue(packet.contains("Do not invent sources"))
    }
}
