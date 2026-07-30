package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ShadowFighterColorScheme = darkColorScheme(
    primary = CyberEmber,
    onPrimary = Color.White,
    primaryContainer = CyberEmberContainer,
    onPrimaryContainer = Color(0xFFFFD5C0),
    secondary = ShadowPurple,
    secondaryContainer = Color(0xFF2A1040),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = ShadowCyan,
    background = ShadowObsidian,
    surface = ShadowCard,
    surfaceVariant = ShadowCardVariant,
    outline = ShadowCardBorder,
    onBackground = ShadowTextPrimary,
    onSurface = ShadowTextPrimary,
    onSurfaceVariant = ShadowTextSecondary
)

@Composable
fun CustomNotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShadowFighterColorScheme,
        typography = Typography,
        content = content
    )
}
