package com.yusufculcu.trader

import android.app.Application
import androidx.room.Room
import com.yusufculcu.trader.data.MIGRATION_1_2
import com.yusufculcu.trader.data.MIGRATION_2_3
import com.yusufculcu.trader.data.MIGRATION_3_4
import com.yusufculcu.trader.data.MIGRATION_4_5
import com.yusufculcu.trader.data.MIGRATION_5_6
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.ai.TraderModelStore
import com.yusufculcu.trader.discovery.DiscoverySourceStore

class TraderApplication : Application() {
    val database: TraderDatabase by lazy {
        Room.databaseBuilder(this, TraderDatabase::class.java, "trader.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }
    val repository: TraderRepository by lazy { TraderRepository(database) }
    val discoveryStrategyRepository: DiscoveryStrategyRepository by lazy { DiscoveryStrategyRepository(database) }
    val discoveryRunRepository: DiscoveryRunRepository by lazy { DiscoveryRunRepository(database) }
    val modelStore: TraderModelStore by lazy { TraderModelStore(this) }
    val discoverySourceStore: DiscoverySourceStore by lazy { DiscoverySourceStore(this) }
}
