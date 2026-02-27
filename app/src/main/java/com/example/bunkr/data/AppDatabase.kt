package com.example.bunkr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Se um desses ::class estiver vermelho, o KSP vai dar erro de MissingType
@Database(entities = [BunkrItem::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bunkrDao(): BunkrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bunkr_database"
                )

                    .fallbackToDestructiveMigration() // Evita crashes se você mudar as tabelas
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}