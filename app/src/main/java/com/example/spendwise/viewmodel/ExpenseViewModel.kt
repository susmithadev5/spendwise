package com.example.spendwise.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.local.AppDatabase
import com.example.spendwise.data.local.Expense
import com.example.spendwise.data.repository.ExpenseRepository
import com.example.spendwise.data.repository.ExpenseRepositoryImpl
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()

    private var expensesJob: Job? = null

    init {
        loadAllExpenses()
    }

    fun addExpense(amount: Double, category: String, date: Long, note: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    category = category,
                    date = date,
                    note = note.trim()
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense.copy(note = expense.note.trim()))
            _editingExpense.value = null
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _editingExpense.value = null
        }
    }

    fun setEditingExpense(expenseId: Int) {
        viewModelScope.launch {
            _editingExpense.value = repository.getExpenseById(expenseId)
        }
    }

    fun clearEditingExpense() {
        _editingExpense.value = null
    }

    fun loadExpensesForToday() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        observeExpenses(repository.getExpensesForDate(calendar.timeInMillis))
    }

    fun loadExpensesForMonth(year: Int, month: Int) {
        observeExpenses(repository.getExpensesForMonth(year, month))
    }

    fun loadAllExpenses() {
        observeExpenses(repository.getAllExpenses())
    }

    private fun observeExpenses(source: Flow<List<Expense>>) {
        expensesJob?.cancel()
        expensesJob = source
            .onEach { _expenses.value = it }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(context)
                    val repository = ExpenseRepositoryImpl(database.expenseDao())
                    return ExpenseViewModel(repository) as T
                }
            }
    }
}
