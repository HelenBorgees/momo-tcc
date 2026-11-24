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

/** Desenho original do indicador inicial, dimensionado para cada espaço. */
@Composable
fun MomoDiaperIllustration(
    status: EstadoFralda,
    modifier: Modifier = Modifier,
    width: Dp = 66.dp
) {
    val color = status.color
    val scale = width.value / 66f
    val symbol = when (status) {
        EstadoFralda.FRALDA_SECA -> Icons.Default.Check
        EstadoFralda.URINA -> Icons.Default.WaterDrop
        EstadoFralda.FEZES, EstadoFralda.TROCA_RECOMENDADA -> Icons.Default.PriorityHigh
        EstadoFralda.DIARREIA -> Icons.Default.Warning
        EstadoFralda.CALIBRANDO -> Icons.Default.Sync
        EstadoFralda.DESCONECTADO -> Icons.Default.WifiOff
        else -> Icons.Default.Sensors
    }

    Box(modifier.size(width, 54.dp * scale)) {
        Canvas(Modifier.fillMaxSize().padding((3.dp * scale))) {
            val w = this.size.width
            val h = this.size.height
            val outline = Path().apply {
                moveTo(w * .12f, h * .15f)
                quadraticTo(w * .5f, h * .26f, w * .88f, h * .15f)
                lineTo(w * .79f, h * .65f)
                quadraticTo(w * .5f, h * 1.1f, w * .21f, h * .65f)
                close()
            }
            drawPath(outline, color.copy(alpha = .08f))
            drawPath(outline, color, style = Stroke((2.5.dp * scale).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(color, Offset(w * .15f, h * .32f), Offset(w * .30f, h * .38f), (2.dp * scale).toPx(), StrokeCap.Round)
            drawLine(color, Offset(w * .85f, h * .32f), Offset(w * .70f, h * .38f), (2.dp * scale).toPx(), StrokeCap.Round)
        }
        if (status == EstadoFralda.FEZES || status == EstadoFralda.DIARREIA) {
            Canvas(Modifier.align(Alignment.Center).padding(top = (4.dp * scale)).size((26.dp * scale))) {
                val w = this.size.width
                val h = this.size.height
                val cocoa = Color(0xFF946B55)
                val shape = Path().apply {
                    if (status == EstadoFralda.FEZES) {
                        // Pequena espiral de três camadas, sem rosto.
                        moveTo(w * .20f, h * .84f)
                        cubicTo(w * .04f, h * .83f, w * .06f, h * .63f, w * .23f, h * .60f)
                        cubicTo(w * .13f, h * .47f, w * .27f, h * .36f, w * .39f, h * .36f)
                        cubicTo(w * .33f, h * .24f, w * .57f, h * .22f, w * .57f, h * .07f)
                        cubicTo(w * .77f, h * .20f, w * .72f, h * .34f, w * .64f, h * .39f)
                        cubicTo(w * .84f, h * .41f, w * .89f, h * .52f, w * .78f, h * .61f)
                        cubicTo(w * .98f, h * .66f, w * .96f, h * .85f, w * .78f, h * .85f)
                        close()
                    } else {
                        // Mancha cremosa de contorno orgânico, distinta da espiral.
                        moveTo(w * .14f, h * .72f)
                        cubicTo(w * .01f, h * .58f, w * .16f, h * .45f, w * .31f, h * .49f)
                        cubicTo(w * .27f, h * .26f, w * .49f, h * .23f, w * .59f, h * .40f)
                        cubicTo(w * .76f, h * .29f, w * .93f, h * .43f, w * .81f, h * .57f)
                        cubicTo(w * 1.02f, h * .61f, w * .95f, h * .82f, w * .75f, h * .81f)
                        cubicTo(w * .60f, h * .97f, w * .44f, h * .84f, w * .39f, h * .82f)
                        cubicTo(w * .25f, h * .91f, w * .10f, h * .85f, w * .14f, h * .72f)
                        close()
                    }
                }
                drawPath(shape, cocoa.copy(alpha = .18f))
                drawPath(shape, cocoa, style = Stroke((1.5.dp * scale).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val fold = Path().apply {
                    moveTo(w * .32f, h * .65f)
                    quadraticTo(w * .51f, h * .73f, w * .70f, h * .64f)
                }
                drawPath(fold, cocoa.copy(alpha = .75f), style = Stroke((1.dp * scale).toPx(), cap = StrokeCap.Round))
            }
        } else {
            Icon(symbol, null, tint = color, modifier = Modifier.align(Alignment.Center).padding(top = (4.dp * scale)).size((20.dp * scale)))
        }
    }
}

internal fun com.example.data.model.TipoEvento.diaperIllustrationStatus(): EstadoFralda? = when (this) {
    com.example.data.model.TipoEvento.URINA -> EstadoFralda.URINA
    com.example.data.model.TipoEvento.FEZES -> EstadoFralda.FEZES
    com.example.data.model.TipoEvento.DIARREIA -> EstadoFralda.DIARREIA
    else -> null
}
