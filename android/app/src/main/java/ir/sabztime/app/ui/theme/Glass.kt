package ir.sabztime.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * رنگ شیشه‌ی کارت بر اساس تم (روشن/تاریک).
 * شفافیت باعث می‌شود رنگ پس‌زمینه از پشت کارت «شیشه‌ای» دیده شود.
 */
@Composable
fun glassCardColor(alpha: Float): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.White.copy(alpha = alpha * 0.07f) else Color.White.copy(alpha = alpha)
}

/** خط دور کارت شیشه‌ای — نازک و محو */
@Composable
fun glassBorder(dark: Boolean = isSystemInDarkTheme()): BorderStroke = BorderStroke(
    width = 1.dp,
    color = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.8f)
)

/**
 * پس‌زمینه‌ی شیشه‌ای کل صفحه:
 * رنگ پایه + چند «لکه» گرادیانی محو که باعث می‌شوند کارت‌های شفاف واقعاً شیشه‌ای به نظر برسند.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) Color(0xFF0C1310) else Color(0xFFF2F8F4)
    val blob1 = if (dark) Color(0xFF1FAF6C).copy(alpha = 0.16f) else Color(0xFF35C985).copy(alpha = 0.20f)
    val blob2 = if (dark) Color(0xFF0A5135).copy(alpha = 0.30f) else Color(0xFF0B7A45).copy(alpha = 0.08f)
    val blob3 = if (dark) Color(0xFFB26A00).copy(alpha = 0.08f) else Color(0xFFF2B74D).copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                // لکه‌های نرم گرادیانی (شبیه نور از پشت شیشه)
                drawRadialBlob(Offset(size.width * 0.9f, size.height * 0.05f), size.width * 0.95f, blob1)
                drawRadialBlob(Offset(size.width * 0.05f, size.height * 0.55f), size.width * 0.85f, blob2)
                drawRadialBlob(Offset(size.width * 0.85f, size.height * 0.95f), size.width * 0.9f, blob3)
            }
    ) {
        content()
    }
}

private fun DrawScope.drawRadialBlob(center: Offset, radius: Float, color: Color) {
    val shader = Brush.radialGradient(
        colors = listOf(color, color.copy(alpha = 0f)),
        center = center,
        radius = radius
    ).createShader(size.width, size.height)
    drawCircle(shader = shader, center = center, radius = radius, style = Fill)
}

/**
 * سطح شیشه‌ای (پایه‌ی کارت‌های GlassCard). ترکیب:
 *  - سطح شفاف
 *  - گرادیان «جیغ» (gloss) محو از بالا
 *  - خط دور نازک
 *  - سایه‌ی نرم
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tonalElevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    Surface(
        modifier = modifier,
        shape = shape,
        color = glassCardColor(if (dark) 1f else 0.62f),
        border = glassBorder(dark),
        tonalElevation = tonalElevation,
        shadowElevation = if (dark) 0.dp else 6.dp,
        content = {
            // نکته: از matchParentSize استفاده می‌شود (نه fillMaxSize) تا کارت
            // با محتوایش رشد کند و در لیست‌های بی‌حد (LazyColumn) مشکل‌ساز نشود.
            Box {
                // لایه‌ی gloss: گرادیان سفید محو از بالا به پایین
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(
                            Brush.verticalGradient(
                                colors = if (dark) {
                                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))
                                } else {
                                    listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.30f))
                                }
                            )
                        )
                )
                content()
            }
        }
    )
}
