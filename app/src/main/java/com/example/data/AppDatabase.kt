package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(value: TransactionStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus = try {
        TransactionStatus.valueOf(value)
    } catch (e: Exception) {
        TransactionStatus.SUCCESS
    }

    @TypeConverter
    fun fromType(value: TransactionType): String = value.name

    @TypeConverter
    fun toType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.PAYMENT
    }
}

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nextpay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
