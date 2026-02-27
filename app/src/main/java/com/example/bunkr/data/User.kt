package com.example.bunkr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios") // <-- O nome aqui deve ser "usuarios"
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String
)