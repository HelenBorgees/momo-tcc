package com.example.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText

/**
 * Official MOMO standalone mark:
 * Diapered peach with leaves and "MO MO" diaper print.
 * References the official logo asset directly.
 */
@Composable
fun MomoLogoVisual(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.momo_logo),
            contentDescription = "Logo MOMO",
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Official MOMO full horizontal wordmark:
 * "M", diapered peach, "M", "O".
 */
@Composable
fun MomoLogoFull(
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
    height: Dp = 60.dp
) {
    Box(
        modifier = modifier.size(width = width, height = height),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_momo_logo_full),
            contentDescription = "MOMO",
            modifier = Modifier.size(width = width, height = height)
        )
    }
}

/**
 * Official MOMO brand title:
 * Exactly "MOMO" (official app title).
 */
@Composable
fun MomoBrandTitle(
    modifier: Modifier = Modifier,
    fontSize: Int = 36
) {
    Text(
        text = "MOMO",
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Black,
        color = PrimaryCoral,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

/**
 * Official MOMO written logomarca (momo_2.png):
 * Full written "MOMO" stylized wordmark with the diapered peach as the first "O".
 */
@Composable
fun MomoLogomarca(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    contentDescription: String = "MOMO"
) {
    // Aspect ratio of momo_2.png is 1218 / 425 ~= 2.866
    val width = height * 2.866f
    Image(
        painter = painterResource(id = R.drawable.momo_2),
        contentDescription = contentDescription,
        modifier = modifier.size(width = width, height = height)
    )
}

