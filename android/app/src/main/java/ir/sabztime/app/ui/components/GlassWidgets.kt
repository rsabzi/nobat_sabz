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
import ir.sabztime.app.ui.theme.BurgundyDeep
import ir.sabztime.app.ui.theme.BurgundyGradient
import ir.sabztime.app.ui.theme.Cocoa
import ir.sabztime.app.ui.theme.CocoaSoft
import ir.sabztime.app.ui.theme.CreamLight
import ir.sabztime.app.ui.theme.DangerRose
import ir.sabztime.app.ui.theme.DangerRoseDeep
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBgEdge
import ir.sabztime.app.ui.theme.GoldInk
import ir.sabztime.app.ui.theme.GoldPale
import ir.sabztime.app.ui.theme.IconCreamBottom
import ir.sabztime.app.ui.theme.IconCreamTop
import ir.sabztime.app.ui.theme.InfoSapphire
import ir.sabztime.app.ui.theme.InfoSapphireDeep
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.MetalGoldGradient
import ir.sabztime.app.ui.theme.MetalGoldTop
import ir.sabztime.app.ui.theme.PillShape
import ir.sabztime.app.ui.theme.RingGold
import ir.sabztime.app.ui.theme.SignalGreenLight
import ir.sabztime.app.ui.theme.SuccessEmerald
import ir.sabztime.app.ui.theme.SuccessEmeraldDeep
import ir.sabztime.app.ui.theme.goldBorder
import ir.sabztime.app.ui.theme.goldRing
import ir.sabztime.app.ui.theme.metalPillBorder
import ir.sabztime.app.ui.theme.topGloss

// ═══════════════════════════════════════════════════════════════════════════
//  عناصر پایه‌ی طراحی «طلایی لوکس»: کاشی رنگی، دکمه‌ی فلزی، کارت شیشه‌ای، چیپ
// ═══════════════════════════════════════════════════════════════════════════

/** پالت رنگی کاشی‌های آیکون (نام‌های قدیمی برای سازگاری حفظ شده‌اند). */
enum class TilePalette { GREEN, MUTED, ERROR, AMBER, GOLD, EMERALD, SAPPHIRE, ROYAL }

/** برس طلایی برس‌خورده — برای آیکون‌ها و متن‌های شاخص */
val BrushedGold: Brush
    get() = Brush.linearGradient(listOf(GoldPale, Gold, GoldInk))

/**
 * رنگ‌آمیزی محتوای یک Box با برس (برای آیکون‌های وکتور «طلاییِ برس‌خورده»).
 * از BlendMode.SrcAtop استفاده می‌کند تا فقط پیکسل‌های خودِ آیکون رنگ شوند.
 */
fun Modifier.brushedGoldTint(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
    }

/**
 * سطح «قرص آبی برند» — گرادیان #1E6FE0→#0B4F9E با هایلایت بالا،
 * قاب برجسته (بالا روشن/پایین تیره) و سایه‌ی گرم.
 */
fun Modifier.metalPill(shape: Shape): Modifier = this
    .shadow(
        elevation = 9.dp,
        shape = shape,
        ambientColor = GoldBgEdge.copy(alpha = 0.5f),
        spotColor = GoldBgEdge.copy(alpha = 0.55f)
    )
    .clip(shape)
    .background(Brush.verticalGradient(MetalGoldGradient))
    .topGloss(0.32f)
    .border(metalPillBorder(), shape)

/** سطح «دکمه‌ی آیکون دایره‌ای روشن» — سفید→نرم آبی با حلقه‌ی آبی #1E6FE0 */
fun Modifier.creamCircle(shape: Shape, ringWidth: Dp = 1.6.dp): Modifier = this
    .shadow(
        elevation = 6.dp,
        shape = shape,
        ambientColor = GoldBgEdge.copy(alpha = 0.4f),
        spotColor = GoldBgEdge.copy(alpha = 0.45f)
    )
    .clip(shape)
    .background(Brush.verticalGradient(listOf(IconCreamTop, IconCreamBottom)))
    .topGloss(0.55f)
    .goldRing(shape, ringWidth)

private fun tileAccent(palette: TilePalette, selected: Boolean): List<Color> = when {
    !selected -> when (palette) {
        TilePalette.ERROR -> listOf(DangerRose, DangerRoseDeep)
        TilePalette.EMERALD -> listOf(SuccessEmerald, SuccessEmeraldDeep)
        TilePalette.SAPPHIRE -> listOf(InfoSapphire, InfoSapphireDeep)
        else -> listOf(CocoaSoft, Cocoa)
    }
    palette == TilePalette.ERROR -> listOf(Color(0xFFFDE8E8), Color(0xFFFFFFFF))
    palette == TilePalette.EMERALD -> listOf(Color(0xFFE7F4EC), Color(0xFFFFFFFF))
    palette == TilePalette.SAPPHIRE -> listOf(Color(0xFFE2EDFB), Color(0xFFFFFFFF))
    palette == TilePalette.ROYAL -> listOf(Color(0xFFE2EDFB), Color(0xFFA9C6EA))
    else -> listOf(CocoaSoft, Cocoa)
}

