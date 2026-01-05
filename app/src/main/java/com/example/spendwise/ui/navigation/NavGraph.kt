package com.example.spendwise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.ui.screens.HomeScreen
import com.example.spendwise.ui.screens.AddExpenseScreen
import com.example.spendwise.ui.screens.EditExpenseScreen
import com.example.spendwise.ui.screens.LoginScreen
import com.example.spendwise.ui.screens.SignupScreen
import com.example.spendwise.ui.screens.SplashScreen
import com.example.spendwise.ui.screens.SetBudgetScreen
import com.example.spendwise.ui.screens.SettingsScreen
import com.example.spendwise.viewmodel.AuthViewModel
import com.example.spendwise.viewmodel.BudgetViewModel
import com.example.spendwise.viewmodel.ExpenseViewModel
import com.example.spendwise.viewmodel.SettingsViewModel
import com.example.spendwise.viewmodel.SplashViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.spendwise.data.local.AppDatabase
import com.example.spendwise.data.repository.CloudSyncRepositoryImpl
import com.example.spendwise.data.repository.BudgetRepositoryImpl
import com.example.spendwise.data.repository.ExpenseRepositoryImpl
import com.example.spendwise.data.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object NavRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Signup = "signup"
    const val Home = "home"
    const val AddExpense = "add_expense"
    const val EditExpense = "edit_expense"
    const val ExpenseId = "expenseId"
    const val SetBudget = "set_budget"
    const val Settings = "settings"
}

@Composable
fun SpendWiseNavHost(
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext
    val database = remember { AppDatabase.getDatabase(context) }
    val expenseRepository = remember { ExpenseRepositoryImpl(database.expenseDao()) }
    val budgetRepository = remember { BudgetRepositoryImpl(database.budgetDao()) }
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val cloudSyncRepository = remember {
        CloudSyncRepositoryImpl(
            firestore = firestore,
            expenseRepository = expenseRepository,
            budgetRepository = budgetRepository,
            firebaseAuth = firebaseAuth
        )
    }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.provideFactory(authRepository))
    val settingsViewModel: SettingsViewModel =
        viewModel(
            factory = SettingsViewModel.provideFactory(
                settingsRepository = settingsRepository,
                cloudSyncRepository = cloudSyncRepository,
                firebaseAuth = firebaseAuth
            )
        )
    val splashViewModel: SplashViewModel =
        viewModel(factory = SplashViewModel.provideFactory(authRepository))
    val expenseViewModel: ExpenseViewModel =
        viewModel(factory = ExpenseViewModel.provideFactory(expenseRepository))
    val budgetViewModel: BudgetViewModel =
        viewModel(
            factory = BudgetViewModel.provideFactory(
                budgetRepository,
                expenseRepository,
                settingsRepository,
                context
            )
        )

    val navigateToHome: () -> Unit = {
        navController.navigate(NavRoutes.Home) {
            popUpTo(NavRoutes.Splash) { inclusive = true }
            launchSingleTop = true
        }
    }

    val navigateToLogin: () -> Unit = {
        navController.navigate(NavRoutes.Login) {
            popUpTo(NavRoutes.Splash) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = NavRoutes.Splash) {
        splashDestination(splashViewModel, navigateToHome, navigateToLogin)
        loginDestination(authViewModel, navigateToHome) {
            navController.navigate(NavRoutes.Signup) { launchSingleTop = true }
        }
        signupDestination(authViewModel, navigateToHome) {
            navController.popBackStack()
        }
        homeDestination(
            authViewModel = authViewModel,
            expenseViewModel = expenseViewModel,
            budgetViewModel = budgetViewModel,
            onLogout = navigateToLogin,
            onAddExpense = { navController.navigate(NavRoutes.AddExpense) },
            onExpenseClick = { expenseId ->
                navController.navigate("${NavRoutes.EditExpense}/$expenseId")
            },
            onSetBudget = { navController.navigate(NavRoutes.SetBudget) },
            onOpenSettings = { navController.navigate(NavRoutes.Settings) }
        )
        addExpenseDestination(expenseViewModel) { navController.popBackStack() }
        editExpenseDestination(expenseViewModel) { navController.popBackStack() }
        setBudgetDestination(budgetViewModel) { navController.popBackStack() }
        settingsDestination(settingsViewModel) { navController.popBackStack() }
    }
}

