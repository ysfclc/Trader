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
class SourcingRecordLifecycleMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun migrationAddsNullableTombstonesWithoutDeletingExistingRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE buyer_requirements (id TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL("CREATE TABLE supplier_offers (id TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL("INSERT INTO buyer_requirements (id) VALUES ('buyer-1')")
                        db.execSQL("INSERT INTO supplier_offers (id) VALUES ('offer-1')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            MIGRATION_12_13.migrate(db)
            db.query("PRAGMA table_info(buyer_requirements)").use { cursor ->
                val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
                assertTrue(columns.contains("deletedAt"))
            }
            db.query("PRAGMA table_info(supplier_offers)").use { cursor ->
                val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
                assertTrue(columns.contains("deletedAt"))
            }
            db.query("SELECT deletedAt FROM buyer_requirements WHERE id='buyer-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            db.query("SELECT deletedAt FROM supplier_offers WHERE id='offer-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            db.query("SELECT COUNT(*) FROM buyer_requirements").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM supplier_offers").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        } finally {
            helper.close()
        }
    }
}
