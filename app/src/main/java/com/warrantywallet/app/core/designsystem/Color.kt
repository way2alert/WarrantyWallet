package com.warrantywallet.app.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary Brand Colors: Deep Navy & Indigo
val DeepNavy = Color(0xFF0B1B3D)
val DarkIndigo = Color(0xFF162A5A)
val MediumIndigo = Color(0xFF1E3A70)
val SoftIndigo = Color(0xFF2E4C8E)

// Accent: Emerald Mint & Teal
val EmeraldAccent = Color(0xFF00A86B)
val TealAccent = Color(0xFF0D9488)
val MintContainerLight = Color(0xFFD1FAE5)
val MintContainerDark = Color(0xFF064E3B)

// Neutrals: Warm Off-White & Crisp Charcoal
val WarmBackgroundLight = Color(0xFFF8F9FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F3F5)
val TextPrimaryLight = Color(0xFF1A1D20)
val TextSecondaryLight = Color(0xFF6C757D)
val DividerLight = Color(0xFFE9ECEF)

// Dark Theme Neutrals
val BackgroundDark = Color(0xFF0B0F19)
val SurfaceDark = Color(0xFF131B2E)
val SurfaceVariantDark = Color(0xFF1E2942)
val TextPrimaryDark = Color(0xFFF8F9FA)
val TextSecondaryDark = Color(0xFFA0AEC0)
val DividerDark = Color(0xFF2D3748)

// Status Colors
val StatusActiveGreen = Color(0xFF10B981)
val StatusActiveContainer = Color(0xFFD1FAE5)
val StatusExpiringAmber = Color(0xFFF59E0B)
val StatusExpiringContainer = Color(0xFFFEF3C7)
val StatusExpiredRed = Color(0xFFEF4444)
val StatusExpiredContainer = Color(0xFFFEE2E2)
val StatusUnknownGray = Color(0xFF6B7280)
val StatusUnknownContainer = Color(0xFFF3F4F6)

val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = Color.White,
    primaryContainer = MediumIndigo,
    onPrimaryContainer = Color.White,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = MintContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = TealAccent,
    background = WarmBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    error = StatusExpiredRed,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = DeepNavy,
    primaryContainer = MediumIndigo,
    onPrimaryContainer = Color.White,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = Color(0xFF6EE7B7),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    error = StatusExpiredRed,
    onError = Color.White
)
