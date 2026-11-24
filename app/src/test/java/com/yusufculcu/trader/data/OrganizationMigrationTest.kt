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

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Int {
        val cursor = db.query(sql)
        return try {
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }
}
