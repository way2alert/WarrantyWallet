package com.warrantywallet.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.warrantywallet.app.core.database.converter.Converters
import com.warrantywallet.app.core.database.dao.DocumentDao
import com.warrantywallet.app.core.database.dao.ProductDao
import com.warrantywallet.app.core.database.dao.ReminderDao
import com.warrantywallet.app.core.database.entity.DocumentEntity
import com.warrantywallet.app.core.database.entity.ProductEntity
import com.warrantywallet.app.core.database.entity.ReminderEntity

@Database(
    entities = [
        ProductEntity::class,
        ReminderEntity::class,
        DocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WarrantyWalletDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun reminderDao(): ReminderDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: WarrantyWalletDatabase? = null

        fun getInstance(context: Context): WarrantyWalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarrantyWalletDatabase::class.java,
                    "warranty_wallet.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
