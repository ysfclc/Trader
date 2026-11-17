package com.yusufculcu.trader.domain

enum class ProblemStatus(val label: String) {
    OPEN("Open"), IN_PROGRESS("In progress"), WAITING("Waiting"), RESOLVED("Resolved"), VERIFIED("Verified");
    companion object {
        fun from(value: String): ProblemStatus = entries.firstOrNull { it.name == value } ?: OPEN
    }
}

object ProblemRules {
    fun canTransition(from: ProblemStatus, to: ProblemStatus): Boolean {
        if (from == to) return true
        return when (from) {
            ProblemStatus.OPEN -> to == ProblemStatus.IN_PROGRESS || to == ProblemStatus.WAITING
            ProblemStatus.IN_PROGRESS -> to == ProblemStatus.WAITING || to == ProblemStatus.RESOLVED || to == ProblemStatus.OPEN
            ProblemStatus.WAITING -> to == ProblemStatus.IN_PROGRESS || to == ProblemStatus.OPEN
            ProblemStatus.RESOLVED -> to == ProblemStatus.IN_PROGRESS || to == ProblemStatus.VERIFIED
            ProblemStatus.VERIFIED -> to == ProblemStatus.IN_PROGRESS
        }
    }
    fun normalizeTitle(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(120)
}
