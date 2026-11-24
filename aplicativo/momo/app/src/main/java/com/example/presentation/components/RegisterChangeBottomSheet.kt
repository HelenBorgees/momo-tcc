package com.example.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ObservacaoCuidador
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralDark
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder

enum class EtapaFluxoTroca {
    OBSERVACAO,
    PREPARAR_NOVA_FRALDA,
    CALIBRANDO,
    CALIBRACAO_CONCLUIDA
}

// Alias para compatibilidade
typealias ChangeFlowStep = EtapaFluxoTroca

/**
 * Fluxo de Registro de Troca e Preparação de Nova Fralda.
 *
 * Funcionamento:
 * 1. Registrar Troca: cuidador seleciona a condição realmente observada (observação humana);
 * 2. Preparar nova fralda: orientação para colocar fralda nova e seca com o sensor;
 * 3. Calibração da fralda seca: o app envia comando ao ESP32-C6 e apresenta o progresso
 *    informado pelo firmware (~30s para calcular a nova referência C0). O app NUNCA decide
 *    sozinho o fim da calibração através de timer local desvinculado;
 * 4. Nova fralda pronta: liberado pelo firmware do ESP32-C6 para iniciar o novo ciclo.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterChangeBottomSheet(
    sheetState: SheetState,
    progressoCalibracao: Int = 0,
    isCalibrando: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmObservation: (observation: ObservacaoCuidador, notes: String?, onResult: (Boolean) -> Unit) -> Unit,
    onStartCalibration: () -> Unit,
    onCompleteMonitoring: () -> Unit
) {
    var currentStep by remember { mutableStateOf(EtapaFluxoTroca.OBSERVACAO) }
    var selectedObservation by remember { mutableStateOf<ObservacaoCuidador?>(null) }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var observedCalibration by remember { mutableStateOf(false) }

    // O app avança a etapa de calibração ESTRITAMENTE quando o dispositivo informa conclusão (>= 100%)
    LaunchedEffect(progressoCalibracao, isCalibrando, currentStep) {
        if (currentStep == EtapaFluxoTroca.CALIBRANDO && isCalibrando) observedCalibration = true
        if (currentStep == EtapaFluxoTroca.CALIBRANDO && observedCalibration && !isCalibrando && progressoCalibracao >= 100) {
            currentStep = EtapaFluxoTroca.CALIBRACAO_CONCLUIDA
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Falha de rede não deve aprisionar o responsável nesta tela.
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("register_change_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentStep) {
                EtapaFluxoTroca.OBSERVACAO -> {
                    // Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Registrar Troca",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = SecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "O que foi observado?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Selecione o que você observou na fralda no momento da troca.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opções de observação do cuidador padronizadas em português (sem seleção prévia)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ObservacaoCuidador.values().forEach { option ->
                            val isSelected = selectedObservation == option
                            val bgColor = if (isSelected) PrimaryCoralLight else Color(0xFFF9FAFB)
                            val borderColor = if (isSelected) PrimaryCoral else SoftBorder
                            val textColor = if (isSelected) PrimaryCoralDark else PrimaryText

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedObservation = option
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("option_${option.name}")
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = option.rotulo,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aviso discreto sobre o registro e uso nos relatórios clínicos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Sua observação será registrada no Histórico e utilizada nos relatórios de acompanhamento.",
                            fontSize = 12.sp,
                            color = SecondaryText,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de notas opcionais
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Observações (opcional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Ex: Pomada aplicada, troca antes da soneca...", fontSize = 13.sp) },
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCoral,
                                unfocusedBorderColor = SoftBorder,
                                focusedContainerColor = Color(0xFFFAFAFA),
                                unfocusedContainerColor = Color(0xFFFAFAFA)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_notes_input")
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CriticalAlert,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().testTag("change_error_message")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botão Confirmar Troca: desabilitado até que o cuidador escolha uma opção
                    MomoPrimaryButton(
                        text = if (isSubmitting) "Salvando..." else "Confirmar Troca",
                        enabled = selectedObservation != null && !isSubmitting,
                        onClick = {
                            val obs = selectedObservation ?: return@MomoPrimaryButton
                            isSubmitting = true
                            errorMessage = null
                            onConfirmObservation(obs, notes.ifBlank { null }) { success ->
                                isSubmitting = false
                                if (success) {
                                    currentStep = EtapaFluxoTroca.PREPARAR_NOVA_FRALDA
                                } else {
                                    errorMessage = "Não foi possível registrar a troca. Tente novamente."
                                }
                            }
                        },
                        testTag = "confirm_change_button"
                    )
                }

                EtapaFluxoTroca.PREPARAR_NOVA_FRALDA -> {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Confirmação de que a observação humana foi gravada com sucesso no histórico
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OnlineGreenLight)
                            .border(1.dp, OnlineGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("change_saved_success_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Troca registrada no Histórico.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnlineGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(PrimaryCoralLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = PrimaryCoral,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Preparar nova fralda",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Coloque uma fralda nova e seca e posicione corretamente o sensor. Quando estiver pronta, inicie a preparação para o monitoramento.",
                        fontSize = 14.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    MomoPrimaryButton(
                        text = "Preparar nova fralda",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            onStartCalibration()
                            currentStep = EtapaFluxoTroca.CALIBRANDO
                        },
                        testTag = "prepare_new_diaper_button"
                    )
                }

                EtapaFluxoTroca.CALIBRANDO -> {
                    if (!isCalibrando && progressoCalibracao < 100) {
                        MomoSecondaryButton(text = "Tentar calibração novamente", onClick = onStartCalibration)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val progressFloat = (progressoCalibracao.coerceIn(0, 100)) / 100f
                    val animatedProgress by animateFloatAsState(targetValue = progressFloat, label = "cal_progress")

                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(110.dp),
                            color = PrimaryCoral,
                            trackColor = PrimaryCoralLight,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$progressoCalibracao%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "progresso",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Preparando nova fralda",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Mantenha a fralda seca e o sensor corretamente posicionados durante esta etapa.",
                        fontSize = 14.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Barra de progresso refletindo o estado recebido do ESP32-C6
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryCoral,
                        trackColor = PrimaryCoralLight
                    )
                }

                EtapaFluxoTroca.CALIBRACAO_CONCLUIDA -> {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(OnlineGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Nova fralda pronta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "A preparação foi concluída e o monitoramento já pode ser iniciado.",
                        fontSize = 14.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    MomoPrimaryButton(
                        text = "Iniciar monitoramento",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            onCompleteMonitoring()
                        },
                        testTag = "start_monitoring_button"
                    )
                }
            }
        }
    }
}
