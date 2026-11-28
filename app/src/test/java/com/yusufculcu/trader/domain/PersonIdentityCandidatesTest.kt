package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.OrganizationPersonAliasEntity
import com.yusufculcu.trader.data.OrganizationPersonEntity
import com.yusufculcu.trader.data.PersonAliasType
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
    fun savedEmailAliasMatchesCanonicalEmailAsReviewOnlySignal() {
        val people = listOf(person("a"), person("b", email = "ada@example.com"))
        val before = people.toList()
        val candidates = PersonIdentityCandidates.find(
            people,
            listOf(alias("alias-a", "a", PersonAliasType.EMAIL, " ADA@EXAMPLE.COM "))
        )

        assertEquals(listOf(PersonIdentitySignal.SAME_EMAIL_ALIAS), candidates.single().signals)
        assertEquals(before, people)
    }

    @Test
    fun savedProfileAliasMatchesCanonicalProfileAfterNormalization() {
        val candidates = PersonIdentityCandidates.find(
            listOf(person("a"), person("b", profile = "https://linkedin.com/in/ada-example")),
            listOf(alias("alias-a", "a", PersonAliasType.PROFILE_URL, "https://www.linkedin.com/in/Ada-Example/?trk=public"))
        )

        assertEquals(listOf(PersonIdentitySignal.SAME_PROFILE_ALIAS), candidates.single().signals)
    }

    @Test
    fun savedNameAliasIsAWeakSignalEvenWhenRolesDiffer() {
        val candidates = PersonIdentityCandidates.find(
            listOf(
                person("a", name = "A. Example", role = "Engineer"),
                person("b", name = "Ada Example", role = "Buyer")
            ),
            listOf(alias("alias-a", "a", PersonAliasType.NAME, "Ada Example"))
        )

        assertEquals(listOf(PersonIdentitySignal.SAME_NAME_ALIAS), candidates.single().signals)
    }

    @Test
    fun aliasesCanMatchEachOtherWithoutChangingCanonicalContacts() {
        val people = listOf(person("a"), person("b"))
        val candidates = PersonIdentityCandidates.find(
            people,
            listOf(
                alias("alias-a", "a", PersonAliasType.EMAIL, "ada@example.com"),
                alias("alias-b", "b", PersonAliasType.EMAIL, " ADA@EXAMPLE.COM ")
            )
        )

        assertEquals(listOf(PersonIdentitySignal.SAME_EMAIL_ALIAS), candidates.single().signals)
        assertTrue(people.all { it.email == null })
    }

    @Test
    fun aliasesMustBelongToTheirPersonAndCompanyAndOtherAliasesAreIgnored() {
        val people = listOf(person("a"), person("b"))
        val invalidOrNonIdentityAliases = listOf(
            alias("wrong-company", "a", PersonAliasType.EMAIL, "shared@example.com", organization = "other"),
            alias("wrong-owner", "missing-person", PersonAliasType.EMAIL, "shared@example.com"),
            alias("other-a", "a", PersonAliasType.OTHER, "shared@example.com"),
            alias("other-b", "b", PersonAliasType.OTHER, "shared@example.com"),
            alias("unsafe-profile-a", "a", PersonAliasType.PROFILE_URL, "http://linkedin.com/in/ada"),
            alias("unsafe-profile-b", "b", PersonAliasType.PROFILE_URL, "https://linkedin.com/in/ada")
        )

        assertTrue(PersonIdentityCandidates.find(people, invalidOrNonIdentityAliases).isEmpty())
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

    private fun alias(
        id: String,
        personId: String,
        type: String,
        value: String,
        organization: String = "org"
    ) = OrganizationPersonAliasEntity(
        id = id,
        personId = personId,
        organizationId = organization,
        aliasType = type,
        value = value,
        normalizedValue = value.trim().lowercase(),
        sourceId = null,
        provenanceKind = "OPERATOR_ENTERED",
        note = null,
        createdAt = 1
    )
}
