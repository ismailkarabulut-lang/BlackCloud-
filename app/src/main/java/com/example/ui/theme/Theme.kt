package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BlackCloudColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepPurple,
    primaryContainer = SlateSurface,
    onPrimaryContainer = TextPrimary,
    secondary = NeonGreen,
    onSecondary = CarbonBlack,
    tertiary = WarnOrange,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = TextSecondary,
    error = GlowRed,
    onError = PureWhite,
    outline = TextMuted
)

@Composable
fun BlackCloudTheme(
    darkTheme: Boolean = true, // BlackCloud kabuğu siber stil gereği varsayılan koyu temadır
    content: @Composable () -> Unit
) {
    val colorScheme = BlackCloudColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Durum çubuğunu siyah yapıp siber havayı pekiştiriyoruz
                window.statusBarColor = CarbonBlack.toArgb()
                window.navigationBarColor = CarbonBlack.toArgb()
                
                val controller = WindowCompat.getInsetsController(window, view)
                // Açık renkli durum çubuğu ikonları (Koyu tema için)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
