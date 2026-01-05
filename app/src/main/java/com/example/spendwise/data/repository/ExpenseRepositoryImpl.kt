package com.example.spendwise.data.repository

import com.example.spendwise.data.local.Expense
import com.example.spendwise.data.local.ExpenseDao
import kotlinx.coroutines.flow.Flow

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    override suspend fun getExpenseById(id: Int): Expense? = expenseDao.getExpenseById(id)

    override fun getExpensesForDate(date: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForDate(date)

    override fun getExpensesForMonth(year: Int, month: Int): Flow<List<Expense>> =
        expenseDao.getExpensesForMonth(year, month)

    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    override suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
    }
}
