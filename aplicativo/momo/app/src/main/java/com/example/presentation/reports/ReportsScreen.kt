package com.example.presentation.reports

import com.example.data.model.EstadoFralda
import com.example.presentation.components.MomoDiaperIllustration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ReportPeriod
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoLogomarca
import com.example.presentation.components.MomoPrimaryButton

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
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SoftGrayTrack
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var choosePdfType by remember { mutableStateOf(false) }
    if (choosePdfType) AlertDialog(
        onDismissRequest = { choosePdfType = false },
        title = { Text("Exportar relatório") },
        text = { Text("O resumo traz os destaques e a tabela diária. A opção completa acrescenta todos os registros do período.") },
        confirmButton = { TextButton(onClick = { choosePdfType = false; viewModel.generateAndSharePdf(context) }) { Text("Gerar resumo") } },
        dismissButton = { TextButton(onClick = { choosePdfType = false; viewModel.generateAndSharePdf(context, includeHistory = true) }) { Text("Incluir histórico completo") } }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .testTag("reports_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MomoLogomarca(height = 24.dp)
            }

            // Header Row: Title + "Gerar PDF" top-right button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Relatórios",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Resumo e métricas de monitoramento",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                }

                // Top right small PDF button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryCoralLight)
                        .clickable {
                            if (!uiState.isGeneratingPdf) {
                                choosePdfType = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryCoral,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = PrimaryCoral,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isGeneratingPdf) "Gerando..." else "Gerar PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCoral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Period Selector
            PeriodSegmentedSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { viewModel.setPeriod(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Metric Grid: Trocas confirmadas pelo cuidador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Left: Trocas confirmadas
                ReportMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TROCAS\nCONFIRMADAS",
                    count = uiState.summary.diaperChangesCount.toString(),
                    icon = Icons.Default.Sync,
                    iconColor = PrimaryCoral,
                    iconBgColor = PrimaryCoralLight,
                    testTag = "report_changes_card"
                )

                // Top Right: Trocas com urina
                ReportMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TROCAS COM\nURINA",
                    count = uiState.summary.diapersWithUrineCount.toString(),
                    icon = null,
                    illustration = EstadoFralda.URINA,
                    iconColor = Orange,
                    iconBgColor = WarningAmberLight,
                    testTag = "report_urine_card"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bottom Left: Trocas com fezes
                ReportMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TROCAS COM\nFEZES",
                    count = uiState.summary.fecesEventsCount.toString(),
                    icon = null,
                    illustration = EstadoFralda.FEZES,
                    iconColor = WarningAmber,
                    iconBgColor = WarningAmberLight,
                    testTag = "report_feces_card"
                )

                // Bottom Right: Trocas com diarreia
                ReportMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TROCAS COM\nDIARREIA",
                    count = uiState.summary.possibleDiarrheaCount.toString(),
                    icon = null,
                    illustration = EstadoFralda.DIARREIA,
                    iconColor = CriticalAlert,
                    iconBgColor = CriticalAlertLight,
                    testTag = "report_diarrhea_card"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wide Card: Tempo Médio Entre Trocas (Matching Image 4)
            MomoCard(
                shape = RoundedCornerShape(22.dp),
                contentPadding = 18.dp,
                testTag = "average_interval_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OnlineGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = OnlineGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Tempo médio entre trocas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "Período: ${uiState.selectedPeriod.label}",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    // Green Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(OnlineGreenLight)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${uiState.summary.averageHoursBetweenChanges}h",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnlineGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Bottom Button: "Gerar PDF"
            MomoPrimaryButton(
                text = if (uiState.isGeneratingPdf) "Gerando Relatório..." else "Gerar PDF",
                icon = Icons.Default.Description,
                onClick = {
                    if (!uiState.isGeneratingPdf) {
                        choosePdfType = true
                    }
                },
                height = 54.dp,
                testTag = "generate_pdf_button"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // PDF Export Confirmation Dialog
        if (uiState.showPdfExportDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPdfExportDialog() },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardSurface,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryCoralLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = PrimaryCoral,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Relatório MOMO Pronto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = "O relatório em PDF do período (${uiState.selectedPeriod.label}) foi gerado com sucesso com o formato escolhido para acompanhamento dos registros.",
                        fontSize = 14.sp,
                        color = SecondaryText,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val uri = uiState.generatedPdfUri
                            if (uri != null) {
                                viewModel.sharePdfUri(context, uri)
                            } else {
                                choosePdfType = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Compartilhar / Baixar PDF", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPdfExportDialog() }) {
                        Text("Fechar", color = SecondaryText)
                    }
                }
            )
        }
    }
}

@Composable
private fun PeriodSegmentedSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(SoftGrayTrack)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReportPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) CardSurface else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) SoftBorder else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryCoral else SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    count: String,
    icon: ImageVector?,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    illustration: EstadoFralda? = null,
    testTag: String = "report_card"
) {
    MomoCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        contentPadding = 16.dp,
        testTag = testTag
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (illustration != null) {
                    MomoDiaperIllustration(illustration, width = 34.dp)
                } else if (icon != null) Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText,
                letterSpacing = 0.5.sp,
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = count,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
    }
}
