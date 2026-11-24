package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CriticalAlert
import com.example.ui.theme.CriticalAlertLight
import com.example.ui.theme.InformationBlue
import com.example.ui.theme.InformationBlueLight
import com.example.ui.theme.MutedText
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight
import com.example.ui.theme.Orange
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

/**
 * Estados do sistema de monitoramento MOMO padronizados em português.
 * Correspondem aos estados operacionais recebidos do firmware do ESP32-C6.
 */
enum class EstadoFralda(
    val titulo: String,
    val rotuloUmidade: String,
    val textoBadge: String,
    val progresso: Float,
    val cor: Color,
    val corFundo: Color,
    val descricao: String,
    val requerTrocaImediata: Boolean = false
) {
    FRALDA_SECA(
        titulo = "Fralda Seca",
        rotuloUmidade = "Seca",
        textoBadge = "FRALDA SECA",
        progresso = 0.0f,
        cor = SuccessGreen,
        corFundo = PrimaryCoralLight,
        descricao = "A fralda está limpa e seca. Monitoramento ativo."
    ),
    AGUARDANDO_FRALDA(
        titulo = "Aguardando Nova Fralda",
        rotuloUmidade = "Aguardando",
        textoBadge = "AGUARDANDO NOVA FRALDA",
        progresso = 0.0f,
        cor = Orange,
        corFundo = OrangeLight,
        descricao = "Coloque uma fralda nova e seca e posicione corretamente o sensor."
    ),
    CALIBRANDO(
        titulo = "Calibrando",
        rotuloUmidade = "Calibrando",
        textoBadge = "CALIBRANDO",
        progresso = 0.5f,
        cor = PrimaryCoral,
        corFundo = PrimaryCoralLight,
        descricao = "Mantenha a fralda seca e o sensor corretamente posicionados durante esta etapa."
    ),
    MONITORANDO(
        titulo = "Monitoramento Ativo",
        rotuloUmidade = "Monitorando",
        textoBadge = "MONITORAMENTO ATIVO",
        progresso = 0.2f,
        cor = OnlineGreen,
        corFundo = OnlineGreenLight,
        descricao = "Sistema ativo. Leituras do sensor em andamento."
    ),
    URINA(
        titulo = "Urina detectada",
        rotuloUmidade = "Urina",
        textoBadge = "URINA DETECTADA",
        progresso = 0.5f,
        cor = Orange,
        corFundo = OrangeLight,
        descricao = "O sensor identificou um evento compatível com urina nesta fralda."
    ),
    FEZES(
        titulo = "Possível presença de fezes",
        rotuloUmidade = "Fezes",
        textoBadge = "POSSÍVEL PRESENÇA DE FEZES",
        progresso = 1.0f,
        cor = WarningAmber,
        corFundo = WarningAmberLight,
        descricao = "Recomendamos verificar a fralda.",
        requerTrocaImediata = true
    ),
    DIARREIA(
        titulo = "Possível episódio de diarreia",
        rotuloUmidade = "Diarreia",
        textoBadge = "POSSÍVEL EPISÓDIO DE DIARREIA",
        progresso = 1.0f,
        cor = CriticalAlert,
        corFundo = CriticalAlertLight,
        descricao = "Recomendamos verificar a fralda.",
        requerTrocaImediata = true
    ),
    TROCA_RECOMENDADA(
        titulo = "Troca Recomendada",
        rotuloUmidade = "Troca recomendada",
        textoBadge = "TROCA RECOMENDADA",
        progresso = 1.0f,
        cor = CriticalAlert,
        corFundo = CriticalAlertLight,
        descricao = "Troca recomendada pelo sistema.",
        requerTrocaImediata = true
    ),
    DESCONECTADO(
        titulo = "Dispositivo Desconectado",
        rotuloUmidade = "Desconectado",
        textoBadge = "DISPOSITIVO DESCONECTADO",
        progresso = 0.0f,
        cor = SecondaryText,
        corFundo = SoftBorder,
        descricao = "Dispositivo fora de alcance ou desconectado da rede Wi-Fi."
    );

    // Propriedades compatíveis com interfaces anteriores
    val title: String get() = titulo
    val moistureLabel: String get() = rotuloUmidade
    val badgeText: String get() = textoBadge
    val progress: Float get() = progresso
    val color: Color get() = cor
    val backgroundColor: Color get() = corFundo
    val description: String get() = descricao
    val requiresImmediateChange: Boolean get() = requerTrocaImediata

    companion object {
        fun fromString(valor: String?): EstadoFralda {
            return when (valor?.trim()?.uppercase()) {
                "AGUARDANDO_FRALDA" -> AGUARDANDO_FRALDA
                "CALIBRANDO" -> CALIBRANDO
                "MONITORANDO" -> MONITORANDO
                "URINA", "URINA_DETECTADA" -> URINA
                "FEZES", "FEZES_DETECTADAS" -> FEZES
                "DIARREIA", "DIARREIA_DETECTADA" -> DIARREIA
                "TROCA_RECOMENDADA" -> TROCA_RECOMENDADA
                "DESCONECTADO" -> DESCONECTADO
                "FRALDA_SECA" -> FRALDA_SECA
                else -> MONITORANDO
            }
        }
    }
}

/**
 * Typealias garantindo retrocompatibilidade com componentes existentes.
 */
typealias DiaperStatus = EstadoFralda
