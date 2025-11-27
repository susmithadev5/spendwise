package com.example.spendwise.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class Budget(
    @PrimaryKey val id: Int = 1,
    val amount: Double,
    val month: Int? = null,
    val year: Int? = null
)
