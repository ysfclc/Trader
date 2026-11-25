package com.yusufculcu.trader.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrganizationMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migration7To8CreatesDossierTablesWithCascadingEvidenceRelationships() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            MIGRATION_7_8.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('org-1', 'Acme', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_sources (id, organizationId, sourceKind, title, url, excerpt, capturedAt) VALUES ('source-1', 'org-1', 'MANUAL_REFERENCE', 'Source', 'https://example.com', '', 3)")
            db.execSQL("INSERT INTO organization_claims (id, organizationId, sourceId, field, statement, reviewState, reviewNote, reviewedAt, createdAt, updatedAt) VALUES ('claim-1', 'org-1', 'source-1', 'Capability', 'Makes parts', 'UNREVIEWED', NULL, NULL, 4, 4)")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM organization_claims"))
            db.execSQL("DELETE FROM organizations WHERE id = 'org-1'")
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM organization_sources"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM organization_claims"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun migration8To9CreatesCompanyScopedContactsWithCascadeDelete() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object : SupportSQLiteOpenHelper.Callback(8) { override fun onCreate(db: SupportSQLiteDatabase) = Unit; override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit }).build())
        try { val db = helper.writableDatabase; db.setForeignKeyConstraintsEnabled(true); MIGRATION_7_8.migrate(db); MIGRATION_8_9.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('org-1', 'Acme', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_people (id, organizationId, fullName, role, email, phone, profileUrl, notes, createdAt, updatedAt) VALUES ('person-1', 'org-1', 'Ada', 'Buyer', 'ada@example.com', NULL, NULL, NULL, 3, 3)")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM organization_people")); db.execSQL("DELETE FROM organizations WHERE id = 'org-1'"); assertEquals(0, scalar(db, "SELECT COUNT(*) FROM organization_people"))
        } finally { helper.close() }
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


    @Test
    fun migration9To10CreatesBuyerRequirementTablesAndCascades() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object : SupportSQLiteOpenHelper.Callback(9) { override fun onCreate(db: SupportSQLiteDatabase) = Unit; override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit }).build())
        try {
            val db=helper.writableDatabase; db.setForeignKeyConstraintsEnabled(true)
            MIGRATION_7_8.migrate(db); MIGRATION_8_9.migrate(db); MIGRATION_9_10.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('org', 'Acme', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_sources (id, organizationId, sourceKind, title, url, excerpt, capturedAt) VALUES ('src', 'org', 'MANUAL_REFERENCE', 'RFQ', 'https://acme.example/rfq', '', 3)")
            db.execSQL("INSERT INTO buyer_requirements (id, organizationId, title, description, status, destinationCountry, destinationCity, requiredByText, currency, budgetMinorUnits, notes, createdAt, updatedAt) VALUES ('req', 'org', 'CNC parts', NULL, 'OPEN', 'Poland', 'Kraków', NULL, 'EUR', NULL, NULL, 4, 4)")
            db.execSQL("INSERT INTO buyer_requirement_items (id, requirementId, productName, specification, quantity, unit, targetUnitPriceMinorUnits, createdAt) VALUES ('item', 'req', 'Housing', 'Aluminium 6082', '12.5', 'pcs', NULL, 5)")
            db.execSQL("INSERT INTO buyer_requirement_evidence (id, requirementId, sourceId, linkedAt) VALUES ('link', 'req', 'src', 6)")
            assertEquals(1, scalar(db,"SELECT COUNT(*) FROM buyer_requirement_items"))
            db.execSQL("DELETE FROM organizations WHERE id='org'")
            assertEquals(0,scalar(db,"SELECT COUNT(*) FROM buyer_requirements"))
            assertEquals(0,scalar(db,"SELECT COUNT(*) FROM buyer_requirement_items"))
            assertEquals(0,scalar(db,"SELECT COUNT(*) FROM buyer_requirement_evidence"))
        } finally { helper.close() }
    }
