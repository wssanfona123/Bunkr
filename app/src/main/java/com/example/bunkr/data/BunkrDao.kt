package com.example.bunkr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BunkrDao {

    // --- Funções de Usuário ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM usuarios WHERE username = :username AND password = :password")
    suspend fun checkLogin(username: String, password: String): User?

    // --- Funções de Itens (Bunkr) ---

    @Insert
    suspend fun insert(item: BunkrItem)

    @Query("SELECT * FROM itens_bunkr")
    suspend fun getAllItems(): List<BunkrItem>

    // ESTA FUNÇÃO DEVE FICAR DENTRO DA INTERFACE:
    @Query("SELECT * FROM itens_bunkr WHERE packageName = :packageName LIMIT 1")
    suspend fun getItemByPackage(packageName: String): BunkrItem?
}