package ir.sabztime.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * پالت دوتاییِ لوکس «نوبت‌سبز».
 *
 * دلیلِ وجود: تمِ لوکس عمدتاً توکن‌های رنگی را «هاردکد» استفاده می‌کند (نه از طریق
 * MaterialTheme)، پس برای اینکه حالتِ تاریک واقعاً در همه‌جای UI اثر کند، همین
 * توکن‌ها را در یک پالت دوتایی می‌گذاریم و در محل استفاده از طریق CompositionLocal
 * می‌خوانیم.
 *
 * - [Light] == دقیقاً مقدارهای فعلی توکن‌ها (تغییرناپذیر؛ حالت روشن دست نمی‌خورد).
 * - [Dark]  == معادلِ تیره‌ی لوکس (همان روحیه، اما زمینه‌ی سرمه‌ای/تیره و متنِ روشن).
 *
 * نام فیلدها عمداً با نام توکن‌های قدیمی یکسان است تا تبدیل محل‌های استفاده،
 * مکانیکی و کم‌ریسک بماند.
 */
data class LuxPalette(
    val isDark: Boolean,
    // ── متن ──
    val Cocoa: Color,
    val CocoaSoft: Color,
    val Cream: Color,
    val CreamDim: Color,
    val TextMuted: Color,
    val CreamLight: Color,
    val PureWhite: Color,
    // ── سطح کارت / آیکون ──
    val IconCreamTop: Color,
    val IconCreamBottom: Color,
    val GoldPale: Color,
    val GoldTint: Color,
    val NavyCard: Color,
    val NavyCardDeep: Color,
    val MidnightSoft: Color,
    val MidnightDeep: Color,
    // ── زمینه‌ی رادیال سراسری ──
    val bgTop: Color,
    val bgMid: Color,
    val bgDeep: Color,
    val bgEdge: Color
)

/** حالتِ روشن — دقیقاً همان مقدارهای فعلی توکن‌ها. */
val LuxPaletteLight = LuxPalette(
    isDark = false,
    Cocoa = Cocoa,
    CocoaSoft = CocoaSoft,
    Cream = Cream,
    CreamDim = CreamDim,
    TextMuted = TextMuted,
    CreamLight = CreamLight,
    PureWhite = PureWhite,
    IconCreamTop = IconCreamTop,
    IconCreamBottom = IconCreamBottom,
    GoldPale = GoldPale,
    GoldTint = GoldTint,
    NavyCard = NavyCard,
    NavyCardDeep = NavyCardDeep,
    MidnightSoft = MidnightSoft,
    MidnightDeep = MidnightDeep,
    bgTop = GoldBgTop,
    bgMid = GoldBgMid,
    bgDeep = GoldBgDeep,
    bgEdge = GoldBgEdge
)

/**
 * حالتِ تیره‌ی لوکس — زمینه‌ی سرمه‌ایِ عمیق، سطح‌های تیره، متنِ روشن.
 * آبی/طلاییِ برند به‌عنوان accent تقریباً حفظ می‌شود تا همان هویت دیده شود.
 */
val LuxPaletteDark = LuxPalette(
    isDark = true,
    // متن: روشن روی زمینه‌ی تیره
    Cocoa = Color(0xFFE8EDF5),
    CocoaSoft = Color(0xFF9AA7B8),
    Cream = Color(0xFFE8EDF5),
    CreamDim = Color(0xFF9AA7B8),
    TextMuted = Color(0xFF8896A9),
    CreamLight = Color(0xFFFFFFFF),
    PureWhite = Color(0xFFFFFFFF),
    // سطح کارت / آیکون: سرمه‌ایِ تیره
    IconCreamTop = Color(0xFF2C3750),
    IconCreamBottom = Color(0xFF1B2436),
    GoldPale = Color(0xFF3A4A68),
    GoldTint = Color(0xFF1E2A3F),
    NavyCard = Color(0xFF141C2E),
    NavyCardDeep = Color(0xFF202B42),
    MidnightSoft = Color(0xFF202B42),
    MidnightDeep = Color(0xFF0E1526),
    // زمینه‌ی رادیال: سرمه‌ایِ عمیق با هاله‌ی آبیِ محو
    bgTop = Color(0xFF141F33),
    bgMid = Color(0xFF0E182A),
    bgDeep = Color(0xFF0A1220),
    bgEdge = Color(0xFF05090F)
)

/** CompositionLocal پالت لوکس؛ به‌صورت پیش‌فرض روشن (سازگاری با حالت فعلی). */
val LocalLuxPalette = staticCompositionLocalOf { LuxPaletteLight }

/** پالتِ جاری را در یک Composable برمی‌گرداند. */
@Composable
fun luxPalette(): LuxPalette = LocalLuxPalette.current
