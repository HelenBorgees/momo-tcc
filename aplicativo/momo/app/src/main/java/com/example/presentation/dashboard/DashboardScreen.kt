package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.DiaperCurrentState
import com.example.data.model.DiaperEvent
import com.example.data.model.DiaperStatus
import com.example.data.model.EstadoFralda
import com.example.data.model.EventType
import com.example.presentation.components.BabyHeader
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoLogomarca
import com.example.presentation.components.MomoPrimaryButton
import com.example.presentation.components.MomoDiaperIllustration
import com.example.presentation.components.diaperIllustrationStatus
import com.example.presentation.components.MomoRadialGauge
import com.example.presentation.components.MomoStatusBadge
import com.example.presentation.components.RegisterChangeBottomSheet
import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.CriticalAlertLight
import com.example.ui.theme.InformationBlue
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight
import com.example.ui.theme.Orange
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SoftGrayTrack
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToEditBaby: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val observedUiState by viewModel.uiState.collectAsState()
    val repositoryMode by com.example.data.repository.MomoRepositoryProvider.modoAtual.collectAsState()
    val isDemo = repositoryMode == com.example.data.repository.ModoRepositorio.DEMO
    val uiState = if (observedUiState.isDemoMode == isDemo) observedUiState else DashboardUiState(isDemoMode = isDemo)
    val operationError by viewModel.operationError.collectAsState()
    if (operationError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.operationError.value = null },
            title = { Text("Operação não confirmada") },
            text = { Text(operationError ?: "") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.operationError.value = null }) { Text("Entendi") } }
        )
    }
    val isSheetOpen by viewModel.isRegisterChangeSheetOpen.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .testTag("dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 0. Top Bar: MOMO Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MomoLogomarca(height = 24.dp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isDemoMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E8FF))
                        .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO DEMONSTRAÇÃO ATIVO • DADOS SIMULADOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 1. Top Header: Baby Info + Status
            BabyHeader(
                babyProfile = uiState.babyProfile,
                isDeviceOnline = uiState.deviceState.online,
                isPaired = uiState.deviceState.isPaired,
                isDemo = uiState.isDemoMode,
                onEditProfileClick = onNavigateToEditBaby,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))


            // 2. MAIN DIAPER STATUS CARD (Discrete classifications without volume or continuous percentage)
            MainDiaperStatusCard(
                diaperState = uiState.diaperState,
                isDeviceOnline = uiState.deviceState.online,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Cartão de Monitoramento Operacional (Diferenciação clara entre conexão, monitoramento, evento e recomendação)
            val isOnline = uiState.deviceState.online
            val diaperStatus = uiState.diaperState.status
            val isReplacementRecommended = isOnline && (uiState.diaperState.replacementRecommended || diaperStatus in listOf(EstadoFralda.FEZES, EstadoFralda.DIARREIA, EstadoFralda.TROCA_RECOMENDADA))

            if (isReplacementRecommended) {
                // Alerta de recomendação apenas quando existir critério validado (ou suspeita de fezes/diarreia)
                MomoCard(
                    modifier = Modifier.fillMaxWidth().testTag("recommendation_alert_card"),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = CriticalAlertLight,
                    borderColor = CriticalAlert.copy(alpha = 0.5f),
                    contentPadding = 16.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CriticalAlert.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (diaperStatus == EstadoFralda.FEZES || diaperStatus == EstadoFralda.DIARREIA) {
                                MomoDiaperIllustration(diaperStatus, width = 38.dp)
                            } else Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CriticalAlert,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val alertTitle = when (diaperStatus) {
                            EstadoFralda.FEZES -> "POSSÍVEL PRESENÇA DE FEZES"
                            EstadoFralda.DIARREIA -> "POSSÍVEL EPISÓDIO DE DIARREIA"
                            else -> "TROCA RECOMENDADA"
                        }
                        val alertMessage = when (diaperStatus) {
                            EstadoFralda.FEZES, EstadoFralda.DIARREIA -> "Recomendamos verificar a fralda."
                            else -> "Critério validado de troca atingido."
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alertTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CriticalAlert,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alertMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Classificação automática sujeita a verificação humana.",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else if (isOnline && diaperStatus == EstadoFralda.URINA) {
                // Estado com Urina detectada: informa o evento sem disparar troca automática ou sugerir contagem
                MomoCard(
                    modifier = Modifier.fillMaxWidth().testTag("urine_detected_monitoring_card"),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFFFFF7ED),
                    borderColor = Orange.copy(alpha = 0.4f),
                    contentPadding = 14.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Orange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            MomoDiaperIllustration(EstadoFralda.URINA, width = 34.dp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MONITORAMENTO ATIVO • URINA IDENTIFICADA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Orange,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Leituras em andamento. A detecção de urina não gera recomendação automática de troca.",
                                fontSize = 12.sp,
                                color = SecondaryText,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                // Monitoramento normal (Seca, Monitorando, etc.)
                MomoCard(
                    modifier = Modifier.fillMaxWidth().testTag("normal_monitoring_card"),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFFF9FAFB),
                    borderColor = SoftBorder,
                    contentPadding = 14.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) OnlineGreenLight else SoftBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isOnline) OnlineGreen else SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!isOnline) "SEM COMUNICAÇÃO" else when (diaperStatus) {
                                    EstadoFralda.CALIBRANDO -> "CALIBRAÇÃO EM ANDAMENTO"
                                    EstadoFralda.AGUARDANDO_FRALDA -> "PREPARE UMA FRALDA SECA"
                                    else -> "MONITORAMENTO ATIVO"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) OnlineGreen else SecondaryText,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isOnline) {
                                    when (diaperStatus) {
                                        EstadoFralda.CALIBRANDO -> "Mantenha o sensor posicionado na fralda seca."
                                        EstadoFralda.AGUARDANDO_FRALDA -> "Posicione o sensor e inicie a calibração em Dispositivo."
                                        else -> "Acompanhe o estado acima. O monitor está enviando leituras."
                                    }
                                } else {
                                    "Comunicação indisponível com o ESP32-C6."
                                },
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. Quick Metrics (Prioritizing last event, last event time, and elapsed diaper time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Último Evento Identificado
                MomoCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = 16.dp,
                    testTag = "last_event_metric_card"
                ) {
                    Text(
                        text = "ÚLTIMO EVENTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.diaperState.lastEventAtMillis > 0L) uiState.diaperState.lastEventDescription else "Aguardando evento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.diaperState.formattedLastEventTime(),
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }

                // Card 2: Tempo Nesta Fralda
                MomoCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = 16.dp,
                    testTag = "time_metric_card"
                ) {
                    Text(
                        text = "FRALDA EM USO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.diaperState.formattedElapsedTime(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Desde a última troca",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Prominent Coral Button: Registrar Troca
            MomoPrimaryButton(
                text = "Registrar Troca",
                icon = Icons.Default.Sync,
                onClick = { viewModel.openRegisterChangeSheet() },
                height = 54.dp,
                testTag = "register_change_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Section: ÚLTIMAS ATIVIDADES
            MomoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                contentPadding = 18.dp,
                testTag = "recent_activities_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ÚLTIMAS ATIVIDADES",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToHistory() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ver Histórico",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryCoral
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = PrimaryCoral,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.recentEvents.isEmpty()) {
                    Text(
                        text = "Nenhuma atividade registrada ainda.",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.recentEvents.forEach { event ->
                            RecentActivityItem(event = event)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Integrated Bottom Sheet for Change Registration & Dry Diaper Calibration
        if (isSheetOpen) {
            RegisterChangeBottomSheet(
                sheetState = sheetState,
                progressoCalibracao = uiState.diaperState.progressoCalibracao,
                isCalibrando = uiState.diaperState.isCalibrating,
                onDismiss = { viewModel.closeRegisterChangeSheet() },
                onConfirmObservation = { observation, notes, onResult ->
                    viewModel.confirmDiaperObservation(observation, notes, onResult)
                },
                onStartCalibration = {
                    viewModel.startDiaperCalibration()
                },
                onCompleteMonitoring = {
                    viewModel.completeCalibrationAndStartMonitoring()
                }
            )
        }
    }
}

@Composable
private fun MainDiaperStatusCard(
    diaperState: com.example.data.model.DiaperCurrentState,
    isDeviceOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val status = if (isDeviceOnline) diaperState.status else EstadoFralda.DESCONECTADO

    MomoCard(
        modifier = modifier.testTag("main_diaper_status_card"),
        shape = RoundedCornerShape(26.dp),
        contentPadding = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Badge (evita repetição desnecessária de títulos)
            val badgeLabel = when (status) {
                EstadoFralda.URINA -> "EVENTO IDENTIFICADO"
                EstadoFralda.FEZES, EstadoFralda.DIARREIA -> "ALERTA DO SENSOR"
                EstadoFralda.FRALDA_SECA, EstadoFralda.MONITORANDO -> "MONITORAMENTO ATIVO"
                EstadoFralda.CALIBRANDO -> "CALIBRAÇÃO"
                EstadoFralda.AGUARDANDO_FRALDA -> "NOVA FRALDA"
                EstadoFralda.TROCA_RECOMENDADA -> "AVISO DE TROCA"
                EstadoFralda.DESCONECTADO -> "OFFLINE"
            }

            MomoStatusBadge(
                text = badgeLabel,
                textColor = status.color,
                backgroundColor = status.backgroundColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Circular State Indicator
            MomoRadialGauge(
                status = status,
                size = 116.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Status Title
            Text(
                text = status.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description reflecting TCC scientific model
            val descriptionText = when (status) {
                EstadoFralda.URINA -> "O sensor identificou um evento compatível com urina nesta fralda."
                EstadoFralda.FEZES -> "Verifique a fralda e registre o que observou ao realizar a troca."
                EstadoFralda.DIARREIA -> "Verifique a fralda. A classificação do sensor precisa da sua confirmação."
                EstadoFralda.TROCA_RECOMENDADA -> "Critério validado de troca atendido."
                EstadoFralda.FRALDA_SECA -> "A fralda está limpa e seca. Monitoramento ativo."
                EstadoFralda.AGUARDANDO_FRALDA -> "Coloque uma fralda nova e seca e posicione o sensor."
                EstadoFralda.CALIBRANDO -> "Mantenha a fralda seca e o sensor posicionado durante a calibração."
                EstadoFralda.MONITORANDO -> "Sistema ativo. Leituras do sensor em andamento."
                EstadoFralda.DESCONECTADO -> "Dispositivo fora de alcance ou desconectado da rede Wi-Fi."
            }

            Text(
                text = descriptionText,
                fontSize = 13.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = SoftBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Connection status & last update
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDeviceOnline) OnlineGreen else Color(0xFF9CA3AF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDeviceOnline) "Monitor conectado" else "Monitor offline",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }

                Text(
                    text = "Último sinal: ${diaperState.lastUpdatedAtText}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun RecentActivityItem(
    event: DiaperEvent,
    modifier: Modifier = Modifier
) {
    val dotColor = when (event.tipo) {
        com.example.data.model.TipoEvento.CALIBRACAO -> OnlineGreen
        com.example.data.model.TipoEvento.URINA -> Orange
        com.example.data.model.TipoEvento.FEZES -> WarningAmber
        com.example.data.model.TipoEvento.DIARREIA -> CriticalAlert
        com.example.data.model.TipoEvento.TROCA -> InformationBlue
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFBFBFC))
            .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val illustration = event.tipo.diaperIllustrationStatus()
                if (illustration != null) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        MomoDiaperIllustration(illustration, width = 26.dp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = event.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = event.subtitle,
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            Text(
                text = event.timeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryText,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
