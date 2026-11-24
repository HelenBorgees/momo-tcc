package com.example.presentation.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoPrimaryButton
import com.example.presentation.components.MomoSecondaryButton
import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.InformationBlue
import com.example.ui.theme.InformationBlueLight
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralDark
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SoftGrayTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    onFinishPairing: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: DevicePairingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parear Dispositivo MOMO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    if (uiState.currentStep != PairingStep.SUCCESS && onNavigateBack != null) {
                        IconButton(onClick = {
                            if (uiState.currentStep == PairingStep.PREPARE) {
                                onNavigateBack()
                            } else {
                                viewModel.previousStep()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = PrimaryText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApplicationBackground
                )
            )
        },
        containerColor = ApplicationBackground,
        modifier = modifier.testTag("device_pairing_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Progress Step indicators (1 to 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PairingStep.values().forEach { step ->
                        val isDone = step.stepNumber <= uiState.currentStep.stepNumber
                        val isCurrent = step == uiState.currentStep

                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(if (isDone) PrimaryCoral else SoftGrayTrack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${step.stepNumber}",
                                fontSize = if (isCurrent) 12.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) Color.White else SecondaryText
                            )
                        }

                        if (step.stepNumber < PairingStep.values().size) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .background(if (step.stepNumber < uiState.currentStep.stepNumber) PrimaryCoral else SoftGrayTrack)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Step Title
                Text(
                    text = uiState.currentStep.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de status de pareamento com proteção estrita contra quebra de linha
                PairingStatusBadge(status = uiState.pairingStatus)

                Spacer(modifier = Modifier.height(16.dp))

                // Content Card for Current Step
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pairing_step_content"
                ) { step ->
                    when (step) {
                        PairingStep.PREPARE -> PrepareStepContent()
                        PairingStep.CONNECT_SOFTAP -> SoftApStepContent()
                        PairingStep.CONFIGURE_WIFI -> ConfigureWifiStepContent(
                            ssid = uiState.selectedSsid,
                            onSsidChange = { viewModel.onSsidChange(it) },
                            password = uiState.wifiPassword,
                            onPasswordChange = { viewModel.onPasswordChange(it) }
                        )
                        PairingStep.VERIFYING -> VerifyingStepContent(
                            isChecking = uiState.isCheckingFirebase,
                            checkFailed = uiState.firebaseCheckFailed
                        )
                        PairingStep.SUCCESS -> SuccessStepContent()
                    }
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                when (uiState.currentStep) {
                    PairingStep.PREPARE -> {
                        MomoPrimaryButton(
                            text = "O dispositivo está ligado",
                            onClick = { viewModel.nextStep() },
                            testTag = "pairing_next_1"
                        )
                        if (uiState.isDemoMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            MomoSecondaryButton(
                                text = "Pareamento Simulado Rápido",
                                onClick = {
                                    viewModel.startSimulatedAutoPairing(onAutoFinish = onFinishPairing)
                                },
                                testTag = "pairing_auto_simulate_button"
                            )
                        }
                    }
                    PairingStep.CONNECT_SOFTAP -> {
                        MomoPrimaryButton(
                            text = "Conectado ao MOMO-SETUP",
                            onClick = { viewModel.nextStep() },
                            testTag = "pairing_next_2"
                        )
                        if (uiState.isDemoMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            MomoSecondaryButton(
                                text = "Usar em Modo Demonstração",
                                onClick = { viewModel.skipToSuccess() },
                                testTag = "pairing_demo_skip_button_2"
                            )
                        }
                    }
                    PairingStep.CONFIGURE_WIFI -> {
                        MomoPrimaryButton(
                            text = "Enviar Configuração Wi-Fi",
                            onClick = { viewModel.nextStep() },
                            testTag = "pairing_next_3"
                        )
                    }
                    PairingStep.VERIFYING -> {
                        if (!uiState.isDemoMode && !uiState.isCheckingFirebase && !uiState.firebaseCheckFailed) {
                            MomoPrimaryButton(text = "Já tenho internet — verificar dispositivo", onClick = { viewModel.startFirebaseVerification() })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (uiState.firebaseCheckFailed) {
                            MomoPrimaryButton(
                                text = "Verificar Novamente no Firebase",
                                onClick = { viewModel.startFirebaseVerification() },
                                testTag = "pairing_retry_firebase"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MomoSecondaryButton(
                                text = "Voltar e Corrigir Wi-Fi",
                                onClick = { viewModel.previousStep() },
                                testTag = "pairing_back_to_wifi"
                            )
                        } else {
                            if (uiState.isDemoMode) {
                                MomoSecondaryButton(
                                    text = "Concluir em Modo Demo",
                                    onClick = { viewModel.skipToSuccess() },
                                    testTag = "pairing_verify_fallback"
                                )
                            } else {
                                MomoSecondaryButton(
                                    text = "Cancelar e Voltar",
                                    onClick = { viewModel.previousStep() },
                                    testTag = "pairing_cancel_verify"
                                )
                            }
                        }
                    }
                    PairingStep.SUCCESS -> {
                        MomoPrimaryButton(
                            text = "Ir para o Início",
                            onClick = onFinishPairing,
                            testTag = "pairing_finish_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrepareStepContent() {
    MomoCard(
        shape = RoundedCornerShape(24.dp),
        contentPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryCoralLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = PrimaryCoral,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "1. Pressione o botão liga/desliga no módulo MOMO.\n\n2. Verifique se o LED de status começa a piscar em modo de pareamento.\n\n3. Mantenha o dispositivo próximo ao seu smartphone.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = PrimaryText,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SoftApStepContent() {
    Column(modifier = Modifier.fillMaxWidth()) {
        MomoCard(
            shape = RoundedCornerShape(24.dp),
            contentPadding = 20.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(InformationBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = InformationBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Conecte seu celular à rede Wi-Fi temporária gerada pelo sensor:",
                    fontSize = 14.sp,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "MOMO-SETUP-0428",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCoral
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Senha de pareamento definida no firmware",
                    fontSize = 12.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Important SoftAP Warning Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(InformationBlueLight)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = InformationBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Durante esta etapa, seu celular poderá ficar temporariamente sem internet. Isso é normal.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E3A8A),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ConfigureWifiStepContent(
    ssid: String,
    onSsidChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    MomoCard(
        shape = RoundedCornerShape(24.dp),
        contentPadding = 20.dp
    ) {
        Text(
            text = "Informe a rede Wi-Fi da sua residência (2.4 GHz) para o monitor conectar:",
            fontSize = 13.sp,
            color = SecondaryText,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nome da Rede Wi-Fi (SSID)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsidChange,
            placeholder = { Text("Ex: MinhaRede_2.4G", fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = PrimaryCoral
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCoral,
                unfocusedBorderColor = SoftBorder,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface
            ),
            modifier = Modifier.fillMaxWidth().testTag("wifi_ssid_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Senha do Wi-Fi",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Digite a senha da rede", fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryCoral
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCoral,
                unfocusedBorderColor = SoftBorder,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface
            ),
            modifier = Modifier.fillMaxWidth().testTag("wifi_password_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "As credenciais serão enviadas localmente ao endereço http://192.168.4.1 e não são armazenadas em logs.",
            fontSize = 11.sp,
            color = SecondaryText
        )
    }
}

@Composable
private fun VerifyingStepContent(
    isChecking: Boolean,
    checkFailed: Boolean
) {
    MomoCard(
        shape = RoundedCornerShape(24.dp),
        contentPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = PrimaryCoral,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Aguardando Dispositivo no Firebase...",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(InformationBlueLight)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Ação necessária no smartphone:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Reconecte seu celular ao Wi-Fi residencial ou ative os dados móveis para restabelecer a internet.\n\n2. O monitor MOMO está conectando à sua rede e iniciando o envio dos dados para o Firebase.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A8A),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else if (checkFailed) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dispositivo ainda não detectado",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ainda não recebemos o status do ESP32 no Firebase.\n\nCertifique-se de que:\n• Seu smartphone restabeleceu o acesso à internet;\n• O monitor MOMO está ligado;\n• O nome e a senha do Wi-Fi residencial estão corretos.",
                    fontSize = 13.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Start,
                    lineHeight = 19.sp
                )
            } else {
                CircularProgressIndicator(
                    color = PrimaryCoral,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Configuração recebida pelo ESP32. Reconecte o celular à internet e toque em verificar dispositivo.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SuccessStepContent() {
    MomoCard(
        shape = RoundedCornerShape(24.dp),
        contentPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(OnlineGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OnlineGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Monitor MOMO Pareado!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "O dispositivo está online e pronto para monitorar a fralda com precisão.",
                fontSize = 14.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PairingStatusBadge(status: PairingStatus) {
    val (bgColor, textColor) = when (status) {
        PairingStatus.ONLINE -> Pair(OnlineGreenLight, OnlineGreen)
        PairingStatus.NAO_PAREADO -> Pair(Color(0xFFF3F4F6), Color(0xFF6B7280))
        PairingStatus.OFFLINE -> Pair(Color(0xFFFEF2F2), Color(0xFFDC2626))
        PairingStatus.ERRO -> Pair(Color(0xFFFEF2F2), Color(0xFFDC2626))
        PairingStatus.ENVIANDO_WIFI,
        PairingStatus.AGUARDANDO_CONFIRMACAO_FIREBASE -> Pair(InformationBlueLight, Color(0xFF1E3A8A))
        PairingStatus.AGUARDANDO_CONFIGURACAO -> Pair(PrimaryCoralLight, PrimaryCoralDark)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.rotulo,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

