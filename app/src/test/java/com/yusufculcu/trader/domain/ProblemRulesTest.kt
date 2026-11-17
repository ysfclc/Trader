package com.yusufculcu.trader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemRulesTest {
    @Test fun titleNormalizationRemovesWhitespaceNoise() {
        assertEquals("Fix the workflow", ProblemRules.normalizeTitle("  Fix   the\n workflow  "))
    }
    @Test fun openProblemCannotSkipToVerified() {
        assertTrue(ProblemRules.canTransition(ProblemStatus.OPEN, ProblemStatus.IN_PROGRESS))
        assertTrue(ProblemRules.canTransition(ProblemStatus.OPEN, ProblemStatus.WAITING))
        assertFalse(ProblemRules.canTransition(ProblemStatus.OPEN, ProblemStatus.VERIFIED))
    }
    @Test fun resolvedProblemRequiresExplicitVerification() {
        assertTrue(ProblemRules.canTransition(ProblemStatus.RESOLVED, ProblemStatus.VERIFIED))
        assertFalse(ProblemRules.canTransition(ProblemStatus.IN_PROGRESS, ProblemStatus.VERIFIED))
    }
    @Test fun verifiedProblemCanBeReopened() {
        assertTrue(ProblemRules.canTransition(ProblemStatus.VERIFIED, ProblemStatus.IN_PROGRESS))
        assertFalse(ProblemRules.canTransition(ProblemStatus.VERIFIED, ProblemStatus.OPEN))
    }
}
