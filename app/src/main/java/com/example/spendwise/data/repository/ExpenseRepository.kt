package com.example.spendwise.data.repository

import com.example.spendwise.data.local.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun insertExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun getExpenseById(id: Int): Expense?
    fun getExpensesForDate(date: Long): Flow<List<Expense>>
    fun getExpensesForMonth(year: Int, month: Int): Flow<List<Expense>>
    fun getAllExpenses(): Flow<List<Expense>>
}