private fun tileBase(palette: TilePalette, selected: Boolean): List<Color> = when {
    !selected -> listOf(Color(0xE6FFFFFF), Color(0xD9EDF1F6)) // سفیدِ نیمه‌شفاف
    palette == TilePalette.ERROR -> listOf(DangerRose, DangerRoseDeep)
    palette == TilePalette.EMERALD -> listOf(SignalGreenLight, SuccessEmeraldDeep)
    palette == TilePalette.SAPPHIRE -> listOf(Color(0xFF3D76D6), InfoSapphireDeep)
    palette == TilePalette.ROYAL -> BurgundyGradient
    palette == TilePalette.MUTED -> listOf(Color(0xFFAFC0D2), Color(0xFF8FA3B8))
    else -> listOf(MetalGoldTop, Color(0xFF0B4F9E)) // کاشی آبی برند
}

/**
 * کاشی آیکون لوکس — «کاشی رنگی برند» با حلقه‌ی طلایی:
 * حالت فعال: سطح رنگی (طلا/زرشکی/زمردی/…) با آیکون متضاد؛
 * حالت خنثی: دایره‌ی کرم شفاف با آیکون قهوه‌ایِ محو.
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
    val shape = RoundedCornerShape(tileSize * 0.33f)
    val base = tileBase(palette, selected)
    val accent = tileAccent(palette, selected)

    Box(modifier = modifier.size(tileSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 7.dp,
                    shape = shape,
                    ambientColor = GoldBgEdge.copy(alpha = 0.45f),
                    spotColor = GoldBgEdge.copy(alpha = 0.5f)
                )
                .clip(shape)
                .background(Brush.verticalGradient(base))
                .topGloss(if (selected) 0.22f else 0.5f)
                .then(
                    if (selected && base.first() == MetalGoldTop) {
                        Modifier.border(metalPillBorder(), shape)
                    } else {
                        val ring = listOf(GoldPale, GoldInk, Gold, RingGold, GoldPale)
                            .map { if (selected) it else it.copy(alpha = 0.45f) }
                        Modifier.border(
                            BorderStroke(if (selected) 1.4.dp else 1.dp, Brush.sweepGradient(ring)),
                            shape
                        )
                    }
                )
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
 * دکمه‌ی اصلی (CTA) — قرص فلز طلای برجسته با متن قهوه‌ایِ تیره؛
 * برای پالت‌های رنگی (خطا/زمرد/…) سطح رنگی با متن کرم روشن.
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
    val (surface, border, contentColor) = when {
        !enabled -> Triple(
            listOf(Color(0xFFE4ECF5), Color(0xFFD0DCE8)),
            null as BorderStroke?,
            Cocoa.copy(alpha = 0.45f)
        )
        palette == TilePalette.ERROR -> Triple(
            listOf(Color(0xFFE05B5B), DangerRoseDeep),
            null,
            CreamLight
        )
        palette == TilePalette.EMERALD -> Triple(
            listOf(SignalGreenLight, SuccessEmeraldDeep),
            null,
            CreamLight
        )
        palette == TilePalette.ROYAL -> Triple(
            listOf(Color(0xFF1E6FE0), BurgundyDeep),
            null,
            CreamLight
        )
        else -> Triple(
            MetalGoldGradient,
            metalPillBorder(),
            Cocoa
        )
    }
    val finalBorder = border
    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = RingGold.copy(alpha = 0.55f),
                spotColor = GoldBgEdge.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(surface))
            .topGloss(if (enabled) 0.3f else 0f)
            .then(if (finalBorder != null) Modifier.border(finalBorder, shape) else Modifier)
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

/** دکمه‌ی ثانویه — کرمِ شفاف با حلقه‌ی طلایی و متن قهوه‌ای */
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
            .background(Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x59FFFFFF))))
            .topGloss(0.4f)
            .border(goldBorder(if (enabled) 0.75f else 0.25f, 1.3.dp), shape)
            .clickable(onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) Cocoa else CocoaSoft.copy(alpha = 0.45f)
        ) {
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
 * کارت لوکس اصلی — شیشه‌ی روشنِ «سفید→کهربایی» با قاب گرادیانی طلایی،
 * گوشه‌ی ۲۶dp و سایه‌ی گرم نرم.
 * (نام GlassCard برای سازگاری با صفحه‌های موجود حفظ شده است.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LuxCardShape,
    onClick: (() -> Unit)? = null,
    pattern: Boolean = false,
    gradient: List<Color> = ir.sabztime.app.ui.theme.NavyCardGradient,
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
            .background(Brush.verticalGradient(listOf(Color(0x73FFFFFF), Color(0x59EDF1F6))))
            .border(goldBorder(0.3f), shape)
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
                        if (sel) Brush.verticalGradient(MetalGoldGradient)
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
                            tint = if (sel) Cocoa else CocoaSoft
                        )
                        SpacerW(6)
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Cocoa else CocoaSoft.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * چیپ لوکس — انتخاب‌شده: قرص فلز طلاییِ برجسته؛ عادی: کرم شفاف با حلقه‌ی طلایی.
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
                if (selected) Brush.verticalGradient(MetalGoldGradient)
                else Brush.verticalGradient(listOf(Color(0x8CFFFFFF), Color(0x73EDF1F6)))
            )
            .border(goldBorder(if (selected) 0f else 0.45f), PillShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) Cocoa else CocoaSoft
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { content() }
        }
    }
}
