package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.OrganizationPersonEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonIdentityCandidatesTest {
    @Test
    fun exactNormalizedEmailCreatesReviewSuggestionWithoutIdentityAssertion() {
        val candidates = PersonIdentityCandidates.find(listOf(
            person("a", email = " ADA@EXAMPLE.COM "),
            person("b", email = "ada@example.com")
        ))
        assertEquals(1, candidates.size)
        assertEquals(listOf(PersonIdentitySignal.SAME_EMAIL), candidates.single().signals)
    }

    @Test
    fun linkedinProfileNormalizationIgnoresHostCaseQueryAndTrailingSlash() {
        val candidates = PersonIdentityCandidates.find(listOf(
            person("a", profile = "https://www.linkedin.com/in/Ada-Example/?trk=public"),
            person("b", profile = "https://linkedin.com/in/ada-example")
        ))
        assertEquals(listOf(PersonIdentitySignal.SAME_PROFILE), candidates.single().signals)
    }

    @Test
    fun sameNameAndRoleIsOnlyAWeakSuggestion() {
        val candidates = PersonIdentityCandidates.find(listOf(
            person("a", name = "Ada  Example", role = "Purchasing Manager"),
            person("b", name = "ADA EXAMPLE", role = " purchasing-manager ")
        ))
        assertEquals(listOf(PersonIdentitySignal.SAME_NAME_AND_ROLE), candidates.single().signals)
    }

    @Test
    fun commonNameWithoutAnotherSignalDoesNotCreateCandidate() {
        assertTrue(PersonIdentityCandidates.find(listOf(
            person("a", name = "Alex Smith", role = "Buyer"),
            person("b", name = "Alex Smith", role = "Sales Manager")
        )).isEmpty())
    }

    @Test
    fun differentCompaniesAreNeverCompared() {
        assertTrue(PersonIdentityCandidates.find(listOf(
            person("a", organization = "org-a", email = "same@example.com"),
            person("b", organization = "org-b", email = "same@example.com")
        )).isEmpty())
    }

    @Test
    fun resultsAreStableAndCanExposeMultipleIndependentSignals() {
        val candidates = PersonIdentityCandidates.find(listOf(
            person("z", name = "Ada Example", role = "Buyer", email = "ada@example.com", profile = "https://linkedin.com/in/ada"),
            person("a", name = "ADA EXAMPLE", role = "Buyer", email = "ada@example.com", profile = "https://www.linkedin.com/in/ada/")
        ))
        assertEquals("a", candidates.single().firstPersonId)
        assertEquals("z", candidates.single().secondPersonId)
        assertEquals(
            listOf(PersonIdentitySignal.SAME_EMAIL, PersonIdentitySignal.SAME_PROFILE, PersonIdentitySignal.SAME_NAME_AND_ROLE),
            candidates.single().signals
        )
    }

    private fun person(
        id: String,
        organization: String = "org",
        name: String = "Contact",
        role: String? = null,
        email: String? = null,
        profile: String? = null
    ) = OrganizationPersonEntity(
        id = id, organizationId = organization, fullName = name, role = role,
        email = email, phone = null, profileUrl = profile, notes = null,
        createdAt = 1, updatedAt = 1
    )
}
