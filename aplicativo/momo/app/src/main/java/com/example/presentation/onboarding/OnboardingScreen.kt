package com.example.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.presentation.components.MomoPrimaryButton
import com.example.ui.theme.ApplicationBackground
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftGrayTrack

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Monitoramento Inteligente",
            description = "Acompanhe as condições da fralda através do sistema de monitoramento.",
            icon = Icons.Default.Sensors
        ),
        OnboardingPageData(
            title = "Alertas Importantes",
            description = "Receba alertas quando forem identificadas condições que indiquem necessidade de troca.",
            icon = Icons.Default.NotificationsActive
        ),
        OnboardingPageData(
            title = "Mais Tranquilidade",
            description = "Acompanhe o histórico de eventos e trocas de forma simples.",
            icon = Icons.Default.VolunteerActivism
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApplicationBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("onboarding_screen")
    ) {
        // Top Institutional Branding Header + Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.momo_logo),
                    contentDescription = "Logo MOMO",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MOMO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryCoral
                )
            }

            if (currentPage < pages.size - 1) {
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Pular",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText
                    )
                }
            }
        }

        // Animated Page Content
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.align(Alignment.Center),
            label = "onboarding_content"
        ) { targetIndex ->
            val page = pages[targetIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Large Circular Icon Background
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(PrimaryCoralLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = PrimaryCoral,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Bottom Navigation Block (Indicators + Next Button)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryCoral else SoftGrayTrack)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val isLastPage = currentPage == pages.size - 1
            MomoPrimaryButton(
                text = if (isLastPage) "Começar Agora" else "Próximo",
                onClick = {
                    if (isLastPage) {
                        onFinishOnboarding()
                    } else {
                        currentPage++
                    }
                },
                testTag = if (isLastPage) "onboarding_start_button" else "onboarding_next_button"
            )
        }
    }
}
