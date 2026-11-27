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

    @Test
    fun migration16To17CreatesAliasAndAppendOnlyIdentityTablesWithCascade() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object : SupportSQLiteOpenHelper.Callback(16) { override fun onCreate(db: SupportSQLiteDatabase) = Unit; override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit }).build())
        try {
            val db = helper.writableDatabase; db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE organizations (id TEXT NOT NULL PRIMARY KEY, canonicalName TEXT NOT NULL, websiteUrl TEXT, domain TEXT, country TEXT, city TEXT, industry TEXT, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE organization_people (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, fullName TEXT NOT NULL, role TEXT, email TEXT, phone TEXT, profileUrl TEXT, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE organization_sources (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, sourceKind TEXT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, excerpt TEXT NOT NULL, capturedAt INTEGER NOT NULL, FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE)")
            db.execSQL("INSERT INTO organizations VALUES ('org','Acme',NULL,NULL,NULL,NULL,NULL,NULL,1,1)")
            db.execSQL("INSERT INTO organization_people VALUES ('p1','org','Ada',NULL,NULL,NULL,NULL,NULL,1,1)")
            db.execSQL("INSERT INTO organization_people VALUES ('p2','org','A. Ada',NULL,NULL,NULL,NULL,NULL,1,1)")
            db.execSQL("INSERT INTO organization_sources VALUES ('src','org','MANUAL_REFERENCE','Team','https://acme.example/team','',1)")
            MIGRATION_16_17.migrate(db)
            db.execSQL("INSERT INTO organization_person_aliases VALUES ('alias','p1','org','NAME','A. Ada','a. ada','src','SOURCE_LINKED',NULL,2)")
            db.execSQL("INSERT INTO organization_person_identity_decisions VALUES ('decision','org','p1','p2','SAME_PERSON','Reviewed','src',3)")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM organization_person_aliases"))
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM organization_person_identity_decisions"))
            db.execSQL("DELETE FROM organizations WHERE id='org'")
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM organization_person_aliases"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM organization_person_identity_decisions"))
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


    @Test
    fun migration10To11CreatesSourcingTablesAndCascadesReviewAndEvidence() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(object : SupportSQLiteOpenHelper.Callback(10) { override fun onCreate(db: SupportSQLiteDatabase) = Unit; override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit }).build())
        try {
            val db=helper.writableDatabase; db.setForeignKeyConstraintsEnabled(true)
            MIGRATION_7_8.migrate(db); MIGRATION_8_9.migrate(db); MIGRATION_9_10.migrate(db); MIGRATION_10_11.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('buyer', 'Buyer', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organizations (id, canonicalName, websiteUrl, domain, country, city, industry, notes, createdAt, updatedAt) VALUES ('supplier', 'Supplier', NULL, NULL, NULL, NULL, NULL, NULL, 1, 2)")
            db.execSQL("INSERT INTO organization_sources (id, organizationId, sourceKind, title, url, excerpt, capturedAt) VALUES ('src', 'supplier', 'MANUAL_REFERENCE', 'Capabilities', 'https://supplier.example', '', 3)")
            db.execSQL("INSERT INTO buyer_requirements (id, organizationId, title, description, status, destinationCountry, destinationCity, requiredByText, currency, budgetMinorUnits, notes, createdAt, updatedAt) VALUES ('req', 'buyer', 'CNC parts', NULL, 'OPEN', NULL, NULL, NULL, NULL, NULL, NULL, 4, 4)")
            db.execSQL("INSERT INTO supplier_offers (id, organizationId, title, description, status, currency, originCountry, originCity, notes, createdAt, updatedAt) VALUES ('offer', 'supplier', 'Machining', NULL, 'AVAILABLE', 'EUR', 'Türkiye', NULL, NULL, 5, 5)")
            db.execSQL("INSERT INTO supplier_offer_items (id, offerId, productName, specification, material, quantityAvailable, quantityUnit, minimumOrderQuantity, unitPriceMinorUnits, currency, leadTimeDays, createdAt) VALUES ('item', 'offer', 'Housing', NULL, NULL, '100', 'pcs', '10', 2000, 'EUR', 20, 6)")
            db.execSQL("INSERT INTO supplier_offer_evidence (id, offerId, sourceId, linkedAt) VALUES ('evidence', 'offer', 'src', 7)")
            db.execSQL("INSERT INTO sourcing_reviews (id, buyerRequirementId, supplierOfferId, status, rationale, createdAt, updatedAt) VALUES ('review', 'req', 'offer', 'SHORTLISTED', 'Human review', 8, 8)")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM sourcing_reviews"))
            db.execSQL("DELETE FROM organizations WHERE id='supplier'")
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM supplier_offers"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM supplier_offer_items"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM supplier_offer_evidence"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM sourcing_reviews"))
        } finally { helper.close() }
    }

}
