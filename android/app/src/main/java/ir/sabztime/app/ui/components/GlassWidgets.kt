package ir.sabztime.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ir.sabztime.app.ui.theme.GlassGradientAmberDark
import ir.sabztime.app.ui.theme.GlassGradientAmberLight
import ir.sabztime.app.ui.theme.GlassGradientDark
import ir.sabztime.app.ui.theme.GlassGradientErrorDark
import ir.sabztime.app.ui.theme.GlassGradientErrorLight
import ir.sabztime.app.ui.theme.GlassGradientGreenDark
import ir.sabztime.app.ui.theme.GlassGradientGreenLight
import ir.sabztime.app.ui.theme.GlassGradientLight
import ir.sabztime.app.ui.theme.GlassGradientMutedDark
import ir.sabztime.app.ui.theme.GlassGradientMutedLight
import ir.sabztime.app.ui.theme.LocalSabzDarkTheme

// ---------------------------------------------------------------------------
// پالت‌های آیکون سه‌بعدی
//   BLUE  = برند/ناوبری/CTA   GREEN = سیگنال (نوبت آزاد، موفقیت، تأیید)
// ---------------------------------------------------------------------------

enum class TilePalette { BLUE, GREEN, MUTED, ERROR, AMBER }

private fun tileColors(palette: TilePalette, selected: Boolean, dark: Boolean): List<Color> = when {
    !selected -> if (dark) GlassGradientMutedDark else GlassGradientMutedLight
    palette == TilePalette.ERROR -> if (dark) GlassGradientErrorDark else GlassGradientErrorLight
    palette == TilePalette.AMBER -> if (dark) GlassGradientAmberDark else GlassGradientAmberLight
    palette == TilePalette.GREEN -> if (dark) GlassGradientGreenDark else GlassGradientGreenLight
    else -> if (dark) GlassGradientDark else GlassGradientLight
}

/**
 * آیکون «سه‌بعدی» — کاشی گرادیانی با جیغ (gloss) بالا و سایه.
 * پیش‌فرض: آبی برند؛ برای «نوبت آزاد/موفقیت» از [TilePalette.GREEN] استفاده کنید.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tileSize: Dp = 44.dp,
    selected: Boolean = true,
    palette: TilePalette = TilePalette.BLUE,
    contentDescription: String? = null
) {
    val dark = LocalSabzDarkTheme.current
    val colors = tileColors(palette, selected, dark)
    val shape = RoundedCornerShape(tileSize * 0.30f)
    Box(modifier = modifier.size(tileSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(3.dp, shape)
                .background(Brush.verticalGradient(colors), shape)
        )
        // لایه‌ی gloss (نور از بالا)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (dark) 0.14f else 0.30f),
                            Color.White.copy(alpha = 0f)
                        )
                    ),
                    shape = shape
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(tileSize * 0.52f),
            tint = Color.White
        )
    }
}

// ---------------------------------------------------------------------------
// دکمه گرادیانی (CTA اصلی) — رنگ پیش‌فرض: آبی برند
// ---------------------------------------------------------------------------

@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: TilePalette = TilePalette.BLUE,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    val dark = LocalSabzDarkTheme.current
    val colors = tileColors(palette, enabled, dark)
    val textColor = if (enabled) Color.White else if (dark) Color(0xFF8B96A5) else Color(0xFF6B7686)
    Box(
        modifier = modifier
            .shadow(if (enabled) 5.dp else 0.dp, shape)
            .background(Brush.verticalGradient(colors), shape)
            .clip(shape)
            .clickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// کارت شیشه‌ای
// ---------------------------------------------------------------------------

/**
 * کارت شیشه‌ای اصلی رابط — شفاف، با خط دور نازک، جیغ محو و سایه‌ی نرم.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick) else modifier.fillMaxWidth()
    ir.sabztime.app.ui.theme.GlassSurface(modifier = m, shape = shape) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

/**
 * «کنترل قطعه‌ای» (Segmented control) برای انتخاب‌های دو/چندتایی
 * مثل ورود/ثبت‌نام یا حالت پوسته — انتخاب‌شده با آبی برند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList()
) {
    val dark = LocalSabzDarkTheme.current
    val shape = RoundedCornerShape(14.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val gradient = Brush.verticalGradient(if (dark) GlassGradientDark else GlassGradientLight)
            Surface(
                onClick = { onSelect(i) },
                modifier = Modifier.weight(1f),
                shape = shape,
                color = when {
                    sel -> Color.Transparent
                    dark -> Color.White.copy(alpha = 0.06f)
                    else -> Color.White.copy(alpha = 0.55f)
                },
                border = if (sel) BorderStroke(1.dp, Color.Transparent)
                else BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.16f) else Color(0xFFD3DAE3)),
                tonalElevation = 0.dp,
                contentColor = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    if (sel) Box(Modifier.matchParentSize().background(gradient, shape))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        icons.getOrNull(i)?.let { ic ->
                            Icon(
                                ic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SpacerW(6)
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// چیپ شیشه‌ای (جایگزین FilterChip با کنترل کامل رنگ‌ها)
// ---------------------------------------------------------------------------

/**
 * چیپ انتخابی شیشه‌ای — حالت انتخاب‌شده با گرادیان آبی برند، غیرانتخاب‌شده شفاف.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val dark = LocalSabzDarkTheme.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = when {
                    selected -> Color.Transparent
                    dark -> Color.White.copy(alpha = 0.18f)
                    else -> Color(0xFFD3DAE3)
                },
                shape = shape
            )
            .background(
                if (selected) {
                    Brush.verticalGradient(if (dark) GlassGradientDark else GlassGradientLight)
                } else {
                    Brush.verticalGradient(
                        listOf(
                            if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.7f),
                            if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.5f)
                        )
                    )
                },
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

/** فاصله‌ی افقی کوچک — از Common.kt (SpacerW(Int)) استفاده می‌شود */
