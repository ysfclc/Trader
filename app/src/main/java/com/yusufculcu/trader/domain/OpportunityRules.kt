package com.yusufculcu.trader.domain

enum class OpportunityLane {
    TRADE,
    SERVICE,
    CAREER,
    OTHER
}

enum class OpportunityStage {
    DISCOVERED,
    QUALIFYING,
    READY_TO_CONTACT,
    CONTACTED,
    WAITING,
    WON,
    LOST
}

object OpportunityRules {
    fun isValidLane(value: String): Boolean = OpportunityLane.entries.any { it.name == value }
    fun isValidStage(value: String): Boolean = OpportunityStage.entries.any { it.name == value }
    fun isClosed(value: String): Boolean = value == OpportunityStage.WON.name || value == OpportunityStage.LOST.name
}
