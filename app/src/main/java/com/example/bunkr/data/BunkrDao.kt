package com.example.bunkr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BunkrDao {

    // --- Funções de Usuário (Login do App) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM usuarios WHERE username = :username AND password = :password")
    suspend fun checkLogin(username: String, password: String): User?

    // --- Funções de Itens (Cofre de Senhas) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BunkrItem)

    @Query("SELECT * FROM itens_bunkr")
    suspend fun getAllItems(): List<BunkrItem>

    // Esta função resolve o erro "Unresolved reference"
    // Ela busca pelo final da string (ex: busca "instagram" em "android://hash@instagram/")
    @Query("SELECT * FROM itens_bunkr WHERE packageName LIKE '%' || :packagePath || '%' LIMIT 1")
    suspend fun getItemByPackage(packagePath: String): BunkrItem?
}