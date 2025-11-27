package com.example.spendwise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: Budget)

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    suspend fun getBudget(): Budget?

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    fun observeBudget(): Flow<Budget?>

    @Query("DELETE FROM budget")
    suspend fun clearBudget()
}
