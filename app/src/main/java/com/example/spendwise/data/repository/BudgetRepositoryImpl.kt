package com.example.spendwise.data.repository

import com.example.spendwise.data.local.Budget
import com.example.spendwise.data.local.BudgetDao
import java.util.Calendar
import kotlinx.coroutines.flow.Flow

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override suspend fun setBudget(amount: Double, month: Int?, year: Int?) {
        budgetDao.setBudget(
            Budget(
                id = 1,
                amount = amount,
                month = month,
                year = year
            )
        )
    }

    override suspend fun updateBudget(amount: Double, month: Int?, year: Int?) {
        val existing = budgetDao.getBudget()
        budgetDao.setBudget(
            Budget(
                id = 1,
                amount = amount,
                month = month ?: existing?.month ?: currentMonth(),
                year = year ?: existing?.year ?: currentYear()
            )
        )
    }

    override suspend fun getBudget(): Budget? = budgetDao.getBudget()

    override fun observeBudget(): Flow<Budget?> = budgetDao.observeBudget()

    override suspend fun clearBudget() {
        budgetDao.clearBudget()
    }

    private fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
