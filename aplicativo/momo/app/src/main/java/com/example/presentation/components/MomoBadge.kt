package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenLight

/**
 * Pill-shaped badge for status tags (e.g. URINA DETECTADA, FRALDA SECA, etc.)
 */
@Composable
fun MomoStatusBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Status pill de conectividade com proteção contra quebra de linha em "ONLINE" e "NÃO PAREADO".
 */
@Composable
fun DeviceStatusPill(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    isPaired: Boolean = true,
    isDemo: Boolean = false
) {
    val (bgColor, dotColor, textColor, text) = when {
        isDemo -> Tuple4(Color(0xFFF3E8FF), Color(0xFF9333EA), Color(0xFF7E22CE), "DEMO")
        !isPaired -> Tuple4(Color(0xFFFEF2F2), Color(0xFFEF4444), Color(0xFFB91C1C), "NÃO PAREADO")
        isOnline -> Tuple4(OnlineGreenLight, OnlineGreen, OnlineGreen, "ONLINE")
        else -> Tuple4(Color(0xFFF3F4F6), Color(0xFF9CA3AF), Color(0xFF6B7280), "OFFLINE")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
