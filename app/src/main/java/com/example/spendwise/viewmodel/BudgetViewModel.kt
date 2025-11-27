package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.repository.BudgetRepository
import com.example.spendwise.data.repository.ExpenseRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _currentBudget = MutableStateFlow<Double?>(null)
    val currentBudget: StateFlow<Double?> = _currentBudget.asStateFlow()

    private val _monthlySpending = MutableStateFlow(0.0)
    val monthlySpending: StateFlow<Double> = _monthlySpending.asStateFlow()

    private val _remainingBudget = MutableStateFlow<Double?>(null)
    val remainingBudget: StateFlow<Double?> = _remainingBudget.asStateFlow()

    private val _isOverBudget = MutableStateFlow(false)
    val isOverBudget: StateFlow<Boolean> = _isOverBudget.asStateFlow()

    private val month: Int
    private val year: Int

    init {
        val calendar = Calendar.getInstance()
        month = calendar.get(Calendar.MONTH) + 1
        year = calendar.get(Calendar.YEAR)

        observeBudget()
        observeMonthlyExpenses()
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            budgetRepository.setBudget(amount, month, year)
        }
    }

    fun updateBudget(amount: Double) {
        viewModelScope.launch {
            budgetRepository.updateBudget(amount, month, year)
        }
    }

    fun loadBudget() {
        viewModelScope.launch {
            val budget = budgetRepository.getBudget()
            _currentBudget.value = budget?.amount
            updateDerivedState()
        }
    }

    fun calculateMonthlyTotals() {
        updateDerivedState()
    }

    private fun observeBudget() {
        budgetRepository.observeBudget()
            .onEach { budget ->
                _currentBudget.value = budget?.amount
                updateDerivedState()
            }
            .launchIn(viewModelScope)
    }

    private fun observeMonthlyExpenses() {
        expenseRepository.getExpensesForMonth(year, month)
            .onEach { expenses ->
                _monthlySpending.value = expenses.sumOf { it.amount }
                updateDerivedState()
            }
            .launchIn(viewModelScope)
    }

    private fun updateDerivedState() {
        val budget = _currentBudget.value
        val spending = _monthlySpending.value
        _remainingBudget.value = budget?.minus(spending)
        _isOverBudget.value = budget != null && spending > budget
    }

    companion object {
        fun provideFactory(
            budgetRepository: BudgetRepository,
            expenseRepository: ExpenseRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetViewModel(budgetRepository, expenseRepository) as T
                }
            }
    }
}
