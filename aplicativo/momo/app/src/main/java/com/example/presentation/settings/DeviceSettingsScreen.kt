package com.example.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import com.example.presentation.components.DeveloperPasswordDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DiaperStatus
import com.example.data.repository.ModoRepositorio
import com.example.presentation.components.DeviceStatusPill
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoLogomarca
import com.example.presentation.components.MomoPrimaryButton
import com.example.presentation.components.MomoSecondaryButton

import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.CriticalAlertLight
import com.example.ui.theme.InformationBlue
import com.example.ui.theme.InformationBlueLight
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight
import com.example.ui.theme.Orange
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralDark
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSettingsScreen(
    onNavigateToEditBaby: () -> Unit,
    onNavigateToCreateBaby: () -> Unit = onNavigateToEditBaby,
    onNavigateToPairing: () -> Unit,
    onNavigateToWelcome: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: DeviceSettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val observedUiState by viewModel.uiState.collectAsState()
    val repositoryMode by com.example.data.repository.MomoRepositoryProvider.modoAtual.collectAsState()
    val uiState = if (observedUiState.modoRepositorio == repositoryMode) observedUiState else DeviceSettingsUiState(modoRepositorio = repositoryMode)
    val operationError by viewModel.operationError.collectAsState()
    if (operationError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.operationError.value = null },
            title = { Text("Operação não confirmada") },
            text = { Text(operationError ?: "") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.operationError.value = null }) { Text("Entendi") } }
        )
    }
    val context = LocalContext.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDemoToolsPasswordDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .testTag("device_settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Header: Title and Subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MomoLogomarca(height = 24.dp)
                }
                Text(
                    text = "Gerenciamento do Dispositivo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Informações de conexão e calibração",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MAIN DEVICE CARD (Matching Image 5)
            MomoCard(
                shape = RoundedCornerShape(26.dp),
                contentPadding = 20.dp,
                testTag = "device_info_card"
            ) {
                // Device Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimaryCoralLight)
                                .border(1.dp, PrimaryCoral.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = PrimaryCoral,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = uiState.deviceState.modelName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uiState.deviceState.online) "Dispositivo conectado" else "Desconectado",
                                    fontSize = 13.sp,
                                    color = SecondaryText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val isDemo = uiState.modoRepositorio == ModoRepositorio.DEMO
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDemo) Color(0xFFF3E8FF) else OnlineGreenLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isDemo) "MODO DEMO" else "ESP32-C6 REAL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDemo) Color(0xFF7E22CE) else OnlineGreen
                                    )
                                }
                            }
                        }
                    }

                    DeviceStatusPill(isOnline = uiState.deviceState.online)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Metric Grid Info Inside Device Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceGridMetric(
                        modifier = Modifier.weight(1f),
                        label = "STATUS",
                        value = if (uiState.deviceState.online) "Online" else "Offline",
                        icon = Icons.Default.CheckCircle,
                        iconColor = if (uiState.deviceState.online) OnlineGreen else Color(0xFF9CA3AF)
                    )
                    DeviceGridMetric(
                        modifier = Modifier.weight(1f),
                        label = "CALIBRAÇÃO",
                        value = if (uiState.deviceState.isCalibrated) "Calibrado" else "Pendente",
                        icon = Icons.Default.CheckCircle,
                        iconColor = if (uiState.deviceState.isCalibrated) OnlineGreen else Orange
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DeviceGridMetric(
                    modifier = Modifier.fillMaxWidth(),
                    label = "REDE WI-FI",
                    value = uiState.deviceState.wifiSsid.take(24) + (if (uiState.deviceState.wifiSsid.length > 24) "..." else ""),
                    icon = Icons.Default.Wifi,
                    iconColor = PrimaryCoral
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = SoftBorder, thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Footer: Última Comunicação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ÚLTIMA COMUNICAÇÃO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = uiState.deviceState.lastCommunicationTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.modoRepositorio != ModoRepositorio.DEMO && !uiState.deviceState.online) {
                Text(uiState.deviceState.connectionMessage, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), color = SecondaryText, fontSize = 13.sp)
            }

            // CONTA & SESSÃO (Firebase Auth)
            MomoCard(
                shape = RoundedCornerShape(24.dp),
                contentPadding = 16.dp,
                testTag = "user_account_card"
            ) {
                val user = uiState.currentUser
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (user != null) PrimaryCoralLight else Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.photoUrl?.toString().isNullOrBlank()) {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = "Foto do usuário",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = if (user != null) Icons.Default.AccountCircle else Icons.Default.Science,
                                contentDescription = null,
                                tint = if (user != null) PrimaryCoral else Color(0xFF7E22CE),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.modoRepositorio == ModoRepositorio.DEMO) "Sessão de demonstração" else (user?.displayName ?: "Não autenticado"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.modoRepositorio == ModoRepositorio.DEMO) "Dados simulados locais; conta Google não utilizada" else (user?.email ?: "Entre com Google para usar seu monitor"),
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }

                    if (user != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2))
                                .clickable { showSignOutDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_sign_out_button")
                        ) {
                            Text(
                                text = "Sair",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CriticalAlert
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryCoralLight)
                                .clickable { onNavigateToWelcome() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("settings_sign_in_button")
                        ) {
                            Text(
                                text = "Entrar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCoral
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ACTION CARDS (Matching Image 5)
            MomoCard(
                shape = RoundedCornerShape(24.dp),
                contentPadding = 8.dp,
                testTag = "device_actions_card"
            ) {
                // Action 1: Calibrar nova fralda
                ActionListItem(
                    icon = Icons.Default.AutoAwesome,
                    iconColor = PrimaryCoral,
                    iconBgColor = PrimaryCoralLight,
                    title = "Calibrar nova fralda",
                    subtitle = "Registra referência com a fralda seca",
                    onClick = { viewModel.calibrateNewDiaper() }
                )

                HorizontalDivider(
                    color = SoftBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Action 2: Reconfigurar Wi-Fi
                ActionListItem(
                    icon = Icons.Default.Wifi,
                    iconColor = OnlineGreen,
                    iconBgColor = OnlineGreenLight,
                    title = "Reconfigurar Wi-Fi",
                    subtitle = "Alterar rede ou senha de conexão",
                    onClick = onNavigateToPairing
                )

                HorizontalDivider(
                    color = SoftBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Action 3: Perfil do Bebê (Acesso normal sem senha)
                val profile = uiState.babyProfile
                val babyProfileSubtitle = when {
                    uiState.isLoadingBabyProfile -> "Carregando perfil do bebê..."
                    profile != null && profile.name.isNotBlank() -> "Editar dados de ${profile.name.trim()}"
                    else -> "Configurar perfil do bebê"
                }

                ActionListItem(
                    icon = Icons.Default.Face,
                    iconColor = InformationBlue,
                    iconBgColor = InformationBlueLight,
                    title = "Perfil do Bebê",
                    subtitle = babyProfileSubtitle,
                    onClick = {
                        if (profile != null && profile.name.isNotBlank()) {
                            onNavigateToEditBaby()
                        } else {
                            onNavigateToCreateBaby()
                        }
                    }
                )


                HorizontalDivider(
                    color = SoftBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Action 4: Sobre o Sistema
                ActionListItem(
                    icon = Icons.Default.Info,
                    iconColor = Orange,
                    iconBgColor = PrimaryCoralLight,
                    title = "Sobre o MOMO",
                    subtitle = "Monitoramento Inteligente de Fraldas • UFOP",
                    onClick = { viewModel.openAboutDialog() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEPARATED DEVELOPER / DEMO SIMULATION PANEL
            // Clearly identified as test/debug mode for evaluation, protected by password
            DeveloperDemoSection(
                modoRepositorio = uiState.modoRepositorio,
                ipEsp32 = uiState.ipEsp32,
                onAlternarModo = { viewModel.alternarModoRepositorio(it) },
                onAtualizarIp = { viewModel.atualizarIpEsp32(it) },
                currentStatus = uiState.diaperState.status,
                isDeviceOnline = uiState.deviceState.online,
                onStatusChange = { viewModel.simulateStatus(it) },
                onToggleOnline = { viewModel.toggleDeviceOnline() },
                onTriggerUrine = { viewModel.triggerUrineEvent() },
                onTriggerCalibrationInProgress = { viewModel.triggerCalibrationInProgress() },
                onTriggerCalibrationComplete = { viewModel.triggerCalibrationComplete() },
                onTriggerFeces = { viewModel.triggerFecesEvent() },
                onTriggerDiarrhea = { viewModel.triggerDiarrheaEvent() },
                onResetDemo = { viewModel.resetDemoData() },
                onNavigateToPairing = onNavigateToPairing,
                onNavigateToWelcome = onNavigateToWelcome,
                onResetSetup = {
                    viewModel.resetOnboardingAndSetup {
                        onNavigateToWelcome()
                    }
                },
                isUnlocked = uiState.isDemoToolsUnlocked,
                onRequestUnlock = { showDemoToolsPasswordDialog = true },
                onLock = { viewModel.lockDemoTools() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Calibration Success Dialog
        if (uiState.calibrationSuccess) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCalibrationSuccess() },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardSurface,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OnlineGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Calibração Concluída",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = "A nova fralda seca foi calibrada com sucesso. O estado inicial foi redefinido para FRALDA SECA.",
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissCalibrationSuccess() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            )
        }

        // About Academic Prototype Dialog
        if (uiState.showAboutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAboutDialog() },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardSurface,
                title = {
                    Text(
                        text = "MOMO • UFOP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Monitoramento Inteligente de Fraldas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCoral
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Protótipo acadêmico desenvolvido no curso de Engenharia de Controle e Automação da Universidade Federal de Ouro Preto (UFOP).\n\n" +
                                    "• Suporta 1 cuidador, 1 bebê e 1 dispositivo de monitoramento por vez.\n" +
                                    "• Arquitetura preparada para integração nativa com Firebase e Backend HTTPS.\n" +
                                    "• Modo de testes integrado para validação de fluxos e interface.",
                            fontSize = 13.sp,
                            color = SecondaryText,
                            lineHeight = 19.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissAboutDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Entendido", color = Color.White)
                    }
                }
            )
        }

        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardSurface,
                title = {
                    Text(
                        text = "Sair da conta Google?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = "Ao sair, você retornará para a tela inicial do MOMO e sua sessão será encerrada.",
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.signOut(context) {
                                onNavigateToWelcome()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalAlert),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sair", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancelar", color = SecondaryText)
                    }
                }
            )
        }

        // Developer Password Dialog for Demo & Testing Tools
        if (showDemoToolsPasswordDialog) {
            DeveloperPasswordDialog(
                title = "Área de Testes e Simulação",
                description = "Informe a senha de desenvolvedor para liberar os controles de testes e simulações acadêmicas.",
                onDismissRequest = { showDemoToolsPasswordDialog = false },
                onPasswordCorrect = {
                    showDemoToolsPasswordDialog = false
                    viewModel.unlockDemoTools()
                }
            )
        }
    }
}

@Composable
private fun DeviceGridMetric(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActionListItem(
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeveloperDemoSection(
    modoRepositorio: ModoRepositorio,
    ipEsp32: String,
    onAlternarModo: (ModoRepositorio) -> Unit,
    onAtualizarIp: (String) -> Unit,
    currentStatus: DiaperStatus,
    isDeviceOnline: Boolean,
    onStatusChange: (DiaperStatus) -> Unit,
    onToggleOnline: () -> Unit,
    onTriggerUrine: () -> Unit,
    onTriggerCalibrationInProgress: () -> Unit,
    onTriggerCalibrationComplete: () -> Unit,
    onTriggerFeces: () -> Unit,
    onTriggerDiarrhea: () -> Unit,
    onResetDemo: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    onResetSetup: () -> Unit,
    isUnlocked: Boolean,
    onRequestUnlock: () -> Unit,
    onLock: () -> Unit
) {
    if (!isUnlocked) {
        // Locked State: Protected by developer password
        MomoCard(
            shape = RoundedCornerShape(24.dp),
            backgroundColor = Color(0xFFFAF5FF),
            borderColor = Color(0xFFE9D5FF),
            contentPadding = 20.dp,
            testTag = "developer_demo_card_locked"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ÁREA DE TESTES E DEMO (DADOS SIMULADOS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Acesso restrito a desenvolvedores",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Esta seção permite simular estados e eventos da fralda para avaliação acadêmica. Digite a senha de desenvolvedor para desbloquear os controles.",
                fontSize = 12.sp,
                color = SecondaryText,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("unlock_demo_tools_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desbloquear Ferramentas de Teste", color = Color.White, fontSize = 13.sp)
            }
        }
        return
    }

    // Unlocked State: Full simulation controls with re-lock option
    MomoCard(
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color(0xFFFAF5FF),
        borderColor = Color(0xFFE9D5FF),
        contentPadding = 20.dp,
        testTag = "developer_demo_card"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ÁREA DE TESTES E DEMO (DADOS SIMULADOS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Acesso liberado (Modo Desenvolvedor)",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            TextButton(
                onClick = onLock,
                modifier = Modifier.testTag("lock_demo_tools_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bloquear", fontSize = 12.sp, color = SecondaryText)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 0. Modo do Repositório (Demo vs HTTP Real ESP32-C6)
        Text(
            text = "Modo de Comunicação do Repositório:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val isDemo = modoRepositorio == ModoRepositorio.DEMO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDemo) PrimaryCoralLight else CardSurface)
                    .border(1.dp, if (isDemo) PrimaryCoral else SoftBorder, RoundedCornerShape(12.dp))
                    .clickable { onAlternarModo(ModoRepositorio.DEMO) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Demonstração (Simulado)",
                    fontSize = 11.sp,
                    fontWeight = if (isDemo) FontWeight.Bold else FontWeight.Medium,
                    color = if (isDemo) PrimaryCoralDark else PrimaryText,
                    textAlign = TextAlign.Center
                )
            }

            val isHttp = modoRepositorio == ModoRepositorio.HTTP_ESP32
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isHttp) OnlineGreenLight else CardSurface)
                    .border(1.dp, if (isHttp) OnlineGreen else SoftBorder, RoundedCornerShape(12.dp))
                    .clickable { onAlternarModo(ModoRepositorio.HTTP_ESP32) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HTTP (ESP32-C6)",
                    fontSize = 11.sp,
                    fontWeight = if (isHttp) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHttp) OnlineGreen else PrimaryText,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Calibration Simulation
        Text(
            text = "Simulação de Calibração da Fralda Seca:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                    .clickable { onTriggerCalibrationInProgress() }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Calibração em andamento",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCoral,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                    .clickable { onTriggerCalibrationComplete() }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Calibração concluída",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnlineGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Simulação de Detecção de Urina (Conforme modelo científico: sem contadores de micções consecutivas)
        Text(
            text = "Detecção de Urina (Random Forest / DTW):",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .border(1.dp, Orange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onTriggerUrine() }
                .padding(vertical = 12.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simular Urina Detectada no Sensor",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Nota: Detecção da primeira aplicação. Micções consecutivas não são contabilizadas.",
            fontSize = 11.sp,
            color = SecondaryText,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Feces & Diarrhea Simulations
        Text(
            text = "Outros Eventos Fisiológicos:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
                    .clickable { onTriggerFeces() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fezes detectadas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
                    .clickable { onTriggerDiarrhea() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Diarreia detectada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CriticalAlert
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Navegação entre Fluxos do App:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToWelcome() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Boas-Vindas / Onboarding",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E22CE)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToPairing() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fluxo de Pareamento",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E22CE)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hardware & Conectividade:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Row: Online Simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
                .clickable { onToggleOnline() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDeviceOnline) "Simular Dispositivo Offline" else "Simular Dispositivo Online",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDeviceOnline) CriticalAlert else OnlineGreen
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset demo data button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onResetDemo() }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color(0xFF7E22CE),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Restaurar Dados Padrão do Protótipo",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7E22CE)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reset Setup and Onboarding ("Testar fluxo de primeiro acesso")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFEDE9FE))
                .border(1.5.dp, Color(0xFF9333EA), RoundedCornerShape(14.dp))
                .clickable { onResetSetup() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = Color(0xFF7E22CE),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Testar fluxo de primeiro acesso",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E22CE)
                )
            }
        }
    }
}
