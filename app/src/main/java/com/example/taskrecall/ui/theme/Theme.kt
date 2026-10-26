package com.example.taskrecall.ui.theme

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

/**
 * 品牌色：低饱和绿，贴近“记录与归档”的安静感。
 * 规格未固定配色，这里只做最小可用的 Material 3 主题；
 * 深色模式在系统切换时自动生效。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1CF),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4E6355),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8D8),
    onSecondaryContainer = Color(0xFF0B1F14),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF98D5B4),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF145138),
    onPrimaryContainer = Color(0xFFB4F1CF),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF203528),
    secondaryContainer = Color(0xFF364B3E),
    onSecondaryContainer = Color(0xFFD0E8D8),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C2),
)

/**
 * 全局 Compose 主题。
 * Android 12+ 支持取系统壁纸色，规格未要求禁用，保留动态取色；
 * 低版本回退到上面的品牌色。
 */
@Composable
fun TaskRecallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
