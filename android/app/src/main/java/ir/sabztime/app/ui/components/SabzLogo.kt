package ir.sabztime.app.ui.components

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * لوگوی اختصاصی نوبت‌سبز — برگ ساده و مدرن (هم‌راستا با آیکون لانچر).
 * جایگزین آیکون ژنریک «گل اسپا» در صفحات خوش‌آمد و ورود.
 *
 * نکته‌ی فنی: از توابع داخلی materialIcon/materialPath استفاده نمی‌کنیم
 * (پارامتر fillType در آن‌ها وجود ندارد)؛ سازنده‌ی عمومی ImageVector.Builder
 * و path(pathFillType=...) API پایدار و عمومی compose-ui است.
 */
val SabzLeafIcon: ImageVector = ImageVector.Builder(
    name = "SabzLeaf",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(pathFillType = PathFillType.EvenOdd) {
        // بدنه‌ی برگ (عدسی عمودی با نوک بالا و پایین)
        moveTo(12f, 2.2f)
        curveTo(5.6f, 6.8f, 4.2f, 13.6f, 12f, 21.8f)
        curveTo(19.8f, 13.6f, 18.4f, 6.8f, 12f, 2.2f)
        close()
        // رگبرگ اصلی (با even-odd به‌صورت حفره رندر می‌شود)
        moveTo(11.2f, 6.8f)
        lineTo(12.8f, 6.8f)
        lineTo(12.8f, 17.2f)
        lineTo(11.2f, 17.2f)
        close()
        // رگبرگ‌های جانبی
        moveTo(11.2f, 10.6f)
        lineTo(8.6f, 9.0f)
        lineTo(9.4f, 7.9f)
        lineTo(12.0f, 9.6f)
        close()
        moveTo(12.8f, 9.6f)
        lineTo(15.4f, 7.9f)
        lineTo(16.2f, 9.0f)
        lineTo(13.6f, 10.6f)
        close()
        moveTo(11.2f, 13.6f)
        lineTo(9.0f, 12.1f)
        lineTo(9.8f, 11.0f)
        lineTo(12.0f, 12.5f)
        close()
        moveTo(12.8f, 12.5f)
        lineTo(15.0f, 11.0f)
        lineTo(15.8f, 12.1f)
        lineTo(13.6f, 13.6f)
        close()
    }
}.build()
