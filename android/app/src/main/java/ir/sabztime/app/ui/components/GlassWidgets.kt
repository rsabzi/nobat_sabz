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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.sabztime.app.ui.theme.Burgundy
import ir.sabztime.app.ui.theme.BurgundyDeep
import ir.sabztime.app.ui.theme.Cream
import ir.sabztime.app.ui.theme.DangerRose
import ir.sabztime.app.ui.theme.DangerRoseDeep
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBright
import ir.sabztime.app.ui.theme.GoldDeep
import ir.sabztime.app.ui.theme.GoldDim
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.InfoSapphire
import ir.sabztime.app.ui.theme.InfoSapphireDeep
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.MidnightDeep
import ir.sabztime.app.ui.theme.NavyCardGradient
import ir.sabztime.app.ui.theme.NavyInk
import ir.sabztime.app.ui.theme.PillShape
import ir.sabztime.app.ui.theme.SuccessEmerald
import ir.sabztime.app.ui.theme.SuccessEmeraldDeep
import ir.sabztime.app.ui.theme.TextMuted
import ir.sabztime.app.ui.theme.goldBorder

// ═══════════════════════════════════════════════════════════════════════════
//  عناصر پایه‌ی طراحی «پرایم»: کاشی آیکون سه‌بعدی، دکمه، کارت، چیپ
// ═══════════════════════════════════════════════════════════════════════════

/** پالت رنگی کاشی‌های آیکون (نام‌های قدیمی برای سازگاری حفظ شده‌اند). */
enum class TilePalette { GREEN, MUTED, ERROR, AMBER, GOLD, EMERALD, SAPPHIRE, ROYAL }

/** برس طلایی برس‌خورده — برای آیکون‌ها و متن‌های شاخص */
val BrushedGold: Brush
    get() = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep, GoldBright))

/**
 * رنگ‌آمیزی محتوای یک Box با برس طلایی (برای آیکون‌های وکتور «طلاییِ برس‌خورده»).
 * از BlendMode.SrcAtop استفاده می‌کند تا فقط پیکسل‌های خودِ آیکون طلایی شوند.
 */
fun Modifier.brushedGoldTint(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
    }

private fun tileAccent(palette: TilePalette, selected: Boolean): List<Color> = when {
    !selected -> listOf(TextMuted, Color(0xFF6C7591))
    palette == TilePalette.ERROR -> listOf(Color(0xFFFFB3C1), DangerRose, DangerRoseDeep)
    palette == TilePalette.EMERALD -> listOf(Color(0xFF9FF0D2), SuccessEmerald, SuccessEmeraldDeep)
    palette == TilePalette.SAPPHIRE -> listOf(Color(0xFFAFCBFF), InfoSapphire, InfoSapphireDeep)
    palette == TilePalette.ROYAL -> listOf(Color(0xFFE7B7C6), Color(0xFFB2647C), BurgundyDeep)
    else -> listOf(GoldLight, Gold, GoldDeep, GoldBright)
}

private fun tileBase(palette: TilePalette, selected: Boolean): List<Color> = when {
    !selected -> listOf(Color(0xFF1B2136), Color(0xFF11162A))
    palette == TilePalette.ERROR -> listOf(Color(0xFF3A1220), Color(0xFF21090F))
    palette == TilePalette.EMERALD -> listOf(Color(0xFF0E2A24), Color(0xFF07140F))
    palette == TilePalette.SAPPHIRE -> listOf(Color(0xFF10213F), Color(0xFF070E1C))
    palette == TilePalette.ROYAL -> listOf(Burgundy, BurgundyDeep)
    else -> listOf(Color(0xFF16213F), NavyInk)
}

