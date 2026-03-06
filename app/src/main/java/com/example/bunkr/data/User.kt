package com.example.bunkr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0, // Adicionado = 0 para permitir auto-incremento
    val nome: String,
    val username: String,
    val password: String,
    val passwordHint: String? = null,
    val fotoPath: String? = null,

    // Novas informações solicitadas
    val dataCriacao: Long = System.currentTimeMillis(),
    val status: String = "ativo", // ativo, inativo, suspenso
    var lastLogin: Long? = null,
    var loginAttempts: Int = 0,
    var lockedUntil: Long? = null,
    val dispositivo: String = android.os.Build.MODEL
)