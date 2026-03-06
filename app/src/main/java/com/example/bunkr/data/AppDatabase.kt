package com.example.bunkr.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco de Dados Principal do Bunkr
 * Version 2: Inclui suporte a múltiplos usuários e metadados de segurança.
 */
@Database(
    entities = [BunkrItem::class, User::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bunkrDao(): BunkrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Se a INSTANCE não for nula, retorna ela.
            // Se for, cria o banco de forma thread-safe.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bunkr_database"
                )
                    // O fallbackToDestructiveMigration limpa o banco se houver conflito de versão.
                    // Ideal para a fase de desenvolvimento.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}