private fun NavGraphBuilder.splashDestination(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    composable(NavRoutes.Splash) {
        SplashScreen(
            viewModel = viewModel,
            onNavigateToHome = onNavigateToHome,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}

private fun NavGraphBuilder.loginDestination(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onSignupClick: () -> Unit
) {
    composable(NavRoutes.Login) {
        val uiState by authViewModel.uiState.collectAsState()

        LoginScreen(
            uiState = uiState,
            onEmailChange = {
                authViewModel.onEmailChange(it)
                authViewModel.clearError()
            },
            onPasswordChange = {
                authViewModel.onPasswordChange(it)
                authViewModel.clearError()
            },
            onLogin = { authViewModel.login(onLoginSuccess) },
            onSignupClick = onSignupClick,
            onClearError = authViewModel::clearError
        )
    }
}

private fun NavGraphBuilder.signupDestination(
    authViewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    composable(NavRoutes.Signup) {
        val uiState by authViewModel.uiState.collectAsState()

        SignupScreen(
            uiState = uiState,
            onEmailChange = {
                authViewModel.onEmailChange(it)
                authViewModel.clearError()
            },
            onPasswordChange = {
                authViewModel.onPasswordChange(it)
                authViewModel.clearError()
            },
            onConfirmPasswordChange = {
                authViewModel.onConfirmPasswordChange(it)
                authViewModel.clearError()
            },
            onSignup = { authViewModel.signup(onSignupSuccess) },
            onLoginClick = onLoginClick,
            onClearError = authViewModel::clearError
        )
    }
}

private fun NavGraphBuilder.homeDestination(
    authViewModel: AuthViewModel,
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    onLogout: () -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (Int) -> Unit,
    onSetBudget: () -> Unit,
    onOpenSettings: () -> Unit
) {
    composable(NavRoutes.Home) {
        val uiState by authViewModel.uiState.collectAsState()
        val expenses by expenseViewModel.expenses.collectAsState()
        val monthlySpending by budgetViewModel.monthlySpending.collectAsState()
        val currentBudget by budgetViewModel.currentBudget.collectAsState()
        val remainingBudget by budgetViewModel.remainingBudget.collectAsState()
        val isOverBudget by budgetViewModel.isOverBudget.collectAsState()

        if (!uiState.isAuthenticated) {
            LaunchedEffect(Unit) { authViewModel.logout(onLogout) }
        } else {
            HomeScreen(
                userEmail = uiState.currentUserEmail,
                expenses = expenses,
                monthlySpending = monthlySpending,
                currentBudget = currentBudget,
                remainingBudget = remainingBudget,
                isOverBudget = isOverBudget,
                onAddExpense = onAddExpense,
                onExpenseClick = onExpenseClick,
                onShowToday = { expenseViewModel.loadExpensesForToday() },
                onShowMonth = { year, month ->
                    expenseViewModel.loadExpensesForMonth(year, month)
                },
                onShowAll = { expenseViewModel.loadAllExpenses() },
                onSetBudget = onSetBudget,
                onOpenSettings = onOpenSettings,
                onLogout = { authViewModel.logout(onLogout) }
            )
        }
    }
}

private fun NavGraphBuilder.addExpenseDestination(
    expenseViewModel: ExpenseViewModel,
    onFinished: () -> Unit
) {
    composable(NavRoutes.AddExpense) {
        AddExpenseScreen(
            onSaveExpense = { amount, category, date, note ->
                expenseViewModel.addExpense(amount, category, date, note)
                onFinished()
            },
            onNavigateBack = onFinished
        )
    }
}

private fun NavGraphBuilder.editExpenseDestination(
    expenseViewModel: ExpenseViewModel,
    onFinished: () -> Unit
) {
    composable(
        route = "${NavRoutes.EditExpense}/{${NavRoutes.ExpenseId}}",
        arguments = listOf(
            navArgument(NavRoutes.ExpenseId) { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val expenseId = backStackEntry.arguments?.getInt(NavRoutes.ExpenseId) ?: return@composable

        LaunchedEffect(expenseId) {
            expenseViewModel.setEditingExpense(expenseId)
        }

        val editingExpense by expenseViewModel.editingExpense.collectAsState()

        EditExpenseScreen(
            expense = editingExpense,
            onSaveChanges = { updated ->
                expenseViewModel.updateExpense(updated)
                onFinished()
            },
            onDelete = { expense ->
                expenseViewModel.deleteExpense(expense)
                onFinished()
            },
            onNavigateBack = {
                expenseViewModel.clearEditingExpense()
                onFinished()
            }
        )
    }
}

private fun NavGraphBuilder.setBudgetDestination(
    budgetViewModel: BudgetViewModel,
    onFinished: () -> Unit
) {
    composable(NavRoutes.SetBudget) {
        SetBudgetScreen(
            onSaveBudget = { amount ->
                budgetViewModel.setBudget(amount)
                budgetViewModel.calculateMonthlyTotals()
            },
            onNavigateBack = onFinished
        )
    }
}

private fun NavGraphBuilder.settingsDestination(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    composable(NavRoutes.Settings) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = onNavigateBack
        )
    }
}
