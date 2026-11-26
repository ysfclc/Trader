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
    @Test
    fun schemaFourteenBackupRequiresProductEvidenceArray() {
        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            val root = JSONObject(legacyRoot(14, 11, includeProductEvidence = false))
                .put("products", JSONArray())
            var rejected = false
            try {
                TraderBackupManager(database).decodeSnapshot(root.toString())
            } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        } finally {
            database.close()
        }
    }

        val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
        try {
            val root = legacyRoot(14, 11, includeProductEvidence = true)
            var rejected = false
            try {
                TraderBackupManager(database).decodeSnapshot(root)
            } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
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
