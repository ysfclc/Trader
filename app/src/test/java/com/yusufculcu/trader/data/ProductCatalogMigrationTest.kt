package com.yusufculcu.trader.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductCatalogMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migration13To14CreatesUniqueCompanySkuAndCascadingSourceEvidenceTables() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            MIGRATION_7_8.migrate(db)
            MIGRATION_13_14.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('org', 'Acme', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_sources (id, organizationId, sourceKind, title, url, excerpt, capturedAt) VALUES ('source', 'org', 'MANUAL_REFERENCE', 'Catalogue', 'https://acme.example/catalogue', '', 3)")
            db.execSQL("INSERT INTO products (id, organizationId, skuCode, skuKey, name, description, specification, material, unit, status, createdAt, updatedAt) VALUES ('product', 'org', 'cnc-01', 'CNC-01', 'Housing', NULL, NULL, 'Aluminium', 'pcs', 'DRAFT', 4, 5)")
            db.execSQL("INSERT INTO product_evidence (id, productId, sourceId, linkedAt) VALUES ('link', 'product', 'source', 6)")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM products"))
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM product_evidence"))

            var duplicateRejected = false
            try {
                db.execSQL("INSERT INTO products (id, organizationId, skuCode, skuKey, name, description, specification, material, unit, status, createdAt, updatedAt) VALUES ('duplicate', 'org', 'CNC-01', 'CNC-01', 'Duplicate', NULL, NULL, NULL, NULL, 'DRAFT', 7, 7)")
            } catch (_: Exception) { duplicateRejected = true }
            assertTrue(duplicateRejected)

            db.execSQL("DELETE FROM organizations WHERE id = 'org'")
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM products"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM product_evidence"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun migration14To15CreatesFitAssessmentEvidenceAndAppendOnlyOverrideTables() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            MIGRATION_7_8.migrate(db)
            MIGRATION_13_14.migrate(db)
            db.execSQL("CREATE TABLE IF NOT EXISTS proof_assets (id TEXT NOT NULL, schemaVersion INTEGER NOT NULL, kind TEXT NOT NULL, payloadJson TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('org', 'Acme', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_sources (id, organizationId, sourceKind, title, url, excerpt, capturedAt) VALUES ('source', 'org', 'MANUAL_REFERENCE', 'Catalogue', 'https://acme.example/catalogue', '', 3)")
            db.execSQL("INSERT INTO organization_claims (id, organizationId, sourceId, field, statement, reviewState, reviewNote, reviewedAt, createdAt, updatedAt) VALUES ('claim', 'org', 'source', 'Capability', 'Makes precision parts', 'SUPPORTED', NULL, 4, 3, 4)")
            db.execSQL("INSERT INTO proof_assets (id, schemaVersion, kind, payloadJson, createdAt, updatedAt) VALUES ('proof', 1, 'PRODUCT', '{}', 1, 2)")

            MIGRATION_14_15.migrate(db)
            db.execSQL("INSERT INTO fit_assessments (id, organizationId, proofAssetId, computedOutcome, evaluatedAt, createdAt, updatedAt) VALUES ('assessment', 'org', 'proof', 'PARTIAL', 5, 5, 5)")
            db.execSQL("INSERT INTO fit_assessment_evidence (id, assessmentId, organizationClaimId, proofClaimId, relationship, rationale, createdAt) VALUES ('evidence', 'assessment', 'claim', 'proof-claim', 'SUPPORTS', 'Reviewed fit', 6)")
            db.execSQL("INSERT INTO fit_assessment_overrides (id, assessmentId, decision, rationale, reviewerLabel, reviewedAt) VALUES ('override-1', 'assessment', 'REQUEST_MORE_EVIDENCE', 'Need current volume proof', 'Reviewer', 7)")
            db.execSQL("INSERT INTO fit_assessment_overrides (id, assessmentId, decision, rationale, reviewerLabel, reviewedAt) VALUES ('override-2', 'assessment', 'ACCEPT_FIT', 'New documents reviewed', 'Reviewer', 8)")

            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM fit_assessments"))
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM fit_assessment_evidence"))
            assertEquals(2, scalar(db, "SELECT COUNT(*) FROM fit_assessment_overrides"))
            assertEquals("ACCEPT_FIT", textScalar(db, "SELECT decision FROM fit_assessment_overrides ORDER BY reviewedAt DESC LIMIT 1"))

            var duplicateAssessmentRejected = false
            try {
                db.execSQL("INSERT INTO fit_assessments (id, organizationId, proofAssetId, computedOutcome, evaluatedAt, createdAt, updatedAt) VALUES ('assessment-duplicate', 'org', 'proof', 'SUPPORTED', 9, 9, 9)")
            } catch (_: Exception) { duplicateAssessmentRejected = true }
            assertTrue(duplicateAssessmentRejected)

            var duplicateEvidenceRejected = false
            try {
                db.execSQL("INSERT INTO fit_assessment_evidence (id, assessmentId, organizationClaimId, proofClaimId, relationship, rationale, createdAt) VALUES ('evidence-duplicate', 'assessment', 'claim', 'proof-claim', 'CONTRADICTS', 'Conflicting', 10)")
            } catch (_: Exception) { duplicateEvidenceRejected = true }
            assertTrue(duplicateEvidenceRejected)

            var orphanOverrideRejected = false
            try {
                db.execSQL("INSERT INTO fit_assessment_overrides (id, assessmentId, decision, rationale, reviewerLabel, reviewedAt) VALUES ('orphan', 'missing', 'REJECT_FIT', 'No assessment', NULL, 11)")
            } catch (_: Exception) { orphanOverrideRejected = true }
            assertTrue(orphanOverrideRejected)

            db.execSQL("DELETE FROM organizations WHERE id = 'org'")
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM fit_assessments"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM fit_assessment_evidence"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM fit_assessment_overrides"))
        } finally {
            helper.close()
        }
    }

    private fun textScalar(db: SupportSQLiteDatabase, sql: String): String? {
        val cursor = db.query(sql)
        return try { if (cursor.moveToFirst()) cursor.getString(0) else null } finally { cursor.close() }
    }

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Int {
        val cursor = db.query(sql)
        return try {
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }
}
