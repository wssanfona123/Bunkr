package com.example.bunkr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "itens_bunkr")
data class BunkrItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,       // <-- O Adapter procura por 'title'
    val accountName: String, // <-- O Adapter procura por 'accountName'
    val password: String,
    val packageName: String? = null
)