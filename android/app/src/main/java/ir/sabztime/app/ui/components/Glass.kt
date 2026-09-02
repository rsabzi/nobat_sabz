package ir.sabztime.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import ir.sabztime.app.ui.theme.Accent
import ir.sabztime.app.ui.theme.BrandGreen

/**
 * رنگ‌های لایه‌ی شیشه‌ای (Glassmorphism).
 * کارت‌ها نیمه‌شفاف با لبه‌ی روشن و سایه‌ی نرم هستند تا حس عمق شیشه بدهند
 * و در هر دو تم روشن/تاریک به‌صورت خودکار سازگارند.
 */
@Immutable
data class GlassColors(
    val cardTop: Color,
    val cardBottom: Color,
    val strokeTop: Color,
    val strokeBottom: Color,
    val navBar: Color,
    val backdropTop: Color,
    val backdropBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val badgeTop: Color,
    val badgeBottom: Color,
    val badgeGlow: Color
)

val LightGlass = GlassColors(
    cardTop = Color.White.copy(alpha = 0.72f),
    cardBottom = Color.White.copy(alpha = 0.48f),
    strokeTop = Color.White.copy(alpha = 0.90f),
    strokeBottom = BrandGreen.copy(alpha = 0.16f),
    navBar = Color.White.copy(alpha = 0.78f),
    backdropTop = Color(0xFFE3F4EA),
    backdropBottom = Color(0xFFF6FBF8),
    blobA = BrandGreen.copy(alpha = 0.30f),
    blobB = Accent.copy(alpha = 0.22f),
    badgeTop = Color(0xFF3CB871),
    badgeBottom = Color(0xFF128A4C),
    badgeGlow = BrandGreen.copy(alpha = 0.35f)
)

val DarkGlass = GlassColors(
    cardTop = Color.White.copy(alpha = 0.07f),
    cardBottom = Color.White.copy(alpha = 0.035f),
    strokeTop = Color.White.copy(alpha = 0.16f),
    strokeBottom = Color.White.copy(alpha = 0.04f),
    navBar = Color(0xFF121B16).copy(alpha = 0.86f),
    backdropTop = Color(0xFF090E0B),
    backdropBottom = Color(0xFF111A14),
    blobA = Color(0xFF34B36A).copy(alpha = 0.20f),
    blobB = Accent.copy(alpha = 0.10f),
    badgeTop = Color(0xFF4ADE80),
    badgeBottom = Color(0xFF15803D),
    badgeGlow = Color(0xFF34B36A).copy(alpha = 0.30f)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlass }

/** دسترسی کوتاه: MaterialTheme.glass */
@Composable
val MaterialTheme.glass: GlassColors
    get() = LocalGlassColors.current

