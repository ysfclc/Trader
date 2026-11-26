package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductCatalogRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val organizations = OrganizationRepository(database)
    private val products = ProductCatalogRepository(database)

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun skuIsUniqueWithinOneCompanyButMayRepeatAcrossCompaniesAndEditsPreserveIdentity() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Northstar Precision"))
        val otherCompany = organizations.createOrganization(OrganizationDraft("Anatolia Components"))
        val product = products.create(
            company.id,
            ProductDraft("cnc-100", "Precision housing", "Machined aluminium housing", "6082-T6 ±0.05mm", "Aluminium", "pcs")
        )
        assertEquals("cnc-100", product.skuCode)
        assertEquals("CNC-100", product.skuKey)

        var duplicateRejected = false
        try {
            products.create(company.id, ProductDraft("CNC-100", "Duplicate SKU"))
        } catch (_: IllegalArgumentException) { duplicateRejected = true }
        assertTrue(duplicateRejected)

        val otherProduct = products.create(otherCompany.id, ProductDraft("CNC-100", "Other company's item"))
        assertEquals("CNC-100", otherProduct.skuKey)

        val updated = products.update(
            product.id,
            ProductDraft("CNC-101", "Revised housing", specification = "Drawing B", status = ProductStatus.ACTIVE)
        )
        assertEquals(product.id, updated.id)
        assertEquals(product.createdAt, updated.createdAt)
        assertEquals("CNC-101", updated.skuKey)
        assertEquals(ProductStatus.ACTIVE, updated.status)

        val events = database.businessActivityDao().getAll().filter { it.entityType == BusinessActivityEntityType.PRODUCT && it.entityId == product.id }
        assertTrue(events.any { it.eventType == BusinessActivityAction.CREATED })
        assertTrue(events.any { it.eventType == BusinessActivityAction.STATUS_CHANGED && it.fromStatus == ProductStatus.DRAFT && it.toStatus == ProductStatus.ACTIVE })
    }

    @Test
    fun evidenceReconciliationIsAtomicAndRestrictedToSourcesInTheSameCompany() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Buyer Components"))
        val otherCompany = organizations.createOrganization(OrganizationDraft("Unrelated Supplier"))
        val first = organizations.addSource(company.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Current catalogue", "https://buyer.example/catalogue"))
        val second = organizations.addSource(company.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Drawing revision B", "https://buyer.example/drawing-b"))
        val foreign = organizations.addSource(otherCompany.id, OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Foreign source", "https://other.example/catalogue"))
        val product = products.create(company.id, ProductDraft(name = "Precision spacer"))

        assertEquals(1, products.setEvidenceLinks(product.id, setOf(first.id)))
        assertEquals(setOf(first.id), products.observeEvidence(product.id).first().map { it.sourceId }.toSet())
        assertEquals(2, products.setEvidenceLinks(product.id, setOf(second.id)))
        assertEquals(setOf(second.id), products.observeEvidence(product.id).first().map { it.sourceId }.toSet())
        val eventCount = database.businessActivityDao().getAll().size

        var foreignRejected = false
        try {
            products.setEvidenceLinks(product.id, setOf(foreign.id))
        } catch (_: IllegalArgumentException) { foreignRejected = true }
        assertTrue(foreignRejected)
        assertEquals(setOf(second.id), products.observeEvidence(product.id).first().map { it.sourceId }.toSet())
        assertEquals(eventCount, database.businessActivityDao().getAll().size)
        assertEquals(0, products.setEvidenceLinks(product.id, setOf(second.id)))
        assertTrue(database.businessActivityDao().getAll().any {
            it.entityId == product.id && it.eventType == BusinessActivityAction.EVIDENCE_UNLINKED && it.details == "Current catalogue"
        })
    }

    @Test
    fun archivedProductsRemainRecoverableAndPreventCompanyCascadeDeletion() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Tooling Works"))
        val product = products.create(company.id, ProductDraft(name = "Mould insert"))
        assertTrue(products.setStatus(product.id, ProductStatus.ARCHIVED))
        assertEquals(1, products.observeForOrganization(company.id).first().size)

        var deletionRejected = false
        try { organizations.deleteOrganization(company.id) } catch (_: IllegalArgumentException) { deletionRejected = true }
        assertTrue(deletionRejected)
        assertFalse(database.productDao().getAll().isEmpty())
    }
 
    @Test
    fun createRejectsMissingCompanyAndEveryOutOfBoundsProductField() = runBlocking {
        expectIllegal { products.create("missing-company", ProductDraft(name = "Valid item")) }
        val company = organizations.createOrganization(OrganizationDraft("Validation Works"))
        val invalidDrafts = listOf(
            ProductDraft(name = " "),
            ProductDraft(name = "N".repeat(201)),
            ProductDraft(skuCode = "S".repeat(81), name = "Valid item"),
            ProductDraft(name = "Valid item", description = "D".repeat(4_001)),
            ProductDraft(name = "Valid item", specification = "S".repeat(2_001)),
            ProductDraft(name = "Valid item", material = "M".repeat(121)),
            ProductDraft(name = "Valid item", unit = "U".repeat(41)),
            ProductDraft(name = "Valid item", status = "UNKNOWN")
        )
        invalidDrafts.forEach { draft -> expectIllegal { products.create(company.id, draft) } }

        val normalized = products.create(
            company.id,
            ProductDraft(name = "  Valid item  ", skuCode = " ", description = " ", specification = " ", material = " ", unit = " ")
        )
        assertEquals("Valid item", normalized.name)
        assertEquals(null, normalized.skuCode)
        assertEquals(null, normalized.description)
        assertEquals(null, normalized.specification)
        assertEquals(null, normalized.material)
        assertEquals(null, normalized.unit)
    }

    @Test
    fun updateRejectsMissingProductsAndSkuCollisionsButAllowsItsOwnSku() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Update Works"))
        val first = products.create(company.id, ProductDraft("sku-1", "First item"))
        val second = products.create(company.id, ProductDraft("sku-2", "Second item"))

        expectIllegal { products.update("missing-product", ProductDraft(name = "Missing")) }
        expectIllegal { products.update(second.id, ProductDraft("SKU-1", "Collision")) }

        val updated = products.update(first.id, ProductDraft("sku-1", "Renamed item"))
        assertEquals(first.id, updated.id)
        assertEquals(first.createdAt, updated.createdAt)
        assertEquals("SKU-1", updated.skuKey)
        assertEquals(ProductStatus.DRAFT, updated.status)
    }

    @Test
    fun statusChangesAreValidatedIdempotentAndSafeForMissingProducts() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Status Works"))
        val product = products.create(company.id, ProductDraft(name = "Status item"))

        assertTrue(products.setStatus(product.id, ProductStatus.DRAFT))
        assertFalse(products.setStatus("missing-product", ProductStatus.ACTIVE))
        expectIllegal { products.setStatus(product.id, "UNKNOWN") }
        assertTrue(products.setStatus(product.id, ProductStatus.DISCONTINUED))
        assertTrue(products.setStatus(product.id, ProductStatus.DISCONTINUED))

        val events = database.businessActivityDao().getAll().filter {
            it.entityType == BusinessActivityEntityType.PRODUCT && it.entityId == product.id
        }
        assertEquals(1, events.count { it.eventType == BusinessActivityAction.STATUS_CHANGED })
        assertEquals(ProductStatus.DISCONTINUED, database.productDao().getById(product.id)?.status)
    }

    @Test
    fun evidenceSelectionRejectsMalformedMissingAndExcessiveInputsWithoutMutation() = runBlocking {
        val company = organizations.createOrganization(OrganizationDraft("Evidence Works"))
        val source = organizations.addSource(
            company.id,
            OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Catalogue", "https://evidence.example/catalogue")
        )
        val product = products.create(company.id, ProductDraft(name = "Evidence item"))

        expectIllegal { products.setEvidenceLinks("missing-product", emptySet()) }
        expectIllegal { products.setEvidenceLinks(product.id, setOf(" ")) }
        expectIllegal { products.setEvidenceLinks(product.id, setOf("missing-source")) }
        expectIllegal { products.setEvidenceLinks(product.id, (1..101).map { "source-$it" }.toSet()) }
        assertTrue(database.productEvidenceDao().getForProduct(product.id).isEmpty())

        assertEquals(0, products.setEvidenceLinks(product.id, emptySet()))
        assertEquals(1, products.setEvidenceLinks(product.id, setOf(source.id)))
        assertEquals(1, products.setEvidenceLinks(product.id, emptySet()))
        assertTrue(database.productEvidenceDao().getForProduct(product.id).isEmpty())
    }

    private suspend fun expectIllegal(block: suspend () -> Any?) {
        var rejected = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Expected an IllegalArgumentException", rejected)
    }
}
