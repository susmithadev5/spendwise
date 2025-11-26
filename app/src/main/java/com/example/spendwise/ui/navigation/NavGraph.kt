package com.example.spendwise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.ui.screens.HomeScreen
import com.example.spendwise.ui.screens.LoginScreen
import com.example.spendwise.ui.screens.SignupScreen
import com.example.spendwise.ui.screens.SplashScreen
import com.example.spendwise.viewmodel.AuthViewModel
import com.example.spendwise.viewmodel.SplashViewModel

object NavRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Signup = "signup"
    const val Home = "home"
}

@Composable
fun SpendWiseNavHost(
    authRepository: AuthRepository,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.provideFactory(authRepository))
    val splashViewModel: SplashViewModel =
        viewModel(factory = SplashViewModel.provideFactory(authRepository))

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
        homeDestination(authViewModel, navigateToLogin)
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
    onLogout: () -> Unit
) {
    composable(NavRoutes.Home) {
        val uiState by authViewModel.uiState.collectAsState()

        if (!uiState.isAuthenticated) {
            LaunchedEffect(Unit) { authViewModel.logout(onLogout) }
        } else {
            HomeScreen(
                userEmail = uiState.currentUserEmail,
                onLogout = { authViewModel.logout(onLogout) }
            )
        }
    }
}
