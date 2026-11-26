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

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Int {
        val cursor = db.query(sql)
        return try {
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }
}
