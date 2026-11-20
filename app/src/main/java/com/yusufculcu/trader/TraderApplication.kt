package com.yusufculcu.trader

import android.app.Application
import androidx.room.Room
import com.yusufculcu.trader.data.MIGRATION_1_2
import com.yusufculcu.trader.data.MIGRATION_2_3
import com.yusufculcu.trader.data.MIGRATION_3_4
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.ai.TraderModelStore

class TraderApplication : Application() {
    val database: TraderDatabase by lazy {
        Room.databaseBuilder(this, TraderDatabase::class.java, "trader.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
    val repository: TraderRepository by lazy { TraderRepository(database) }
    val modelStore: TraderModelStore by lazy { TraderModelStore(this) }
}
