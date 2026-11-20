package com.example.moneytrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MoneyTrackDarkColors = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ExpenseRed
)

@Composable
fun MoneyTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoneyTrackDarkColors,
        typography = Typography,
        content = content
    )
}