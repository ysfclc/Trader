package com.yusufculcu.trader.domain

import com.yusufculcu.trader.data.OrganizationPersonEntity
import java.net.URI
import java.text.Normalizer
import java.util.Locale

enum class PersonIdentitySignal {
    SAME_EMAIL,
    SAME_PROFILE,
    SAME_NAME_AND_ROLE
}

data class PersonIdentityCandidate(
    val firstPersonId: String,
    val secondPersonId: String,
    val signals: List<PersonIdentitySignal>
)

/**
 * Produces conservative, review-only duplicate suggestions inside one company.
 * This helper never changes records or asserts that two contacts are one person.
 */
object PersonIdentityCandidates {
    fun find(people: List<OrganizationPersonEntity>): List<PersonIdentityCandidate> {
        val ordered = people.sortedBy { it.id }
        val candidates = mutableListOf<PersonIdentityCandidate>()
        for (i in ordered.indices) {
            for (j in i + 1 until ordered.size) {
                val first = ordered[i]
                val second = ordered[j]
                if (first.organizationId != second.organizationId) continue

                val signals = buildList {
                    val firstEmail = normalizeEmail(first.email)
                    val secondEmail = normalizeEmail(second.email)
                    if (firstEmail != null && firstEmail == secondEmail) add(PersonIdentitySignal.SAME_EMAIL)

                    val firstProfile = normalizeProfile(first.profileUrl)
                    val secondProfile = normalizeProfile(second.profileUrl)
                    if (firstProfile != null && firstProfile == secondProfile) add(PersonIdentitySignal.SAME_PROFILE)

                    val firstName = normalizeText(first.fullName)
                    val secondName = normalizeText(second.fullName)
                    val firstRole = normalizeText(first.role)
                    val secondRole = normalizeText(second.role)
                    if (firstName.isNotBlank() && firstName == secondName &&
                        firstRole.isNotBlank() && firstRole == secondRole
                    ) add(PersonIdentitySignal.SAME_NAME_AND_ROLE)
                }
                if (signals.isNotEmpty()) {
                    candidates += PersonIdentityCandidate(first.id, second.id, signals)
                }
            }
        }
        return candidates.sortedWith(compareBy({ it.firstPersonId }, { it.secondPersonId }))
    }

    private fun normalizeEmail(value: String?): String? =
        value?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }

    private fun normalizeProfile(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val uri = URI(raw)
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
}
