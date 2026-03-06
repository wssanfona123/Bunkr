package com.example.bunkr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BunkrDao {

    // --- Funções de Usuário (Gestão de Contas) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: BunkrItem)

    @Query("SELECT * FROM usuarios WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // --- Funções de Itens (Cofre de Senhas Privado) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BunkrItem)

    // BUSCA PRIVADA: Só retorna os itens que pertencem ao ID do usuário logado
    @Query("SELECT * FROM itens_bunkr WHERE userId = :currentUserId")
    fun getItemsByUser(currentUserId: Int): List<BunkrItem>

    // BUSCA POR PACOTE: Agora também filtrada pelo usuário para evitar conflitos entre contas
    @Query("SELECT * FROM itens_bunkr WHERE packageName = :packageName AND userId = :userId LIMIT 1")
    suspend fun getItemByPackageAndUser(packageName: String, userId: Int): BunkrItem?

    @Query("DELETE FROM itens_bunkr WHERE id = :itemId AND userId = :currentUserId")
    suspend fun deleteItem(itemId: Int, currentUserId: Int)

    @Query("SELECT * FROM itens_bunkr WHERE userId = :userId")
    suspend fun getAllItemsByUserSync(userId: Int): List<BunkrItem>
}