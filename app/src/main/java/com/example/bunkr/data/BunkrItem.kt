package com.example.bunkr.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_bunkr",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"], // ALTERADO: Antes estava "id", agora deve ser "userId"
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class BunkrItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Este campo deve ter o mesmo valor do userId do usuário logado
    val userId: Int,

    val title: String,
    val accountName: String,
    val password: String,
    val packageName: String? = null
)