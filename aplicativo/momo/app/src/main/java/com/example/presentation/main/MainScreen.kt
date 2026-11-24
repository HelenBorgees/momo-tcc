package com.example.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Column
import com.example.data.repository.MomoRepositoryProvider
import com.example.data.repository.ModoRepositorio
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.history.HistoryScreen
import com.example.presentation.navigation.BottomNavTab
import com.example.presentation.reports.ReportsScreen
import com.example.presentation.settings.DeviceSettingsScreen
import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder

@Composable
fun MainScreen(
    onNavigateToEditBaby: () -> Unit,
    onNavigateToCreateBaby: () -> Unit = onNavigateToEditBaby,
    onNavigateToPairing: () -> Unit,
    onNavigateToWelcome: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.DASHBOARD) }

    val mode by MomoRepositoryProvider.modoAtual.collectAsState()
    val activeRepository = remember(mode) { MomoRepositoryProvider.getRepository() }
    val device by activeRepository.getDeviceState().collectAsState(initial = com.example.data.model.DeviceState())
    val diaper by activeRepository.getDiaperCurrentState().collectAsState(initial = com.example.data.model.DiaperCurrentState())
    var alert by remember(mode) { mutableStateOf<String?>(null) }
    val seen = remember(mode) { mutableSetOf<String>() }
    LaunchedEffect(device.deviceId, device.online, diaper.cicloFralda, diaper.sensorEventId, diaper.classificacaoAutomaticaAtiva) {
        val classification = diaper.classificacaoAutomaticaAtiva
        val key = diaper.sensorEventId
        if (mode == ModoRepositorio.HTTP_ESP32 && device.online && key != null && classification in listOf("URINA", "FEZES", "DIARREIA") && seen.add(key)) {
            alert = "O ESP32 identificou: $classification. Verifique a fralda e confirme a observação para registrar no histórico."
        }
    }
    if (alert != null) androidx.compose.material3.AlertDialog(
        onDismissRequest = { alert = null }, title = { Text("Alerta MOMO") },
        text = { Text(alert ?: "") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { alert = null }) { Text("Entendi") } }
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(ApplicationBackground).statusBarsPadding()) {
                Text("MOMO " + com.example.BuildConfig.VERSION_NAME + if (mode == ModoRepositorio.DEMO) " • Demonstração" else " • Conexão real", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, color = SecondaryText)
                if (mode == ModoRepositorio.DEMO) {
                    Text("MODO DEMONSTRAÇÃO — DADOS SIMULADOS", modifier = Modifier.padding(horizontal = 12.dp), color = PrimaryCoral)
                    androidx.compose.material3.TextButton(onClick = {
                        MomoRepositoryProvider.alternarModo(ModoRepositorio.HTTP_ESP32)
                        if (runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull() == null) onNavigateToWelcome()
                    }, modifier = Modifier.testTag("exit_demo_button")) { Text("Sair da demonstração e usar conexão real") }
                }
                else if (!device.online) Text(device.connectionMessage, modifier = Modifier.padding(12.dp))
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorder)
            ) {
                NavigationBar(
                    containerColor = CardSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("momo_bottom_navigation")
                ) {
                    BottomNavTab.values().forEach { tab ->
                        val isSelected = tab == selectedTab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) PrimaryCoral else SecondaryText
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryCoral else SecondaryText
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryCoral,
                                unselectedIconColor = SecondaryText,
                                selectedTextColor = PrimaryCoral,
                                unselectedTextColor = SecondaryText,
                                indicatorColor = PrimaryCoralLight
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name}")
                        )
                    }
                }
            }
        },
        containerColor = ApplicationBackground,
        modifier = modifier.testTag("main_screen_host")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ApplicationBackground)
        ) {
            when (selectedTab) {
                BottomNavTab.DASHBOARD -> {
                    DashboardScreen(
                        onNavigateToHistory = { selectedTab = BottomNavTab.HISTORY },
                        onNavigateToEditBaby = onNavigateToEditBaby
                    )
                }
                BottomNavTab.HISTORY -> {
                    HistoryScreen()
                }
                BottomNavTab.REPORTS -> {
                    ReportsScreen()
                }
                BottomNavTab.DEVICE -> {
                    DeviceSettingsScreen(
                        onNavigateToEditBaby = onNavigateToEditBaby,
                        onNavigateToCreateBaby = onNavigateToCreateBaby,
                        onNavigateToPairing = onNavigateToPairing,
                        onNavigateToWelcome = onNavigateToWelcome,
                        onNavigateToOnboarding = onNavigateToOnboarding
                    )
                }
            }
        }
    }
}
