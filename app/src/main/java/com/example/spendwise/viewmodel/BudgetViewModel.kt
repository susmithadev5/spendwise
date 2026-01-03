package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.repository.BudgetRepository
import com.example.spendwise.data.repository.ExpenseRepository
import com.example.spendwise.data.repository.SettingsRepository
import com.example.spendwise.notification.NotificationHelper
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: android.content.Context
) : ViewModel() {

    private val _currentBudget = MutableStateFlow<Double?>(null)
    val currentBudget: StateFlow<Double?> = _currentBudget.asStateFlow()

    private val _monthlySpending = MutableStateFlow(0.0)
    val monthlySpending: StateFlow<Double> = _monthlySpending.asStateFlow()

    private val _remainingBudget = MutableStateFlow<Double?>(null)
    val remainingBudget: StateFlow<Double?> = _remainingBudget.asStateFlow()

    private val _isOverBudget = MutableStateFlow(false)
    val isOverBudget: StateFlow<Boolean> = _isOverBudget.asStateFlow()

    private val _budgetAlertEnabled = MutableStateFlow(false)
    private val _hasAlertedThreshold = MutableStateFlow(false)

    private val month: Int
    private val year: Int

    init {
        val calendar = Calendar.getInstance()
        month = calendar.get(Calendar.MONTH) + 1
        year = calendar.get(Calendar.YEAR)

        observeBudget()
        observeMonthlyExpenses()
        observeSettings()
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

        if (_budgetAlertEnabled.value) {
            val thresholdReached = budget != null && budget > 0 && (spending / budget) >= 0.75
            if (thresholdReached && !_hasAlertedThreshold.value) {
                NotificationHelper.showBudgetAlert(appContext)
                _hasAlertedThreshold.value = true
            }
            if (!thresholdReached) {
                _hasAlertedThreshold.value = false
            }
        } else {
            _hasAlertedThreshold.value = false
        }
    }

    private fun observeSettings() {
        settingsRepository.budgetAlertEnabled
            .onEach { enabled -> _budgetAlertEnabled.value = enabled }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(
            budgetRepository: BudgetRepository,
            expenseRepository: ExpenseRepository,
            settingsRepository: SettingsRepository,
            appContext: android.content.Context
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BudgetViewModel(
                        budgetRepository = budgetRepository,
                        expenseRepository = expenseRepository,
                        settingsRepository = settingsRepository,
                        appContext = appContext
                    ) as T
                }
            }
    }
}
