package com.yusufculcu.trader

import android.app.Application
import androidx.room.Room
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.TraderRepository

class TraderApplication : Application() {
    val database: TraderDatabase by lazy {
        Room.databaseBuilder(this, TraderDatabase::class.java, "trader.db").build()
    }
    val repository: TraderRepository by lazy { TraderRepository(database) }
}
