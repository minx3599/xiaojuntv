package top.yogiczy.mytv.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import top.yogiczy.mytv.core.designsystem.theme.darkColors
import top.yogiczy.mytv.core.designsystem.theme.lightColors

private val XiaojunGold = Color(0xFFFFC857)
private val XiaojunGoldOn = Color(0xFF2A1B00)
private val XiaojunGoldContainer = Color(0xFF5A430D)
private val XiaojunGoldOnContainer = Color(0xFFFFE8A6)
private val XiaojunMint = Color(0xFF8FD3C7)
private val XiaojunMintOn = Color(0xFF003731)

val colorSchemeForDarkMode = darkColorScheme(
    primary = XiaojunGold,
    onPrimary = XiaojunGoldOn,
    primaryContainer = XiaojunGoldContainer,
    onPrimaryContainer = XiaojunGoldOnContainer,
    secondary = XiaojunMint,
    onSecondary = XiaojunMintOn,
    secondaryContainer = Color(0xFF0B4D45),
    onSecondaryContainer = Color(0xFFB5F2E9),
    tertiary = darkColors.tertiary,
    onTertiary = darkColors.onTertiary,
    tertiaryContainer = darkColors.tertiaryContainer,
    onTertiaryContainer = darkColors.onTertiaryContainer,
    error = darkColors.error,
    onError = darkColors.onError,
    background = Color(0xFF0B0D0F),
    onBackground = darkColors.onBackground,
    surface = Color(0xFF111417),
    onSurface = darkColors.onSurface,
    surfaceVariant = darkColors.surfaceVariant,
    onSurfaceVariant = darkColors.onSurfaceVariant,
    border = darkColors.outline,
    borderVariant = darkColors.outlineVariant,
    scrim = darkColors.scrim,
    inverseSurface = darkColors.inverseSurface,
    inverseOnSurface = darkColors.inverseOnSurface,
    inversePrimary = Color(0xFF8D6800),
    errorContainer = darkColors.errorContainer,
    onErrorContainer = darkColors.onErrorContainer,
)

val colorSchemeForLightMode = lightColorScheme(
    primary = lightColors.primary,
    onPrimary = lightColors.onPrimary,
    primaryContainer = lightColors.primaryContainer,
    onPrimaryContainer = lightColors.onPrimaryContainer,
    secondary = lightColors.secondary,
    onSecondary = lightColors.onSecondary,
    secondaryContainer = lightColors.secondaryContainer,
    onSecondaryContainer = lightColors.onSecondaryContainer,
    tertiary = lightColors.tertiary,
    onTertiary = lightColors.onTertiary,
    tertiaryContainer = lightColors.tertiaryContainer,
    onTertiaryContainer = lightColors.onTertiaryContainer,
    error = lightColors.error,
    onError = lightColors.onError,
    background = lightColors.background,
    onBackground = lightColors.onBackground,
    surface = lightColors.surface,
    onSurface = lightColors.onSurface,
    surfaceVariant = lightColors.surfaceVariant,
    onSurfaceVariant = lightColors.onSurfaceVariant,
    border = lightColors.outline,
    borderVariant = lightColors.outlineVariant,
    scrim = lightColors.scrim,
    inverseSurface = lightColors.inverseSurface,
    inverseOnSurface = lightColors.inverseOnSurface,
    inversePrimary = lightColors.inversePrimary,
    errorContainer = lightColors.errorContainer,
    onErrorContainer = lightColors.onErrorContainer,
)
