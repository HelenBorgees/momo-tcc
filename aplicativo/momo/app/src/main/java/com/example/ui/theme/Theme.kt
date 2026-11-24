package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCoral,
    onPrimary = Color.White,
    primaryContainer = PrimaryCoralLight,
    onPrimaryContainer = PrimaryCoralDark,
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCoralBackground,
    onSecondaryContainer = PrimaryText,
    tertiary = InformationBlue,
    onTertiary = Color.White,
    background = ApplicationBackground,
    onBackground = PrimaryText,
    surface = CardSurface,
    onSurface = PrimaryText,
    surfaceVariant = SoftGrayTrack,
    onSurfaceVariant = SecondaryText,
    outline = SoftBorder,
    error = CriticalAlert,
    onError = Color.White,
    errorContainer = CriticalAlertLight,
    onErrorContainer = CriticalAlert
)

@Composable
fun MOMOTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
