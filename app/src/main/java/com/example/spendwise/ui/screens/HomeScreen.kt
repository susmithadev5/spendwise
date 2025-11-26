package com.example.spendwise.ui.screens

import androidx.compose.runtime.Composable
import com.example.spendwise.data.local.Expense

@Composable
fun HomeScreen(
    userEmail: String?,
    expenses: List<Expense>,
    onAddExpense: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onShowToday: () -> Unit,
    onShowMonth: (Int, Int) -> Unit,
    onShowAll: () -> Unit,
    onLogout: () -> Unit
) {
    ExpenseListScreen(
        userEmail = userEmail,
        expenses = expenses,
        onAddExpense = onAddExpense,
        onExpenseClick = onExpenseClick,
        onShowToday = onShowToday,
        onShowMonth = onShowMonth,
        onShowAll = onShowAll,
        onLogout = onLogout
    )
}
