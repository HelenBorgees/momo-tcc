package com.example.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.presentation.components.DeveloperPasswordDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.auth.AuthViewModel
import com.example.presentation.components.GoogleSignInButton
import com.example.presentation.components.MomoLogomarca
import com.example.presentation.components.MomoSecondaryButton
import com.example.ui.theme.ApplicationBackground

import com.example.ui.theme.CardSurface
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText

@Composable
fun WelcomeScreen(
    onAuthenticatedSuccess: () -> Unit = {},
    onEnterDemoClick: () -> Unit = {},
    onRegisterBabyClick: () -> Unit = {},
    onEnterClick: () -> Unit = onEnterDemoClick,
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    var showDemoPasswordDialog by remember { mutableStateOf(false) }

    // Automatically navigate if already authenticated
    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
            onAuthenticatedSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .testTag("welcome_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered Branding Block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Logomarca Escrita MOMO (momo_2)
                MomoLogomarca(
                    height = 68.dp,
                    contentDescription = "Logo MOMO"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Monitoramento inteligente de fraldas
                Text(
                    text = "Monitoramento inteligente de fraldas",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Acompanhe a condição da fralda de forma simples, segura e remota.
                Text(
                    text = "Acompanhe a condição da fralda de forma simples, segura e remota.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Google Sign-In Button (Real Firebase Auth)
                GoogleSignInButton(
                    onClick = {
                        authViewModel.signInWithGoogle(
                            context = context,
                            onSuccess = onAuthenticatedSuccess
                        )
                    },
                    isLoading = authState.isLoading,
                    testTag = "welcome_google_sign_in_button"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Modo Demonstração (Sem Login) - Protegido por senha de desenvolvedor
                MomoSecondaryButton(
                    text = "Acessar Modo Demonstração (Sem Login)",
                    onClick = {
                        showDemoPasswordDialog = true
                    },
                    testTag = "welcome_demo_button"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Subtle Academic Footer
                Text(
                    text = "MOMO " + com.example.BuildConfig.VERSION_NAME + " • Protótipo acadêmico • UFOP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        // Error Dialog (Handles Google sign-in failure & email collision)
        if (authState.errorMessage != null) {
            val isCollision = authState.isCollisionError
            AlertDialog(
                onDismissRequest = { authViewModel.dismissError() },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CriticalAlert,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isCollision) "Conta com Outro Método" else "Aviso de Autenticação",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = authState.errorMessage ?: "",
                        fontSize = 14.sp,
                        color = SecondaryText,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { authViewModel.dismissError() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isCollision) "Entendido" else "OK", color = Color.White)
                    }
                }
            )
        }

        // Developer Password Dialog for Demo Mode
        if (showDemoPasswordDialog) {
            DeveloperPasswordDialog(
                title = "Acesso ao Modo Demonstração",
                description = "Informe a senha de desenvolvedor para acessar o modo de demonstração com dados simulados.",
                onDismissRequest = { showDemoPasswordDialog = false },
                onPasswordCorrect = {
                    showDemoPasswordDialog = false
                    authViewModel.enterDemoMode(onEnter = onEnterDemoClick)
                }
            )
        }
    }
}