/**
 * کاشی آیکون سه‌بعدی — مربع سرمه‌ای تیره‌ی گرد با آیکون طلاییِ برس‌خورده،
 * حاشیه‌ی مویی طلایی، درخشش داخلی و سایه‌ی نرم (Soft Neumorphism).
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tileSize: Dp = 46.dp,
    selected: Boolean = true,
    palette: TilePalette = TilePalette.GOLD,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(tileSize * 0.32f)
    val accent = tileAccent(palette, selected)
    val base = tileBase(palette, selected)
    val ring = accent.first().copy(alpha = if (selected) 0.42f else 0.18f)

    Box(modifier = modifier.size(tileSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(8.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(Brush.verticalGradient(base))
                .background(
                    Brush.radialGradient(
                        listOf(accent[0].copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .border(BorderStroke(1.dp, ring), shape)
        )
        Box(
            Modifier
                .size(tileSize * 0.52f)
                .brushedGoldTint(Brush.linearGradient(accent))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                tint = Color.White
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  دکمه‌ها
// ───────────────────────────────────────────────────────────────────────────

/**
 * دکمه‌ی اصلی (CTA) — سطح طلایی برس‌خورده با متن سرمه‌ای، سایه‌ی گرم و لبه‌ی روشن.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: TilePalette = TilePalette.GOLD,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    val gradient = when {
        !enabled -> listOf(Color(0xFF262C42), Color(0xFF191E2F))
        palette == TilePalette.ERROR -> listOf(Color(0xFFE0798F), DangerRoseDeep)
        palette == TilePalette.EMERALD -> listOf(SuccessEmerald, SuccessEmeraldDeep)
        palette == TilePalette.ROYAL -> listOf(Color(0xFF8C2342), BurgundyDeep)
        else -> listOf(GoldLight, Gold, GoldDim)
    }
    val contentColor = when {
        !enabled -> TextMuted
        palette == TilePalette.GOLD || palette == TilePalette.AMBER || palette == TilePalette.GREEN -> MidnightDeep
        else -> Cream
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = Gold.copy(alpha = 0.6f),
                spotColor = Gold.copy(alpha = 0.7f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(gradient))
            .border(BorderStroke(1.dp, GoldLight.copy(alpha = if (enabled) 0.55f else 0.10f)), shape)
            .clickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                Modifier.padding(horizontal = 22.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

/** دکمه‌ی ثانویه — شفاف با حاشیه‌ی طلایی و متن کرم */
@Composable
fun OutlineGoldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color(0x0DFFFFFF))))
            .border(goldBorder(if (enabled) 0.45f else 0.12f), shape)
            .clickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) Cream else TextMuted) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) { content() }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  کارت‌ها
// ───────────────────────────────────────────────────────────────────────────

/**
 * کارت لوکس اصلی — سرمه‌ای گرادیانی، حاشیه‌ی مویی طلایی، گوشه‌ی ۲۵dp و سایه‌ی عمیق.
 * (نام GlassCard برای سازگاری با صفحه‌های موجود حفظ شده است.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LuxCardShape,
    onClick: (() -> Unit)? = null,
    pattern: Boolean = false,
    gradient: List<Color> = NavyCardGradient,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier.fillMaxWidth()
    ir.sabztime.app.ui.theme.GlassSurface(
        modifier = if (onClick != null) base.clip(shape).clickable(onClick = onClick) else base,
        shape = shape,
        pattern = pattern,
        gradient = gradient
    ) {
        Column(Modifier.padding(contentPadding)) { content() }
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  کنترل قطعه‌ای و چیپ‌ها
// ───────────────────────────────────────────────────────────────────────────

@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList()
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x14FFFFFF))
            .border(goldBorder(0.20f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val innerShape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(innerShape)
                    .background(
                        if (sel) Brush.verticalGradient(listOf(GoldLight, GoldDim))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icons.getOrNull(i)?.let { ic ->
                        Icon(
                            ic,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (sel) MidnightDeep else GoldDim
                        )
                        SpacerW(6)
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) MidnightDeep else Cream.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

/**
 * چیپ لوکس — انتخاب‌شده: سطح طلایی؛ عادی: سرمه‌ای شفاف با حاشیه‌ی طلایی محو.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(
                if (selected) Brush.verticalGradient(listOf(GoldLight, GoldDim))
                else Brush.verticalGradient(listOf(Color(0x1AFFFFFF), Color(0x0DFFFFFF)))
            )
            .border(goldBorder(if (selected) 0.0f else 0.30f), PillShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) MidnightDeep else Cream) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { content() }
        }
    }
}
