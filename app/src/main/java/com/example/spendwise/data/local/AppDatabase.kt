package com.example.spendwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database

@Database(entities = [Expense::class, Budget::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "spendwise_db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { INSTANCE = it }
            }
    }
}
