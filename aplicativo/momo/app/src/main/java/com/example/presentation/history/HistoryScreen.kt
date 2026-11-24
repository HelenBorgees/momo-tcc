package com.example.presentation.history

import com.example.presentation.components.MomoDiaperIllustration
import com.example.presentation.components.diaperIllustrationStatus
import com.example.data.model.EstadoFralda
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.data.model.DiaperEvent
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.TipoEvento
import com.example.data.model.TrocaFralda
import com.example.presentation.components.MomoCard
import com.example.presentation.components.MomoLogomarca
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .testTag("history_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MomoLogomarca(height = 24.dp)
                }
                // Title and Subtitle
                Text(
                    text = "Histórico do MOMO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Acompanhe as trocas de fralda e as detecções do sensor.",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Abas de trocas de fralda e detecções do sensor
                HistoryTabSelector(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.setTab(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Seletor de Período (Hoje, 7 dias, 30 dias)
                PeriodSegmentedSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.setPeriod(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filtros de Categoria com suporte a opções combinadas
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("history_category_filters")
                ) {
                    items(HistoryCategoryFilter.values()) { category ->
                        val isSelected = category == uiState.selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryCoralLight else Color(0xFFF9FAFB))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryCoral else SoftBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.setCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("filter_${category.name}")
                        ) {
                            Text(
                                text = category.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryCoral else SecondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Aviso explicativo de acordo com a aba ativa
                if (uiState.selectedTab == HistoryTab.CONFIRMED_CHANGES) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, SoftBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Trocas de fralda registradas pelo cuidador.",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(1.dp, WarningAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Histórico de detecções",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "Ocorrências identificadas pelo MOMO.",
                                fontSize = 11.sp,
                                color = PrimaryText,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Lista de itens de acordo com a aba selecionada
            if (uiState.selectedTab == HistoryTab.CONFIRMED_CHANGES) {
                if (uiState.filteredChanges.isEmpty()) {
                    item {
                        EmptyHistoryCard(message = "Nenhuma troca de fralda encontrada com os filtros selecionados.")
                    }
                } else {
                    items(uiState.filteredChanges, key = { it.id }) { change ->
                        ConfirmedChangeCard(change = change)
                    }
                }
            } else {
                if (uiState.filteredSensorEvents.isEmpty()) {
                    item {
                        EmptyHistoryCard(message = "Nenhuma detecção do sensor encontrada com os filtros selecionados.")
                    }
                } else {
                    items(uiState.filteredSensorEvents, key = { it.id }) { event ->
                        SensorEventCard(event = event)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HistoryTabSelector(
    selectedTab: HistoryTab,
    onTabSelected: (HistoryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(SoftGrayTrack)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HistoryTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) CardSurface else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) SoftBorder else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 6.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.rotulo,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PrimaryCoral else SecondaryText
                )
            }
        }
    }
}

@Composable
private fun PeriodSegmentedSelector(
    selectedPeriod: ReportPeriod?,
    onPeriodSelected: (ReportPeriod?) -> Unit
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
            (ReportPeriod.values().toList() + listOf(null)).forEach { period ->
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
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period?.label ?: "Todos",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryCoral else SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(message: String) {
    MomoCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = 24.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = SecondaryText
            )
        }
    }
}

/**
 * Card para exibição de trocas confirmadas pelo cuidador.
 * Sem "Ciclo #...", com data/hora, observação humana e anotações.
 */
@Composable
private fun ConfirmedChangeCard(
    change: TrocaFralda,
    modifier: Modifier = Modifier
) {
    val obs = change.observacaoCuidador
    val illustration = when {
        obs.hasDiarrhea -> EstadoFralda.DIARREIA
        obs.hasFeces -> EstadoFralda.FEZES
        obs.hasUrine -> EstadoFralda.URINA
        obs == ObservacaoCuidador.FRALDA_SECA -> EstadoFralda.FRALDA_SECA
        else -> null
    }
    val iconBgColor = when (illustration) {
        EstadoFralda.DIARREIA -> CriticalAlertLight
        EstadoFralda.FEZES, EstadoFralda.URINA -> WarningAmberLight
        else -> InformationBlueLight
    }

    val dateTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(change.timestampMillis))

    MomoCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = 16.dp,
        testTag = "change_item_${change.id}"
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Circular Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (illustration != null) {
                    MomoDiaperIllustration(illustration, width = 38.dp)
                } else {
                    Icon(Icons.Default.Sync, null, tint = InformationBlue, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TROCA DE FRALDA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = dateTimeStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )

                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = obs.rotulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                if (!change.observacoes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, SoftBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = change.observacoes,
                            fontSize = 12.sp,
                            color = PrimaryText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card para eventos do sensor (ESP32-C6) — aba secundária para avaliação de TCC.
 */
@Composable
private fun SensorEventCard(
    event: DiaperEvent,
    modifier: Modifier = Modifier
) {
    val illustration = event.tipo.diaperIllustrationStatus()
    val iconBgColor = when (event.tipo) {
        TipoEvento.CALIBRACAO -> OnlineGreenLight
        TipoEvento.URINA, TipoEvento.FEZES -> WarningAmberLight
        TipoEvento.DIARREIA -> CriticalAlertLight
        TipoEvento.TROCA -> InformationBlueLight
    }
    var showProvenance by androidx.compose.runtime.remember(event.id) { androidx.compose.runtime.mutableStateOf(false) }
    if (showProvenance) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showProvenance = false },
        title = { Text("Procedência do evento") },
        text = { Text("Reconstrução do teste integrado. A data de referência vem da confirmação do cuidador.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { showProvenance = false }) { Text("Fechar") } }
    )

    MomoCard(
        modifier = modifier.fillMaxWidth().then(if (event.source == "reconstructed_test") Modifier.clickable { showProvenance = true } else Modifier),
        shape = RoundedCornerShape(20.dp),
        contentPadding = 16.dp,
        testTag = "sensor_event_${event.id}"
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (illustration != null) {
                    MomoDiaperIllustration(illustration, width = 38.dp)
                } else {
                    Icon(if (event.tipo == TipoEvento.CALIBRACAO) Icons.Default.AutoAwesome else Icons.Default.Sync, null, tint = if (event.tipo == TipoEvento.CALIBRACAO) OnlineGreen else InformationBlue, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DETECÇÃO DO SENSOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryText,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Text(
                    text = event.situacao +
                        (event.observacaoCuidador?.takeUnless { it == ObservacaoCuidador.NAO_INFORMADO }?.let { " · Observado: ${it.rotulo}" } ?: ""),
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }
    }
}
