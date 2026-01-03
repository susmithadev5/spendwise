package com.example.spendwise.ui.screens

import androidx.compose.runtime.Composable
import com.example.spendwise.data.local.Expense

@Composable
fun HomeScreen(
    userEmail: String?,
    expenses: List<Expense>,
    monthlySpending: Double,
    currentBudget: Double?,
    remainingBudget: Double?,
    isOverBudget: Boolean,
    onAddExpense: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onShowToday: () -> Unit,
    onShowMonth: (Int, Int) -> Unit,
    onShowAll: () -> Unit,
    onSetBudget: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    ExpenseListScreen(
        userEmail = userEmail,
        expenses = expenses,
        monthlySpending = monthlySpending,
        currentBudget = currentBudget,
        remainingBudget = remainingBudget,
        isOverBudget = isOverBudget,
        onAddExpense = onAddExpense,
        onExpenseClick = onExpenseClick,
        onShowToday = onShowToday,
        onShowMonth = onShowMonth,
        onShowAll = onShowAll,
        onSetBudget = onSetBudget,
        onOpenSettings = onOpenSettings,
        onLogout = onLogout
    )
}
