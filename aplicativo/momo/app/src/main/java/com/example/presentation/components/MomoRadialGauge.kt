package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiaperStatus
import com.example.data.model.EstadoFralda
import com.example.ui.theme.PrimaryText

/** Ícone vetorial da fralda. O anel é decorativo: não representa volume ou certeza. */
@Composable
fun MomoRadialGauge(
    status: DiaperStatus,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") progress: Float = 0f,
    size: Dp = 116.dp,
    strokeWidth: Dp = 3.dp
) {
    val color = status.color
    val symbol = when (status) {
        EstadoFralda.FRALDA_SECA -> Icons.Default.Check
        EstadoFralda.URINA -> Icons.Default.WaterDrop
        EstadoFralda.FEZES, EstadoFralda.TROCA_RECOMENDADA -> Icons.Default.PriorityHigh
        EstadoFralda.DIARREIA -> Icons.Default.Warning
        EstadoFralda.CALIBRANDO -> Icons.Default.Sync
        EstadoFralda.DESCONECTADO -> Icons.Default.WifiOff
        else -> Icons.Default.Sensors
    }
    val label = when (status) {
        EstadoFralda.FRALDA_SECA -> "Seca"
        EstadoFralda.URINA -> "Umidade"
        EstadoFralda.FEZES, EstadoFralda.DIARREIA, EstadoFralda.TROCA_RECOMENDADA -> "Atenção"
        EstadoFralda.CALIBRANDO -> "Calibrando"
        EstadoFralda.AGUARDANDO_FRALDA -> "Preparar"
        EstadoFralda.DESCONECTADO -> "Offline"
        else -> "Ativo"
    }
    Box(modifier.size(size).testTag("momo_radial_gauge"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color.copy(alpha = 0.12f))
            drawCircle(color.copy(alpha = 0.4f), style = Stroke(strokeWidth.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MomoDiaperIllustration(status)
            Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
