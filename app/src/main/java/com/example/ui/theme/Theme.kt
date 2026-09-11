package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberNeonOrange,
    secondary = CyberNeonOrangeLight,
    tertiary = CrispWhite,
    background = DeepCarbonBlack,
    surface = DeepCarbonBlackVariant,
    onPrimary = CrispWhite,
    onSecondary = CrispWhite,
    onTertiary = DeepCarbonBlack,
    onBackground = CrispWhite,
    onSurface = CrispWhite,
    surfaceVariant = MutedGrey
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CyberNeonOrange,
    secondary = CyberNeonOrangeLight,
    tertiary = CrispWhite,
    background = CrispWhite,
    surface = DeepCarbonBlackVariant,
    onPrimary = CrispWhite,
    onSecondary = CrispWhite,
    onTertiary = DeepCarbonBlack,
    onBackground = DeepCarbonBlack,
    onSurface = CrispWhite,
    surfaceVariant = MutedGrey
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Cyber theme favors dark
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Use strict brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Enforce dark cyber theme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
