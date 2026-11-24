package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BabyProfile
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText

/**
 * Top header of Dashboard showing "CUIDANDO DE", baby avatar, baby name, weight, calculated age,
 * and device connectivity status.
 */
@Composable
fun BabyHeader(
    babyProfile: BabyProfile?,
    isDeviceOnline: Boolean,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPaired: Boolean = true,
    isDemo: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("baby_header")
    ) {
        // Left side: Baby Avatar + Info (clickable to edit)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onEditProfileClick
                )
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryCoralLight)
                    .border(1.5.dp, PrimaryCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!babyProfile?.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = babyProfile?.photoUri,
                        contentDescription = "Foto de ${babyProfile?.name ?: "bebê"}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else if (babyProfile != null && babyProfile.name.isNotBlank()) {
                    Text(
                        text = babyProfile.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryCoral
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Avatar do bebê",
                        tint = PrimaryCoral,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "CUIDANDO DE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryText,
                    letterSpacing = 0.5.sp
                )
                val name = babyProfile?.name ?: "Bebê"
                val weight = babyProfile?.let { "${"%.0f".format(it.weightKg)} kg" } ?: ""
                val age = babyProfile?.formattedAge() ?: ""

                val subtitleParts = listOfNotNull(
                    if (weight.isNotBlank()) weight else null,
                    if (age.isNotBlank()) age else null
                ).joinToString(" • ")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    if (subtitleParts.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtitleParts,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryText
                        )
                    }
                }
            }
        }

        // Right side: Connectivity Status Pill
        DeviceStatusPill(
            isOnline = isDeviceOnline,
            isPaired = isPaired,
            isDemo = isDemo,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
