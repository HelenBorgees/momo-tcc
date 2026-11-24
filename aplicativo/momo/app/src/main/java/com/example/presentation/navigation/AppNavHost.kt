package com.example.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.FirebaseBabyProfileRepository
import com.example.data.repository.MomoRepositoryProvider
import com.example.presentation.baby.BabyProfileScreen
import com.example.presentation.main.MainScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.pairing.DevicePairingScreen
import com.example.presentation.welcome.WelcomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.auth.AuthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = if (runCatching { Firebase.auth.currentUser }.getOrNull() != null) Screen.Main.route else Screen.Welcome.route,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeMode by MomoRepositoryProvider.modoAtual.collectAsState()
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()

    // Para usuários já autenticados ao abrir o app: recupera automaticamente do RTDB ou solicita preenchimento
    LaunchedEffect(currentUser?.uid, activeMode) {
        if (currentUser != null && MomoRepositoryProvider.modoAtual.value != com.example.data.repository.ModoRepositorio.DEMO) {
            val result = FirebaseBabyProfileRepository.getInstance().fetchBabyProfile(currentUser.uid)
            result.onSuccess { profile ->
                if (MomoRepositoryProvider.modoAtual.value != com.example.data.repository.ModoRepositorio.HTTP_ESP32 || Firebase.auth.currentUser?.uid != currentUser.uid) return@onSuccess
                if (profile != null && profile.name.isNotBlank()) {
                    // O perfil já é observado diretamente do Firebase; não regravar durante leitura.
                } else if (navController.currentDestination?.route == Screen.Main.route) {
                    navController.navigate(Screen.BabyProfile.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            val authViewModel: AuthViewModel = viewModel()
            WelcomeScreen(
                authViewModel = authViewModel,
                onAuthenticatedSuccess = {
                    MomoRepositoryProvider.alternarModo(com.example.data.repository.ModoRepositorio.HTTP_ESP32)
                    val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
                    if (uid != null) {
                        coroutineScope.launch {
                            val result = FirebaseBabyProfileRepository.getInstance().fetchBabyProfile(uid)
                            result.onSuccess { profile ->
                                if (MomoRepositoryProvider.modoAtual.value != com.example.data.repository.ModoRepositorio.HTTP_ESP32 || Firebase.auth.currentUser?.uid != uid) return@onSuccess
                                if (profile != null && profile.name.isNotBlank()) {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                } else {
                                    // Primeiro acesso da conta: solicita cadastro do bebê (campos em branco)
                                    navController.navigate(Screen.BabyProfile.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            }.onFailure { err ->
                                // Falha de rede/conexão: não direciona para cadastro, informa o usuário
                                authViewModel.setErrorMessage("Não foi possível verificar seu perfil no servidor: ${err.localizedMessage ?: "Verifique sua conexão de internet e tente novamente."}")
                            }
                        }
                    } else {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onEnterDemoClick = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.BabyProfile.route)
                }
            )
        }

        composable(Screen.BabyProfile.route) {
            BabyProfileScreen(
                isEditing = false,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateNext = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.BabyProfile.route) { inclusive = true }
                    }
                }
            )
        }

        composable("edit_baby_profile") {
            BabyProfileScreen(
                isEditing = true,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateNext = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DevicePairing.route) {
            DevicePairingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishPairing = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToEditBaby = {
                    navController.navigate("edit_baby_profile")
                },
                onNavigateToCreateBaby = {
                    navController.navigate(Screen.BabyProfile.route)
                },
                onNavigateToPairing = {
                    navController.navigate(Screen.DevicePairing.route)
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }
    }
}
