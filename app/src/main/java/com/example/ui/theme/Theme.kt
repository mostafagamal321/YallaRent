package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = ForestGreen,
  onPrimary = Color.White,
  primaryContainer = MintLight,
  onPrimaryContainer = MintDark,
  secondary = EmeraldMedium,
  onSecondary = Color.White,
  tertiary = AmberAccent,
  onTertiary = Color.White,
  background = SoftBackground,
  onBackground = TextDark,
  surface = CardSurface,
  onSurface = TextDark,
  error = RedAlert,
  onError = Color.White
)

@Composable
fun RentManagerTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}
