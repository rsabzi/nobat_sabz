package ir.sabztime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
val MaterialTheme.glass: GlassColors
    get() = LocalGlassColors.current

/** پس‌زمینه‌ی «شفق قطبی» — گرادیان نرم + حباب‌های رنگی محو، بدون نیاز به API 31 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val g = MaterialTheme.glass
    Box(modifier.background(Brush.verticalGradient(listOf(g.backdropTop, g.backdropBottom)))) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-90).dp, y = (-110).dp)
                .size(300.dp)
                .background(Brush.radialGradient(listOf(g.blobA, Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 90.dp)
                .size(260.dp)
                .background(Brush.radialGradient(listOf(g.blobB, Color.Transparent)), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 140.dp)
                .size(280.dp)
                .background(Brush.radialGradient(listOf(g.blobA.copy(alpha = g.blobA.alpha * 0.6f), Color.Transparent)), CircleShape)
        )
    }
}

private fun Modifier.glassShape(
    shape: Shape,
    top: Color,
    bottom: Color,
    strokeTop: Color,
    strokeBottom: Color,
    elevation: Dp
): Modifier = this
    .shadow(elevation = elevation, shape = shape)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(top, bottom)))
    .border(1.dp, Brush.verticalGradient(listOf(strokeTop, strokeBottom)), shape)

/**
 * کارت شیشه‌ای استاندارد اپلیکیشن.
 * اگر onClick بدهید، کل کارت کلیک‌پذیر می‌شود (هدف لمسی کامل).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val g = MaterialTheme.glass
    Box(
        modifier
            .glassShape(shape, g.cardTop, g.cardBottom, g.strokeTop, g.strokeBottom, 5.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

/** پنل شیشه‌ای آزاد (نوار پایین، جستجو و…) با محتوای BoxScope */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val g = MaterialTheme.glass
    Box(
        modifier
            .glassShape(shape, g.cardTop, g.cardBottom, g.strokeTop, g.strokeBottom, elevation),
        content = content
    )
}

/**
 * آیکون سه‌بعدی کاربرپسند: مربعِ گرد با گرافیک برند، لبه‌ی روشن بالایی
 * و سایه‌ی نرم — حس عمق (۳بعدی) روی همه‌ی نسخه‌های اندروید.
 */
@Composable
fun IconBadge3D(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerTop: Color? = null,
    containerBottom: Color? = null,
    contentDescription: String? = null
) {
    val g = MaterialTheme.glass
    val top = containerTop ?: g.badgeTop
    val bottom = containerBottom ?: g.badgeBottom
    val shape = RoundedCornerShape((size.value / 2.6f).dp)
    Box(
        modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))), shape)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/** نشان اموجی سه‌بعدی — اموجی‌های سیستمی خودشان رنگی و برجسته‌اند */
@Composable
fun EmojiBadge3D(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    tint: Color? = null
) {
    val g = MaterialTheme.glass
    val bg = tint ?: g.badgeGlow.copy(alpha = g.badgeGlow.alpha * 0.4f)
    Box(
        modifier
            .shadow(elevation = 5.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = bg.alpha + 0.08f), bg)))
            .border(1.dp, Brush.verticalGradient(listOf(g.strokeTop, g.strokeBottom)), CircleShape)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.48f).sp)
    }
}

/** دکمه‌ی اصلی با گرافیک برند و سایه‌ی نرم */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingEmoji: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier
            .shadow(elevation = if (enabled) 8.dp else 0.dp, shape = shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.glass.badgeTop, MaterialTheme.glass.badgeBottom)
                )
            )
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f * alpha), Color.Transparent)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingEmoji != null) {
                Text(leadingEmoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = Color.White.copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/** برچسب کوچک شیشه‌ای (chips) */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val g = MaterialTheme.glass
    Box(
        modifier
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) Brush.verticalGradient(listOf(g.badgeTop, g.badgeBottom))
                else Brush.verticalGradient(listOf(g.cardTop, g.cardBottom))
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(g.strokeTop, g.strokeBottom)),
                RoundedCornerShape(50)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
