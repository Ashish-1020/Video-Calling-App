package com.aashu.natalks.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aashu.natalks.call.CallState
import com.aashu.natalks.di.AppContainer
import com.aashu.natalks.ui.auth.LoginScreen
import com.aashu.natalks.ui.auth.RegisterScreen
import com.aashu.natalks.ui.call.CallScreen
import com.aashu.natalks.ui.contacts.ContactsScreen

@Composable
fun NaTalksNavHost(container: AppContainer) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val session = container.authRepository.restoreSession()
        startDestination = if (session != null) Routes.CONTACTS else Routes.LOGIN
    }

    val resolvedStart = startDestination
    if (resolvedStart == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val callState by container.callController.callState.collectAsStateWithLifecycle()
    val currentRoute by navController.currentBackStackEntryAsState()

    LaunchedEffect(callState, currentRoute) {
        val onCallScreen = currentRoute?.destination?.route == Routes.CALL
        if (callState !is CallState.Idle && !onCallScreen) {
            navController.navigate(Routes.CALL) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = resolvedStart) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.CONTACTS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                authRepository = container.authRepository,
                onRegisterSuccess = {
                    navController.navigate(Routes.CONTACTS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                contactsRepository = container.contactsRepository,
                contactNameCache = container.contactNameCache,
                authRepository = container.authRepository,
                callController = container.callController,
                onCallStarted = { },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CALL) {
            CallScreen(
                callController = container.callController,
                eglBase = container.webRtcClient.eglBase,
                onCallFinished = {
                    if (navController.currentDestination?.route == Routes.CALL) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
