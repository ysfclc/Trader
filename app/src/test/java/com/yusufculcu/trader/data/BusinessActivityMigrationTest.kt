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
class BusinessActivityMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migration11To12CreatesIndexedAppendOnlyHistoryThatSurvivesCompanyDeletion() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("""CREATE TABLE organizations (
                id TEXT NOT NULL PRIMARY KEY, canonicalName TEXT NOT NULL, websiteUrl TEXT, domain TEXT,
                country TEXT, city TEXT, industry TEXT, notes TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL
            )""")
            MIGRATION_11_12.migrate(db)
            db.execSQL("INSERT INTO organizations (id, canonicalName, createdAt, updatedAt) VALUES ('org-1', 'Acme', 1, 1)")
            db.execSQL("""INSERT INTO business_activity_events (
                id, organizationId, organizationName, entityType, entityId, entityLabel, eventType,
                fromStatus, toStatus, summary, details, occurredAt
            ) VALUES ('event-1', 'org-1', 'Acme', 'ORGANIZATION', 'org-1', 'Acme', 'CREATED',
                NULL, NULL, 'Company created', '', 2)""")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM business_activity_events"))
            val foreignKeys = db.query("PRAGMA foreign_key_list(business_activity_events)")
            try { assertEquals(0, foreignKeys.count) } finally { foreignKeys.close() }
            assertTrue(scalar(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='index_business_activity_events_occurredAt'") == 1)
            db.execSQL("DELETE FROM organizations WHERE id='org-1'")
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM business_activity_events"))
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
