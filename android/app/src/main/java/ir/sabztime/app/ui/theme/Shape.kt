package ir.sabztime.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * گوشه‌های گرد و نرم — امضای بصری «پرایم» (~۲۵dp روی کارت‌های اصلی).
 */
val SabzShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(25.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** فرم استاندارد کارت‌های لوکس */
val LuxCardShape = RoundedCornerShape(25.dp)

/** فرم سرچ‌بار و فیلدهای ورودی */
val LuxFieldShape = RoundedCornerShape(20.dp)

/** فرم دکمه‌های قرصی */
val PillShape = RoundedCornerShape(50)

/** نوار پیمایش شناور */
val NavBarShape = RoundedCornerShape(28.dp)

/** سازگاری با کد قدیمی */
val GlassShape = LuxCardShape
