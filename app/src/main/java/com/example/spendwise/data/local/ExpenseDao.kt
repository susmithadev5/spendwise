package com.example.spendwise.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Int): Expense?

    @Query(
        "SELECT * FROM expenses " +
            "WHERE date >= :startOfDay AND date < :startOfDay + 86400000 " +
            "ORDER BY date DESC"
    )
    fun getExpensesForDate(startOfDay: Long): Flow<List<Expense>>

    @Query(
        "SELECT * FROM expenses " +
            "WHERE strftime('%Y', datetime(date / 1000, 'unixepoch')) = CAST(:year AS TEXT) " +
            "AND strftime('%m', datetime(date / 1000, 'unixepoch')) = printf('%02d', :month) " +
            "ORDER BY date DESC"
    )
    fun getExpensesForMonth(year: Int, month: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
}
