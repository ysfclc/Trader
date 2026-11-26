package com.yusufculcu.trader

import android.app.Application
import androidx.room.Room
import com.yusufculcu.trader.data.MIGRATION_1_2
import com.yusufculcu.trader.data.MIGRATION_2_3
import com.yusufculcu.trader.data.MIGRATION_3_4
import com.yusufculcu.trader.data.MIGRATION_4_5
import com.yusufculcu.trader.data.MIGRATION_5_6
import com.yusufculcu.trader.data.MIGRATION_6_7
import com.yusufculcu.trader.data.MIGRATION_7_8
import com.yusufculcu.trader.data.MIGRATION_8_9
import com.yusufculcu.trader.data.MIGRATION_9_10
import com.yusufculcu.trader.data.MIGRATION_10_11
import com.yusufculcu.trader.data.MIGRATION_11_12
import com.yusufculcu.trader.data.MIGRATION_12_13
import com.yusufculcu.trader.data.MIGRATION_13_14
import com.yusufculcu.trader.data.SupplierOfferRepository
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.OrganizationRepository
import com.yusufculcu.trader.data.BuyerRequirementRepository
import com.yusufculcu.trader.ai.TraderModelStore
import com.yusufculcu.trader.ai.TraderModelCandidateVerifier
import com.yusufculcu.trader.discovery.DiscoverySourceStore

class TraderApplication : Application() {
    val database: TraderDatabase by lazy {
        Room.databaseBuilder(this, TraderDatabase::class.java, "trader.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
            .build()
    }
    val repository: TraderRepository by lazy { TraderRepository(database) }
    val discoveryStrategyRepository: DiscoveryStrategyRepository by lazy { DiscoveryStrategyRepository(database) }
    val discoveryRunRepository: DiscoveryRunRepository by lazy { DiscoveryRunRepository(database) }
    val organizationRepository: OrganizationRepository by lazy { OrganizationRepository(database) }
    val buyerRequirementRepository: BuyerRequirementRepository by lazy { BuyerRequirementRepository(database) }
    val supplierOfferRepository: SupplierOfferRepository by lazy { SupplierOfferRepository(database) }
    val modelStore: TraderModelStore by lazy { TraderModelStore(this) }
    val modelCandidateVerifier: TraderModelCandidateVerifier by lazy { TraderModelCandidateVerifier(this) }
    val discoverySourceStore: DiscoverySourceStore by lazy { DiscoverySourceStore(this) }
}
