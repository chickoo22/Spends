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

private val DarkColorScheme = darkColorScheme(
    primary = FinanceGreenBright,
    onPrimary = Color(0xFF022416),
    primaryContainer = FinanceGreenContainerDark,
    onPrimaryContainer = FinanceGreenOnContainerDark,
    secondary = FinanceBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFF94A3B8),
    tertiary = FinanceAmber,
    background = SlateDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = SlateDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SlateDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FinanceGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FinanceGreenContainerLight,
    onPrimaryContainer = FinanceGreenOnContainerLight,
    secondary = FinanceBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF334155),
    tertiary = FinanceAmber,
    background = SlateLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = SlateLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded financial styling authoritative & consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MyApplicationTheme(darkTheme, dynamicColor, content)
