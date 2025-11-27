package com.example.spendwise.data.repository

import com.example.spendwise.data.local.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun setBudget(amount: Double, month: Int? = null, year: Int? = null)
    suspend fun updateBudget(amount: Double, month: Int? = null, year: Int? = null)
    suspend fun getBudget(): Budget?
    fun observeBudget(): Flow<Budget?>
    suspend fun clearBudget()
}
