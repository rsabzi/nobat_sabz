package ir.sabztime.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════
//  لایه‌های پایه‌ی بصری: پس‌زمینه‌ی گرادیانی، بافت طلایی، سطوح نئومورفیک
// ═══════════════════════════════════════════════════════════════════════════

/** حاشیه‌ی مویی طلایی — امضای همه‌ی کارت‌های لوکس */
fun goldBorder(alpha: Float = 0.34f, width: Dp = 1.dp): BorderStroke =
    BorderStroke(width, Gold.copy(alpha = alpha))

/** حاشیه‌ی گرادیانی طلایی (برای عناصر شاخص مثل سرچ‌بار و نوار پیمایش) */
fun goldGradientBorder(width: Dp = 1.dp): BorderStroke =
    BorderStroke(width, Brush.linearGradient(GoldHairline))

/**
 * پس‌زمینه‌ی اصلی همه‌ی صفحه‌ها:
 * گرادیان عمودی «سرمه‌ای سوخته → زغالی» + هاله‌های نرم طلایی و زرشکی
 * که به صفحه عمق و حس نورپردازی استودیویی می‌دهند.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(ScreenGradient))
            .drawBehind {
                // هاله‌ی طلایی بالا-راست (منبع نور اصلی)
                radialGlow(
                    center = Offset(size.width * 0.92f, -size.height * 0.02f),
                    radius = size.width * 1.05f,
                    color = Gold.copy(alpha = 0.13f)
                )
                // هاله‌ی سرمه‌ای روشن بالا-چپ
                radialGlow(
                    center = Offset(size.width * 0.05f, size.height * 0.12f),
                    radius = size.width * 0.85f,
                    color = Color(0xFF2A4380).copy(alpha = 0.30f)
                )
                // هاله‌ی زرشکی پایین-چپ
                radialGlow(
                    center = Offset(size.width * 0.10f, size.height * 0.92f),
                    radius = size.width * 0.95f,
                    color = BurgundyLight.copy(alpha = 0.22f)
                )
                // برق طلایی محو پایین-راست
                radialGlow(
                    center = Offset(size.width * 0.95f, size.height * 0.78f),
                    radius = size.width * 0.6f,
                    color = GoldDeep.copy(alpha = 0.10f)
                )
            }
    ) {
        content()
    }
}

private fun DrawScope.radialGlow(center: Offset, radius: Float, color: Color) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}

/**
 * بافت خطوط طلایی ظریف (Pattern) — خطوط مورب موازی با شفافیت بسیار کم.
 * روی کارت‌های سرمه‌ای لوکس کشیده می‌شود و حس پارچه/چرم گران‌قیمت می‌دهد.
 */
fun Modifier.goldLinePattern(
    alpha: Float = 0.075f,
    spacing: Dp = 14.dp,
    strokeWidth: Dp = 1.dp
): Modifier = this.drawBehind {
    val step = spacing.toPx().coerceAtLeast(4f)
    val sw = strokeWidth.toPx()
    val diagonal = size.width + size.height
    var x = -size.height
    while (x < diagonal) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Gold.copy(alpha = alpha * 0.25f),
                    Gold.copy(alpha = alpha),
                    Gold.copy(alpha = alpha * 0.25f)
                ),
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height)
            ),
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = sw
        )
        x += step
    }
}

/** نور محو گوشه‌ی بالای کارت (Gloss) — حس شیشه/لاک براق */
fun Modifier.topGloss(alpha: Float = 0.10f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.55f
        )
    )
}

/** خط نور طلایی روی لبه‌ی بالایی کارت (Rim light) */
fun Modifier.goldRim(alpha: Float = 0.35f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                GoldLight.copy(alpha = alpha),
                Color.Transparent
            )
        ),
        size = androidx.compose.ui.geometry.Size(size.width, 1.2f)
    )
}

/** حلقه‌ی نئونی طلایی (Neon Glow) دور عنصر فعال */
fun Modifier.neonGlow(
    color: Color = Gold,
    radiusFactor: Float = 1.15f,
    alpha: Float = 0.55f
): Modifier = this.drawBehind {
    val r = (size.minDimension / 2f) * radiusFactor * 1.8f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.55f),
                color.copy(alpha = alpha * 0.18f),
                Color.Transparent
            ),
            center = center,
            radius = r
        ),
        radius = r,
        center = center
    )
}

/**
 * سطح لوکس پایه — کارت سرمه‌ای با:
 *  گرادیان نرم + بافت طلایی اختیاری + حاشیه‌ی مویی طلایی + سایه‌ی عمیق (Elevation)
 * (نام قدیمی GlassSurface برای سازگاری حفظ شده است.)
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LuxCardShape,
    tonalElevation: Dp = 0.dp,
    pattern: Boolean = false,
    gradient: List<Color> = NavyCardGradient,
    borderAlpha: Float = 0.28f,
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(shape)
            .background(Brush.verticalGradient(gradient))
            .then(if (pattern) Modifier.goldLinePattern() else Modifier)
            .topGloss(0.06f)
            .goldRim(alpha = 0.28f)
            .border(goldBorder(borderAlpha), shape),
        content = content
    )
}

/** رنگ سطح شیشه‌ای — سازگاری با کد قدیمی */
@Composable
fun glassCardColor(alpha: Float): Color = NavyCard.copy(alpha = alpha)

/** حاشیه‌ی شیشه‌ای — سازگاری با کد قدیمی */
@Composable
fun glassBorder(dark: Boolean = true): BorderStroke = goldBorder()

/** حلقه‌ی نازک طلایی دور تصویر پروفایل و آواتارها */
fun Modifier.goldRing(shape: Shape, width: Dp = 2.dp): Modifier = this.border(
    BorderStroke(
        width,
        Brush.sweepGradient(listOf(GoldLight, GoldDeep, GoldBright, GoldDim, GoldLight))
    ),
    shape
)

/** قاب دایره‌ای طلایی برای رسم دستی (Canvas) */
fun DrawScope.drawGoldRing(strokeWidth: Float = 2f, alpha: Float = 0.6f) {
    drawCircle(
        brush = Brush.sweepGradient(listOf(GoldLight, GoldDeep, GoldBright, GoldLight)),
        radius = size.minDimension / 2f - strokeWidth,
        style = Stroke(width = strokeWidth),
        alpha = alpha
    )
}
