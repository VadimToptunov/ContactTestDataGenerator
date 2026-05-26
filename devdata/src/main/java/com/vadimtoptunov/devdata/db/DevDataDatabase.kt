package com.vadimtoptunov.devdata.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IdentityEntity::class,
        TestSuiteEntity::class,
        RunResultEntity::class,
        HceStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DevDataDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun testSuiteDao(): TestSuiteDao
    abstract fun runResultDao(): RunResultDao
    abstract fun hceStateDao(): HceStateDao

    companion object {
        @Volatile private var INSTANCE: DevDataDatabase? = null

        fun get(context: Context): DevDataDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DevDataDatabase::class.java,
                    "devdata.db"
                ).build().also { INSTANCE = it }
            }
    }
}
