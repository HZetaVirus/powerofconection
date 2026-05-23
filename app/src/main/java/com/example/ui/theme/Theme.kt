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

// We define Slate150Fallback here or import it
private val Slate150Fallback = Color(0xFFE2E8F0)

private val DarkColorScheme =
  darkColorScheme(
    primary = UerjBlue,
    secondary = UerjGreen,
    tertiary = UerjYellow,
    background = Slate900,
    surface = Slate800,
    onPrimary = White,
    onSecondary = White,
    onBackground = Slate50,
    onSurface = Slate150Fallback
  )

private val LightColorScheme =
  lightColorScheme(
    primary = UerjBlue,
    secondary = UerjGreen,
    tertiary = UerjYellow,
    background = Slate50,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onBackground = Slate900,
    onSurface = Slate800
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom colors preserved by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
