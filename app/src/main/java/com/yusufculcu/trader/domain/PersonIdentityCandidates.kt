package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.OrganizationPersonAliasEntity
import com.yusufculcu.trader.data.OrganizationPersonEntity
import com.yusufculcu.trader.data.PersonAliasType
import java.net.URI
import java.text.Normalizer
import java.util.Locale

enum class PersonIdentitySignal {
    SAME_EMAIL,
    SAME_PROFILE,
    SAME_NAME_AND_ROLE,
    SAME_EMAIL_ALIAS,
    SAME_PROFILE_ALIAS,
    SAME_NAME_ALIAS
}

data class PersonIdentityCandidate(
    val firstPersonId: String,
    val secondPersonId: String,
    val signals: List<PersonIdentitySignal>
)

/**
 * Produces conservative, review-only duplicate suggestions inside one company.
 * Saved aliases are additional signals, never identity assertions or merge commands.
 */
object PersonIdentityCandidates {
    fun find(
        people: List<OrganizationPersonEntity>,
        aliases: List<OrganizationPersonAliasEntity> = emptyList()
    ): List<PersonIdentityCandidate> {
        val ordered = people.sortedBy { it.id }
        val peopleById = ordered.associateBy { it.id }
        val aliasesByPerson = aliases.asSequence()
            .filter { alias ->
                val person = peopleById[alias.personId]
                person != null && person.organizationId == alias.organizationId
            }
            .groupBy { it.personId }

        val candidates = mutableListOf<PersonIdentityCandidate>()
        for (i in ordered.indices) {
            for (j in i + 1 until ordered.size) {
                val first = ordered[i]
                val second = ordered[j]
                if (first.organizationId != second.organizationId) continue

                val firstAliases = aliasesByPerson[first.id].orEmpty()
                val secondAliases = aliasesByPerson[second.id].orEmpty()
                val firstEmailAliases = firstAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.EMAIL }
                    .mapNotNull { normalizeEmail(it.value) }
                    .toSet()
                val secondEmailAliases = secondAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.EMAIL }
                    .mapNotNull { normalizeEmail(it.value) }
                    .toSet()
                val firstProfileAliases = firstAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.PROFILE_URL }
                    .mapNotNull { normalizeProfile(it.value) }
                    .toSet()
                val secondProfileAliases = secondAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.PROFILE_URL }
                    .mapNotNull { normalizeProfile(it.value) }
                    .toSet()
                val firstNameAliases = firstAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.NAME }
                    .map { normalizeText(it.value) }
                    .filter { it.isNotBlank() }
                    .toSet()
                val secondNameAliases = secondAliases.asSequence()
                    .filter { it.aliasType == PersonAliasType.NAME }
                    .map { normalizeText(it.value) }
                    .filter { it.isNotBlank() }
                    .toSet()

                val signals = buildList {
                    val firstEmail = normalizeEmail(first.email)
                    val secondEmail = normalizeEmail(second.email)
                    if (firstEmail != null && firstEmail == secondEmail) add(PersonIdentitySignal.SAME_EMAIL)
                    if (hasAliasMatch(firstEmail, firstEmailAliases, secondEmail, secondEmailAliases)) {
                        add(PersonIdentitySignal.SAME_EMAIL_ALIAS)
                    }

                    val firstProfile = normalizeProfile(first.profileUrl)
                    val secondProfile = normalizeProfile(second.profileUrl)
                    if (firstProfile != null && firstProfile == secondProfile) add(PersonIdentitySignal.SAME_PROFILE)
                    if (hasAliasMatch(firstProfile, firstProfileAliases, secondProfile, secondProfileAliases)) {
                        add(PersonIdentitySignal.SAME_PROFILE_ALIAS)
                    }

                    val firstName = normalizeText(first.fullName)
                    val secondName = normalizeText(second.fullName)
                    val firstRole = normalizeText(first.role)
                    val secondRole = normalizeText(second.role)
                    if (firstName.isNotBlank() && firstName == secondName &&
                        firstRole.isNotBlank() && firstRole == secondRole
                    ) add(PersonIdentitySignal.SAME_NAME_AND_ROLE)
                    if (hasAliasMatch(firstName, firstNameAliases, secondName, secondNameAliases)) {
                        add(PersonIdentitySignal.SAME_NAME_ALIAS)
                    }
                }
                if (signals.isNotEmpty()) {
                    candidates += PersonIdentityCandidate(first.id, second.id, signals)
                }
            }
        }
        return candidates.sortedWith(compareBy({ it.firstPersonId }, { it.secondPersonId }))
    }

    private fun hasAliasMatch(
        firstCanonical: String?,
        firstAliases: Set<String>,
        secondCanonical: String?,
        secondAliases: Set<String>
    ): Boolean {
        if (firstAliases.isEmpty() && secondAliases.isEmpty()) return false
        val firstValues = firstAliases + listOfNotNull(firstCanonical)
        val secondValues = secondAliases + listOfNotNull(secondCanonical)
        return firstAliases.any { it in secondValues } || secondAliases.any { it in firstValues }
    }

    private fun normalizeEmail(value: String?): String? {
        val normalized = value?.trim()?.lowercase(Locale.ROOT) ?: return null
        return normalized.takeIf { EMAIL_PATTERN.matches(it) }
    }

    private fun normalizeProfile(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val uri = URI(raw)
            if (!uri.scheme.equals("https", true) || uri.userInfo != null) return null
            val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return null
            if (host != "linkedin.com" && !host.endsWith(".linkedin.com")) return null
            val path = uri.path.orEmpty().trimEnd('/').lowercase(Locale.ROOT)
            if (path.isBlank()) return null
            "https://$host$path"
        }.getOrNull()
    }

    private fun normalizeText(value: String?): String =
        Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
}
