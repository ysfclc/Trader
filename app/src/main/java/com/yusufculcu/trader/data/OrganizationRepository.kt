package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.discovery.DiscoveryDocument
import kotlinx.coroutines.flow.Flow
import java.net.URI
import java.util.Locale
import java.util.UUID

object OrganizationSourceKind {
    const val DISCOVERY_RESULT = "DISCOVERY_RESULT"
    const val MANUAL_REFERENCE = "MANUAL_REFERENCE"
    const val BROWSER_CAPTURE = "BROWSER_CAPTURE"
    val ALL = setOf(DISCOVERY_RESULT, MANUAL_REFERENCE, BROWSER_CAPTURE)
}

object OrganizationClaimState {
    const val UNREVIEWED = "UNREVIEWED"
    const val SUPPORTED = "SUPPORTED"
    const val CONTRADICTED = "CONTRADICTED"
    const val REJECTED = "REJECTED"
    val ALL = setOf(UNREVIEWED, SUPPORTED, CONTRADICTED, REJECTED)
}

data class OrganizationPersonDraft(val fullName: String, val role: String = "", val email: String = "", val phone: String = "", val profileUrl: String = "", val notes: String = "")
data class OrganizationPersonAliasDraft(val aliasType: String, val value: String, val sourceId: String? = null, val note: String = "")
object PersonAliasType { const val NAME = "NAME"; const val EMAIL = "EMAIL"; const val PROFILE_URL = "PROFILE_URL"; const val OTHER = "OTHER"; val ALL = setOf(NAME, EMAIL, PROFILE_URL, OTHER) }
object PersonAliasProvenance { const val OPERATOR_ENTERED = "OPERATOR_ENTERED"; const val SOURCE_LINKED = "SOURCE_LINKED"; val ALL = setOf(OPERATOR_ENTERED, SOURCE_LINKED) }
object PersonIdentityDecisionKind { const val SAME_PERSON = "SAME_PERSON"; const val DISTINCT_PEOPLE = "DISTINCT_PEOPLE"; const val REOPENED = "REOPENED"; val ALL = setOf(SAME_PERSON, DISTINCT_PEOPLE, REOPENED) }

data class OrganizationDraft(
    val canonicalName: String,
    val websiteUrl: String = "",
    val country: String = "",
    val city: String = "",
    val industry: String = "",
    val notes: String = ""
)

data class OrganizationSourceDraft(
    val sourceKind: String,
    val title: String,
    val url: String,
    val excerpt: String = "",
    val capturedAt: Long = System.currentTimeMillis()
)

data class OrganizationClaimDraft(
    val sourceId: String,
    val field: String,
    val statement: String
)

/**
 * Local-first organization dossier storage. Sources are bounded snapshots.
 * Claims are separate source-linked user-authored statements and always begin
 * UNREVIEWED; no model output can mark a claim supported automatically.
 */
class OrganizationRepository(private val database: TraderDatabase) {
    fun buyerRequirementRepository(): BuyerRequirementRepository = BuyerRequirementRepository(database)
    fun productCatalogRepository(): ProductCatalogRepository = ProductCatalogRepository(database)

