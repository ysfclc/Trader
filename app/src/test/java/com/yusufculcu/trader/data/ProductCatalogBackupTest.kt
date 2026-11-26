package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class ProductCatalogBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun encryptedBackupRoundTripPreservesProductMasterDataEvidenceAndActivity() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            val organizations = OrganizationRepository(source)
            val company = organizations.createOrganization(OrganizationDraft("Gaziantep Precision"))
            val sourceRecord = organizations.addSource(
                company.id,
                OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, "Product catalogue", "https://gaziantep.example/catalogue")
            )
            val repository = ProductCatalogRepository(source)
            val product = repository.create(
                company.id,
                ProductDraft("GP-42", "Fixture plate", "Machined fixture plate", "42CrMo4, drawing GP-42-B", "Alloy steel", "pcs", ProductStatus.ACTIVE)
            )
            repository.setEvidenceLinks(product.id, setOf(sourceRecord.id))

            val output = ByteArrayOutputStream()
            TraderBackupManager(source).exportEncrypted(output, "correct horse battery".toCharArray())
            TraderBackupManager(target).restoreEncrypted(
                ByteArrayInputStream(output.toByteArray()),
                "correct horse battery".toCharArray()
            )

            val restored = target.productDao().getAll().single()
            assertEquals(product.id, restored.id)
            assertEquals("GP-42", restored.skuCode)
            assertEquals("GP-42", restored.skuKey)
            assertEquals("Fixture plate", restored.name)
            assertEquals(ProductStatus.ACTIVE, restored.status)
            assertEquals(sourceRecord.id, target.productEvidenceDao().getAll().single().sourceId)
            assertTrue(target.businessActivityDao().getAll().any {
                it.entityType == BusinessActivityEntityType.PRODUCT && it.entityId == product.id &&
                    it.eventType == BusinessActivityAction.EVIDENCE_LINKED
            })
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun schemaThirteenBackupRemainsReadableWithoutProductCatalogueArrays() {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            val decoded = TraderBackupManager(database).decodeSnapshot(legacyRoot(13, 10, includeProductEvidence = false))
            assertTrue(decoded.products.isEmpty())
            assertTrue(decoded.productEvidence.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun schemaFourteenBackupRequiresProductsArray() {
        val root = legacyRoot(14, 11, includeProductEvidence = true)
        assertRejected(root)
    }

    @Test
    fun schemaFourteenBackupRequiresProductEvidenceArray() {
        val root = JSONObject(legacyRoot(14, 11, includeProductEvidence = false))
            .put("products", JSONArray())
        assertRejected(root.toString())
    }

    @Test
    fun schemaFourteenBackupAcceptsPerCompanySkuUniquenessAndValidEvidence() {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            val decoded = TraderBackupManager(database).decodeSnapshot(schemaFourteenRoot().toString())
            assertEquals(3, decoded.products.size)
            assertEquals(2, decoded.productEvidence.size)
            assertEquals(2, decoded.products.count { it.skuKey == "GP-42" })
            assertTrue(decoded.products.any { it.skuCode == null && it.skuKey == null && it.status == ProductStatus.ARCHIVED })
        } finally {
            database.close()
        }
    }

    @Test
    fun schemaFourteenBackupRejectsInvalidSkuDuplicateSkuAndCrossCompanyEvidence() {
        val invalidSku = schemaFourteenRoot()
        invalidSku.getJSONArray("products").getJSONObject(0).put("skuKey", "NOT-GP-42")
        assertRejected(invalidSku.toString())

        val duplicateSku = schemaFourteenRoot()
        duplicateSku.getJSONArray("products").put(productJson("product-duplicate", "org-a", "GP-42", "GP-42", "Duplicate item"))
        assertRejected(duplicateSku.toString())

        val crossCompanyEvidence = schemaFourteenRoot()
        crossCompanyEvidence.put(
            "productEvidence",
            JSONArray().put(evidenceJson("link-cross-company", "product-a", "source-b"))
        )
        assertRejected(crossCompanyEvidence.toString())
    }

    @Test
    fun schemaFourteenBackupRejectsMissingProductOrSourceEvidenceReferences() {
        val missingProduct = schemaFourteenRoot().put(
            "productEvidence",
            JSONArray().put(evidenceJson("link-missing-product", "missing-product", "source-a"))
        )
        assertRejected(missingProduct.toString())

        val missingSource = schemaFourteenRoot().put(
            "productEvidence",
            JSONArray().put(evidenceJson("link-missing-source", "product-a", "missing-source"))
        )
        assertRejected(missingSource.toString())
    }

    private fun schemaFourteenRoot(): JSONObject {
        val root = JSONObject(legacyRoot(14, 11, includeProductEvidence = true))
        root.put(
            "organizations",
            JSONArray()
                .put(organizationJson("org-a", "Northstar Precision"))
                .put(organizationJson("org-b", "Anatolia Components"))
        )
        root.put(
            "organizationSources",
            JSONArray()
                .put(sourceJson("source-a", "org-a", "https://northstar.example/catalogue"))
                .put(sourceJson("source-b", "org-b", "https://anatolia.example/catalogue"))
        )
        root.put(
            "products",
            JSONArray()
                .put(productJson("product-a", "org-a", "GP-42", "GP-42", "Fixture plate", ProductStatus.ACTIVE))
                .put(productJson("product-b", "org-b", "GP-42", "GP-42", "Fixture plate", ProductStatus.DRAFT))
                .put(productJson("product-c", "org-a", null, null, "Uncoded component", ProductStatus.ARCHIVED))
        )
        root.put(
            "productEvidence",
            JSONArray()
                .put(evidenceJson("link-a", "product-a", "source-a"))
                .put(evidenceJson("link-b", "product-b", "source-b"))
        )
        return root
    }

    private fun organizationJson(id: String, name: String) = JSONObject()
        .put("id", id)
        .put("canonicalName", name)
        .put("websiteUrl", JSONObject.NULL)
        .put("domain", JSONObject.NULL)
        .put("country", JSONObject.NULL)
        .put("city", JSONObject.NULL)
        .put("industry", JSONObject.NULL)
        .put("notes", JSONObject.NULL)
        .put("createdAt", 1)
        .put("updatedAt", 2)

    private fun sourceJson(id: String, organizationId: String, url: String) = JSONObject()
        .put("id", id)
        .put("organizationId", organizationId)
        .put("sourceKind", OrganizationSourceKind.MANUAL_REFERENCE)
        .put("title", "Company catalogue")
        .put("url", url)
        .put("excerpt", "")
        .put("capturedAt", 3)

    private fun productJson(
        id: String,
        organizationId: String,
        skuCode: String?,
        skuKey: String?,
        name: String,
        status: String = ProductStatus.DRAFT
    ) = JSONObject()
        .put("id", id)
        .put("organizationId", organizationId)
        .put("skuCode", skuCode ?: JSONObject.NULL)
        .put("skuKey", skuKey ?: JSONObject.NULL)
        .put("name", name)
        .put("description", JSONObject.NULL)
        .put("specification", JSONObject.NULL)
        .put("material", JSONObject.NULL)
        .put("unit", JSONObject.NULL)
        .put("status", status)
        .put("createdAt", 1)
        .put("updatedAt", 2)

    private fun evidenceJson(id: String, productId: String, sourceId: String) = JSONObject()
        .put("id", id)
        .put("productId", productId)
        .put("sourceId", sourceId)
        .put("linkedAt", 4)

    private fun assertRejected(root: String) {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            var rejected = false
            try {
                TraderBackupManager(database).decodeSnapshot(root)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue("Expected malformed product backup to be rejected", rejected)
        } finally {
            database.close()
        }
    }

    private fun legacyRoot(schema: Int, format: Int, includeProductEvidence: Boolean): String {
        val root = JSONObject()
            .put("formatVersion", format)
            .put("databaseSchemaVersion", schema)
        listOf(
            "problems", "planSteps", "activityEvents", "evidence", "opportunities",
            "workflowRuns", "workflowSteps", "workflowEvents", "discoverySources",
            "proofAssets", "discoveryStrategies", "discoveryRuns", "sourceRuns",
            "discoveryDocuments", "organizations", "organizationSources", "organizationClaims",
            "organizationPeople", "buyerRequirements", "buyerRequirementItems",
            "buyerRequirementEvidence", "supplierOffers", "supplierOfferItems",
            "supplierOfferEvidence", "sourcingReviews", "businessActivityEvents"
        ).forEach { root.put(it, JSONArray()) }
        if (includeProductEvidence) root.put("productEvidence", JSONArray())
        return root.toString()
    }

}
