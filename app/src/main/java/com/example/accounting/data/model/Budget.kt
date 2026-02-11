package com.example.accounting.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthStr: String,        // 对应上面的 monthStr，如 "2024-05"
    val budgetAmount: Double     // 该月的预算金额
)