    fun observeOrganizations(): Flow<List<OrganizationEntity>> = database.organizationDao().observeAll()
    fun observeOrganization(id: String): Flow<OrganizationEntity?> = database.organizationDao().observeById(id)
    fun observeSources(organizationId: String): Flow<List<OrganizationSourceEntity>> =
        database.organizationSourceDao().observeForOrganization(organizationId)
    fun observeClaims(organizationId: String): Flow<List<OrganizationClaimEntity>> =
        database.organizationClaimDao().observeForOrganization(organizationId)
    fun observePeople(organizationId: String): Flow<List<OrganizationPersonEntity>> = database.organizationPersonDao().observeForOrganization(organizationId)
    fun observePersonAliases(organizationId: String): Flow<List<OrganizationPersonAliasEntity>> = database.organizationPersonAliasDao().observeForOrganization(organizationId)
    fun observeIdentityDecisions(organizationId: String): Flow<List<OrganizationPersonIdentityDecisionEntity>> = database.organizationPersonIdentityDecisionDao().observeForOrganization(organizationId)
    suspend fun addPersonAlias(personId: String, draft: OrganizationPersonAliasDraft): OrganizationPersonAliasEntity = database.withTransaction {
        val person = database.organizationPersonDao().getById(personId) ?: throw IllegalArgumentException("Contact no longer exists.")
        require(draft.aliasType in PersonAliasType.ALL) { "Unsupported alias type." }
        val value = cleanRequired(draft.value, 512, "Alias")
        val normalized = when (draft.aliasType) {
            PersonAliasType.NAME -> normalizeAliasText(value)
            PersonAliasType.EMAIL -> value.lowercase(Locale.ROOT).also { require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it)) { "Alias email is invalid." } }
            PersonAliasType.PROFILE_URL -> normalizeAliasProfile(value)
            else -> normalizeAliasText(value)
        }
        val source = draft.sourceId?.let { database.organizationSourceDao().getById(it) ?: throw IllegalArgumentException("Alias source does not exist.") }
        require(source == null || source.organizationId == person.organizationId) { "An alias can only use a source attached to the same company dossier." }
        val now = System.currentTimeMillis()
        val alias = OrganizationPersonAliasEntity(UUID.randomUUID().toString(), person.id, person.organizationId, draft.aliasType, value, normalized, source?.id, if (source == null) PersonAliasProvenance.OPERATOR_ENTERED else PersonAliasProvenance.SOURCE_LINKED, cleanOptional(draft.note, 1000, "Alias note"), now)
        database.organizationPersonAliasDao().insert(alias)
        database.organizationDao().touch(person.organizationId, now)
        alias
    }
    suspend fun deletePersonAlias(aliasId: String): Boolean = database.withTransaction { database.organizationPersonAliasDao().deleteById(aliasId) > 0 }
    suspend fun reviewPersonIdentity(firstPersonId: String, secondPersonId: String, decision: String, rationale: String = "", sourceId: String? = null): OrganizationPersonIdentityDecisionEntity = database.withTransaction {
        require(decision in PersonIdentityDecisionKind.ALL) { "Unsupported identity review decision." }
        require(firstPersonId != secondPersonId) { "Choose two different contacts." }
        require(rationale.length <= 1000) { "Identity review note is too long." }
        val first = database.organizationPersonDao().getById(firstPersonId) ?: throw IllegalArgumentException("First contact no longer exists.")
        val second = database.organizationPersonDao().getById(secondPersonId) ?: throw IllegalArgumentException("Second contact no longer exists.")
        require(first.organizationId == second.organizationId) { "Contacts must belong to the same company." }
        val source = sourceId?.let { database.organizationSourceDao().getById(it) ?: throw IllegalArgumentException("Review source does not exist.") }
        require(source == null || source.organizationId == first.organizationId) { "Identity review evidence must belong to the same company." }
        val latestReviewedAt = database.organizationPersonIdentityDecisionDao().getAll().asSequence().filter { it.organizationId == first.organizationId }.maxOfOrNull { it.reviewedAt } ?: -1L
        val now = maxOf(System.currentTimeMillis(), latestReviewedAt + 1L)
        val row = OrganizationPersonIdentityDecisionEntity(UUID.randomUUID().toString(), first.organizationId, minOf(first.id, second.id), maxOf(first.id, second.id), decision, rationale.trim().ifBlank { null }, source?.id, now)
        database.organizationPersonIdentityDecisionDao().append(row)
        database.organizationDao().touch(first.organizationId, now)
        row
    }
    private fun normalizeAliasText(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
    private fun normalizeAliasProfile(value: String): String {
        val uri = runCatching { URI(value.trim()) }.getOrElse { throw IllegalArgumentException("Alias profile URL is invalid.") }
        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: throw IllegalArgumentException("Alias profile URL is invalid.")
        require(host == "linkedin.com" || host.endsWith(".linkedin.com")) { "Profile alias must point to linkedin.com." }
        val path = uri.path.orEmpty().trimEnd('/').lowercase(Locale.ROOT)
        require(uri.scheme.equals("https", true) && path.isNotBlank() && uri.userInfo == null) { "Alias profile URL must be a public HTTPS LinkedIn profile." }
        return "https://$host$path"
    }
    suspend fun addPerson(organizationId: String, draft: OrganizationPersonDraft): OrganizationPersonEntity = database.withTransaction {
        val organization = database.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val now = System.currentTimeMillis()
        val row = buildPerson(organizationId, draft, UUID.randomUUID().toString(), now)
        database.organizationPersonDao().insert(row)
        database.organizationDao().touch(organizationId, now)
        database.appendBusinessActivity(
            organizationId, organization.canonicalName, BusinessActivityEntityType.CONTACT, row.id,
            row.role ?: "Contact", BusinessActivityAction.CREATED, "Contact added",
            details = "A company contact record was added; personal contact details are not copied into activity history.",
            occurredAt = now
        )
        row
    }
    suspend fun updatePerson(personId: String, draft: OrganizationPersonDraft): OrganizationPersonEntity = database.withTransaction {
        val old = database.organizationPersonDao().getById(personId)
            ?: throw IllegalArgumentException("Contact no longer exists.")
        val organization = database.organizationDao().getById(old.organizationId)
            ?: throw IllegalArgumentException("Company dossier no longer exists.")
        val now = System.currentTimeMillis()
        val row = buildPerson(old.organizationId, draft, old.id, old.createdAt).copy(updatedAt = now)
        database.organizationPersonDao().update(row)
        database.organizationDao().touch(old.organizationId, now)
        database.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.CONTACT, row.id,
            row.role ?: "Contact", BusinessActivityAction.UPDATED, "Contact updated",
            details = "Contact details changed; personal fields are not copied into activity history.",
            occurredAt = now
        )
        row
    }
    suspend fun deletePerson(personId: String): Boolean = database.withTransaction {
        val old = database.organizationPersonDao().getById(personId) ?: return@withTransaction false
        val organization = database.organizationDao().getById(old.organizationId)
            ?: return@withTransaction false
        val now = System.currentTimeMillis()
        database.appendBusinessActivity(
            old.organizationId, organization.canonicalName, BusinessActivityEntityType.CONTACT, old.id,
            old.role ?: "Contact", BusinessActivityAction.DELETED, "Contact deleted",
            details = "Contact record deleted; personal fields are not copied into activity history.",
            occurredAt = now
        )
        database.organizationPersonDao().deleteById(personId)
        database.organizationDao().touch(old.organizationId, now)
        true
    }
    suspend fun findByDomain(domain: String): List<OrganizationEntity> {
        val normalized = domain.trim().lowercase(Locale.ROOT).removePrefix("www.")
        if (normalized.isBlank()) return emptyList()
        return database.organizationDao().findByDomain(normalized)
    }

    suspend fun createOrganization(draft: OrganizationDraft): OrganizationEntity = database.withTransaction {
        val now = System.currentTimeMillis()
        val row = buildOrganization(draft, UUID.randomUUID().toString(), now)
        database.organizationDao().insert(row)
        database.appendBusinessActivity(
            row.id, row.canonicalName, BusinessActivityEntityType.ORGANIZATION, row.id,
            row.canonicalName, BusinessActivityAction.CREATED, "Company dossier created",
            occurredAt = now
        )
        row
    }
    suspend fun createFromDiscovery(draft: OrganizationDraft, document: DiscoveryDocument): OrganizationEntity = database.withTransaction {
        val now = System.currentTimeMillis()
        val organization = buildOrganization(draft, UUID.randomUUID().toString(), now)
        val sourceTitle = document.title.trim().take(300).ifBlank { document.sourceName.trim().take(300) }
        val source = buildSource(
            organization.id,
            OrganizationSourceDraft(
                sourceKind = OrganizationSourceKind.DISCOVERY_RESULT,
                title = sourceTitle.ifBlank { "Discovery result" },
                url = document.url,
                excerpt = document.summary.take(8_000),
                capturedAt = document.retrievedAt.coerceAtLeast(0)
            )
        )
        database.organizationDao().insert(organization)
        database.organizationSourceDao().insert(source)
        database.appendBusinessActivity(
            organization.id, organization.canonicalName, BusinessActivityEntityType.ORGANIZATION, organization.id,
            organization.canonicalName, BusinessActivityAction.CREATED, "Company dossier created",
            details = "Created from a discovery result; the saved source remains unreviewed.",
            occurredAt = now
        )
        database.appendBusinessActivity(
            organization.id, organization.canonicalName, BusinessActivityEntityType.ORGANIZATION, organization.id,
            source.title, BusinessActivityAction.SOURCE_ADDED, "Discovery source saved",
            details = "The discovery result is research provenance, not identity verification.",
            occurredAt = now
        )
        organization
    }
    suspend fun updateOrganization(id: String, draft: OrganizationDraft): OrganizationEntity = database.withTransaction {
        val existing = database.organizationDao().getById(id)
            ?: throw IllegalArgumentException("Company dossier no longer exists.")
        val normalized = normalizeWebsite(draft.websiteUrl)
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            canonicalName = cleanRequired(draft.canonicalName, 200, "Company name"),
            websiteUrl = normalized.first,
            domain = normalized.second,
            country = cleanOptional(draft.country, 120, "Country"),
            city = cleanOptional(draft.city, 120, "City"),
            industry = cleanOptional(draft.industry, 160, "Industry"),
            notes = cleanOptional(draft.notes, 4_000, "Notes"),
            updatedAt = now
        )
        database.organizationDao().update(updated)
        database.appendBusinessActivity(
            updated.id, updated.canonicalName, BusinessActivityEntityType.ORGANIZATION, updated.id,
            updated.canonicalName, BusinessActivityAction.UPDATED, "Company profile updated",
            details = "Company profile fields were edited; prior activity entries remain unchanged.",
            occurredAt = now
        )
        updated
    }
    suspend fun addSource(organizationId: String, draft: OrganizationSourceDraft): OrganizationSourceEntity = database.withTransaction {
        val organization = database.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val normalized = buildSource(organizationId, draft)
        val existing = database.organizationSourceDao().findByOrganizationAndUrl(organizationId, normalized.url)
        if (existing != null) return@withTransaction existing
        database.organizationSourceDao().insert(normalized)
        val now = System.currentTimeMillis()
        database.organizationDao().touch(organizationId, now)
        database.appendBusinessActivity(
            organizationId, organization.canonicalName, BusinessActivityEntityType.ORGANIZATION, organizationId,
            normalized.title, BusinessActivityAction.SOURCE_ADDED, "Company source saved",
            details = "A source snapshot was attached to the company dossier; its claims remain subject to review.",
            occurredAt = now
        )
        normalized
    }
    suspend fun addClaim(organizationId: String, draft: OrganizationClaimDraft): OrganizationClaimEntity = database.withTransaction {
        val organization = database.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val source = database.organizationSourceDao().getById(draft.sourceId)
            ?: throw IllegalArgumentException("Select a saved source for this claim.")
        require(source.organizationId == organizationId) {
            "A claim can only use a source attached to the same company dossier."
        }
        val now = System.currentTimeMillis()
        val claim = OrganizationClaimEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            sourceId = source.id,
            field = cleanRequired(draft.field, 80, "Claim field"),
            statement = cleanRequired(draft.statement, 2_000, "Claim statement"),
            reviewState = OrganizationClaimState.UNREVIEWED,
            reviewNote = null,
            reviewedAt = null,
            createdAt = now,
            updatedAt = now
        )
        database.organizationClaimDao().insert(claim)
        database.organizationDao().touch(organizationId, now)
        database.appendBusinessActivity(
            organizationId, organization.canonicalName, BusinessActivityEntityType.CLAIM, claim.id,
            claim.field, BusinessActivityAction.CREATED, "Source-linked claim added",
            toStatus = OrganizationClaimState.UNREVIEWED,
            details = "Claim created from a saved company source and left unreviewed.",
            occurredAt = now
        )
        claim
    }
    suspend fun updateClaim(claimId: String, draft: OrganizationClaimDraft): OrganizationClaimEntity = database.withTransaction {
        val existing = database.organizationClaimDao().getById(claimId)
            ?: throw IllegalArgumentException("Company claim no longer exists.")
        val organization = database.organizationDao().getById(existing.organizationId)
            ?: throw IllegalArgumentException("Company dossier no longer exists.")
        val source = database.organizationSourceDao().getById(draft.sourceId)
            ?: throw IllegalArgumentException("Select a saved source for this claim.")
        require(source.organizationId == existing.organizationId) {
            "A claim can only use a source attached to the same company dossier."
        }
        val field = cleanRequired(draft.field, 80, "Claim field")
        val statement = cleanRequired(draft.statement, 2_000, "Claim statement")
        if (existing.sourceId == source.id && existing.field == field && existing.statement == statement) {
            return@withTransaction existing
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            sourceId = source.id,
            field = field,
            statement = statement,
            reviewState = OrganizationClaimState.UNREVIEWED,
            reviewNote = null,
            reviewedAt = null,
            updatedAt = now
        )
        database.organizationClaimDao().update(updated)
        database.organizationDao().touch(existing.organizationId, now)
        database.appendBusinessActivity(
            existing.organizationId, organization.canonicalName, BusinessActivityEntityType.CLAIM, existing.id,
            updated.field, BusinessActivityAction.UPDATED, "Company claim corrected",
            fromStatus = existing.reviewState, toStatus = OrganizationClaimState.UNREVIEWED,
            details = "Claim content or source association was corrected; the previous review was cleared and must be repeated.",
            occurredAt = now
        )
        updated
    }

    suspend fun reviewClaim(claimId: String, state: String, note: String = ""): Boolean = database.withTransaction {
        require(state in OrganizationClaimState.ALL) { "Unsupported claim review state." }
        require(note.length <= 1_000) { "Review note is too long." }
        val existing = database.organizationClaimDao().getById(claimId) ?: return@withTransaction false
        val organization = database.organizationDao().getById(existing.organizationId)
            ?: return@withTransaction false
        val cleanNote = note.trim().ifBlank { null }
        if (existing.reviewState == state && existing.reviewNote == cleanNote) return@withTransaction true
        val now = System.currentTimeMillis()
        database.organizationClaimDao().update(
            existing.copy(
                reviewState = state,
                reviewNote = cleanNote,
                reviewedAt = if (state == OrganizationClaimState.UNREVIEWED) null else now,
                updatedAt = now
            )
        )
        database.organizationDao().touch(existing.organizationId, now)
        database.appendBusinessActivity(
            existing.organizationId, organization.canonicalName, BusinessActivityEntityType.CLAIM, existing.id,
            existing.field, BusinessActivityAction.CLAIM_REVIEWED, "Company claim review changed",
            fromStatus = existing.reviewState, toStatus = state, details = cleanNote.orEmpty(),
            occurredAt = now
        )
        true
    }
    suspend fun deleteOrganization(id: String): Boolean = database.withTransaction {
        val organization = database.organizationDao().getById(id) ?: return@withTransaction false
        val hasBuyerRequirements = database.buyerRequirementDao().getAll().any { it.organizationId == id }
        val hasSupplierOffers = database.supplierOfferDao().getAll().any { it.organizationId == id }
        val hasProducts = database.productDao().getAll().any { it.organizationId == id }
        require(!hasBuyerRequirements && !hasSupplierOffers && !hasProducts) {
            "Cannot delete this company dossier while it has buyer requirements, supplier offers or product catalogue records; this prevents cascading loss of recoverable commercial data."
        }
        val now = System.currentTimeMillis()
        database.appendBusinessActivity(
            organization.id, organization.canonicalName, BusinessActivityEntityType.ORGANIZATION, organization.id,
            organization.canonicalName, BusinessActivityAction.DELETED, "Company dossier deleted",
            details = "Company records were deleted; this activity entry is retained as a minimal historical marker.",
            occurredAt = now
        )
        database.organizationDao().deleteById(id) > 0
    }
    private fun buildPerson(organizationId: String, draft: OrganizationPersonDraft, id: String, now: Long): OrganizationPersonEntity {
        val name = cleanRequired(draft.fullName, 160, "Contact name")
        val role = cleanOptional(draft.role, 160, "Role")
        val email = cleanOptional(draft.email, 254, "Email")?.also { require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it)) { "Email address is invalid." } }
        val phone = cleanOptional(draft.phone, 64, "Phone")
        val profile = cleanOptional(draft.profileUrl, 2048, "Profile URL")?.also { val host = URI(normalizeHttpsUrl(it, "Profile URL")).host; require(host.equals("linkedin.com", true) || host.endsWith(".linkedin.com", true)) { "Profile URL must point to linkedin.com." } }
        return OrganizationPersonEntity(id, organizationId, name, role, email, phone, profile, cleanOptional(draft.notes, 2000, "Contact notes"), now, now)
    }

    private fun buildOrganization(draft: OrganizationDraft, id: String, now: Long): OrganizationEntity {
        val normalized = normalizeWebsite(draft.websiteUrl)
        return OrganizationEntity(
            id = id,
            canonicalName = cleanRequired(draft.canonicalName, 200, "Company name"),
            websiteUrl = normalized.first,
            domain = normalized.second,
            country = cleanOptional(draft.country, 120, "Country"),
            city = cleanOptional(draft.city, 120, "City"),
            industry = cleanOptional(draft.industry, 160, "Industry"),
            notes = cleanOptional(draft.notes, 4_000, "Notes"),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun buildSource(organizationId: String, draft: OrganizationSourceDraft): OrganizationSourceEntity {
        require(draft.sourceKind in OrganizationSourceKind.ALL) { "Unsupported source type." }
        require(draft.capturedAt >= 0) { "Source timestamp is invalid." }
        val title = cleanRequired(draft.title, 300, "Source title")
        val url = normalizeHttpsUrl(draft.url, "Source URL")
        require(draft.excerpt.length <= 8_000) { "Source excerpt is too long." }
        return OrganizationSourceEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            sourceKind = draft.sourceKind,
            title = title,
            url = url,
            excerpt = draft.excerpt.trim(),
            capturedAt = draft.capturedAt
        )
    }

    private fun normalizeWebsite(raw: String): Pair<String?, String?> {
        if (raw.isBlank()) return null to null
        val normalized = normalizeHttpsUrl(raw, "Company website")
        val host = URI(normalized).host.lowercase(Locale.ROOT).removePrefix("www.")
        return normalized to host
    }

    private fun normalizeHttpsUrl(raw: String, label: String): String {
        val uri = runCatching { URI(raw.trim()) }
            .getOrElse { throw IllegalArgumentException("$label must be a valid HTTPS URL.") }
        require(uri.scheme.equals("https", ignoreCase = true) && uri.host != null && uri.userInfo == null) {
            "$label must use HTTPS and cannot contain credentials."
        }
        val host = uri.host.lowercase(Locale.ROOT).removeSuffix(".")
        require(host.isNotBlank() && host.length <= 253 && !host.contains(":")) { "$label contains an invalid host." }
        require(host != "localhost" && !host.endsWith(".local") && !host.endsWith(".internal") && !isPrivateIpv4(host)) {
            "$label must point to a public host."
        }
        val port = if (uri.port == -1 || uri.port == 443) "" else ":" + uri.port
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        val query = uri.rawQuery?.let { "?" + it } ?: ""
        val normalized = "https://" + host + port + path + query
        require(normalized.length <= 2_048) { "$label is too long." }
        return normalized
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        val a = octets[0]
        val b = octets[1]
        return a == 0 || a == 10 || a == 127 || (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) || (a == 192 && b == 168) || a >= 224
    }

    private fun cleanRequired(value: String, maxLength: Int, label: String): String {
        val clean = value.trim()
        require(clean.isNotBlank() && clean.length <= maxLength) { "$label is required and must be at most $maxLength characters." }
        return clean
    }

    private fun cleanOptional(value: String, maxLength: Int, label: String): String? {
        val clean = value.trim()
        require(clean.length <= maxLength) { "$label is too long." }
        return clean.ifBlank { null }
    }
}
