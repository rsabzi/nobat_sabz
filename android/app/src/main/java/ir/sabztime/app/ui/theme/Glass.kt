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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

// ═══════════════════════════════════════════════════════════════════════════
//  لایه‌های پایه‌ی بصری طلایی: پس‌زمینه‌ی رادیال، قاب‌های طلایی، سطوح شیشه‌ای
// ═══════════════════════════════════════════════════════════════════════════

/** حاشیه‌ی طلایی حلقه‌ای — دور قرص‌ها، چیپ‌ها و کنترل‌های کوچک */
fun goldBorder(alpha: Float = 0.55f, width: Dp = 1.dp): BorderStroke =
    BorderStroke(width, RingGold.copy(alpha = alpha.coerceIn(0f, 1f)))

/**
 * حاشیه‌ی گرادیانی لوکس کارت‌ها — امضای طلایی هر کارت:
 * «#E2EDFB → #0B4F9E (٪۴۰) → #EFF5FC (٪۶۰) → #08418A»
 */
fun goldGradientBorder(width: Dp = 1.4.dp): BorderStroke =
    BorderStroke(
        width,
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to GoldPale,
                0.40f to Gold,
                0.60f to GoldTint,
                1.00f to GoldInk
            )
        )
    )

/** قاب تزئینی کارت با شفافیت دلخواه (سازگار با امضای قدیمی) */
private fun cardBorderStroke(alpha: Float, width: Dp): BorderStroke =
    BorderStroke(
        width,
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to GoldPale.copy(alpha = GoldPale.alpha * alpha),
                0.40f to Gold.copy(alpha = Gold.alpha * alpha),
                0.60f to GoldTint.copy(alpha = GoldTint.alpha * alpha),
                1.00f to GoldInk.copy(alpha = GoldInk.alpha * alpha)
            )
        )
    )

/** لبه‌ی برجسته‌ی فلز قرص‌ها: بالا روشن (هایلایت) → پایین تیره (سایه) */
fun metalPillBorder(width: Dp = 1.dp): BorderStroke =
    BorderStroke(
        width,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                GoldTint.copy(alpha = 0.35f),
                GoldInk.copy(alpha = 0.85f)
            )
        )
    )

/**
 * پس‌زمینه‌ی سراسری همه‌ی صفحه‌ها:
 * رادیال طلای روشن با مرکز در ~٪۳۰ افقی و ~٪۱۵ عمودی (بالای صفحه):
 * «#F3F7FC → #CFE2F7 → #7FA8D9 → #4A74AD»
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GoldBgTop)
            .drawBehind {
                val cx = size.width * 0.30f
                val cy = size.height * 0.15f
                val r = hypot(size.width * 0.70f, size.height * 0.85f) * 1.18f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldBgTop, GoldBgMid, GoldBgDeep, GoldBgEdge),
                        center = Offset(cx, cy),
                        radius = r
                    )
                )
                // برق کرمی ملایم در مرکز — حس نورپردازی استودیویی
                radialGlow(
                    center = Offset(size.width * 0.30f, size.height * 0.16f),
                    radius = size.width * 0.65f,
                    color = GoldTint.copy(alpha = 0.35f)
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
 * بافت خطوط مورب طلاییِ بسیار محو (Pattern) روی کارت‌ها —
 * حس بافت پارچه/فلز برس‌خورده با شفافیت کم.
 */
fun Modifier.goldLinePattern(
    alpha: Float = 0.06f,
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
                    GoldPale.copy(alpha = alpha * 0.25f),
                    Gold.copy(alpha = alpha),
                    GoldPale.copy(alpha = alpha * 0.25f)
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

/** نور محو گوشه‌ی بالای سطح (Gloss) — حس لاک براق / شیشه */
fun Modifier.topGloss(alpha: Float = 0.22f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.55f
        )
    )
}

/** خط نور طلایی روی لبه‌ی بالایی کارت (Rim light) */
fun Modifier.goldRim(alpha: Float = 0.55f): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                GoldPale.copy(alpha = alpha),
                Color.Transparent
            )
        ),
        size = Size(size.width, 1.2f)
    )
}

/** هاله‌ی طلایی نرم (Glow) دور عنصر فعال */
fun Modifier.neonGlow(
    color: Color = GoldPale,
    radiusFactor: Float = 1.15f,
    alpha: Float = 0.45f
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
 * سطح پایه‌ی لوکس — کارت شیشه‌ای روشن با:
 *  گرادیان شفاف «سفید → کهربایی» + بافت طلایی اختیاری + قاب گرادیانی طلایی
 *  + سایه‌ی نرم گرم.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LuxCardShape,
    tonalElevation: Dp = 0.dp,
    pattern: Boolean = false,
    gradient: List<Color> = NavyCardGradient,
    borderAlpha: Float = 0.9f,
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = GoldBgEdge,
                spotColor = GoldBgEdge
            )
            .clip(shape)
            .background(Brush.verticalGradient(gradient))
            .then(if (pattern) Modifier.goldLinePattern() else Modifier)
            .topGloss(0.14f)
            .goldRim(alpha = 0.7f)
            .border(cardBorderStroke(borderAlpha, 1.4.dp), shape),
        content = content
    )
}

/** رنگ سطح شیشه‌ای — سازگاری با کد قدیمی */
@Composable
fun glassCardColor(alpha: Float): Color = NavyCard.copy(alpha = alpha)

/** حاشیه‌ی شیشه‌ای — سازگاری با کد قدیمی */
@Composable
fun glassBorder(dark: Boolean = true): BorderStroke = goldBorder()

/** حلقه‌ی طلایی دور آواتارها و دکمه‌های دایره‌ای */
fun Modifier.goldRing(shape: Shape, width: Dp = 2.dp): Modifier = this.border(
    BorderStroke(
        width,
        Brush.sweepGradient(
            listOf(
                GoldPale,
                GoldInk,
                Gold,
                GoldTint,
                GoldInk,
                GoldPale
            )
        )
    ),
    shape
)

/** قاب دایره‌ای طلایی برای رسم دستی (Canvas) */
fun DrawScope.drawGoldRing(strokeWidth: Float = 2f, alpha: Float = 0.8f) {
    drawCircle(
        brush = Brush.sweepGradient(listOf(GoldPale, GoldInk, Gold, GoldTint, GoldPale)),
        radius = size.minDimension / 2f - strokeWidth,
        style = Stroke(width = strokeWidth),
        alpha = alpha
    )
}
