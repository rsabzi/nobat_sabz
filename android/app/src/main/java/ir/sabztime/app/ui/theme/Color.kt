package ir.sabztime.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  «نوبت‌سبز» — پالت لوکس (Premium Luxury Palette)
//  سرمه‌ای سوخته → زغالی، با لهجه‌ی طلایی برس‌خورده و زرشکی عمیق.
//  این پالت تنها منبع حقیقت رنگ در کل اپلیکیشن است.
// ═══════════════════════════════════════════════════════════════════════════

// ── پس‌زمینه: سرمه‌ای سوخته تا زغالی ──────────────────────────────────────
/** سرمه‌ای سوخته — بالای صفحه */
val MidnightDeep = Color(0xFF070C1C)
val Midnight = Color(0xFF0B1329)
val MidnightSoft = Color(0xFF111B36)
/** زغالی — پایین صفحه */
val Charcoal = Color(0xFF12141A)
val CharcoalDeep = Color(0xFF0C0D11)

/** سطح کارت‌های سرمه‌ای لوکس (کارت کسب‌وکار / سرچ‌بار) */
val NavyCard = Color(0xFF141F3D)
val NavyCardDeep = Color(0xFF0E1730)
/** مربع تیره‌ی پشت آیکون‌های طلایی */
val NavyInk = Color(0xFF091124)

// ── طلایی برس‌خورده (Brushed Gold) ────────────────────────────────────────
val GoldLight = Color(0xFFF6E3B4)
val GoldBright = Color(0xFFE9CE8C)
/** طلایی اصلی برند */
val Gold = Color(0xFFD8B569)
val GoldDim = Color(0xFFB8944A)
val GoldDeep = Color(0xFF8A6C2E)
val GoldShadow = Color(0xFF4A3915)

// ── زرشکی عمیق (Deep Burgundy) ────────────────────────────────────────────
val Burgundy = Color(0xFF5A1026)
val BurgundyDeep = Color(0xFF3C0A1A)
val BurgundyLight = Color(0xFF7A1834)

// ── متن ────────────────────────────────────────────────────────────────────
val Cream = Color(0xFFF4EADA)
val CreamDim = Color(0xFFD9CFC0)
val PureWhite = Color(0xFFFFFFFF)
val WhiteSoft = Color(0xFFE8EBF2)
val TextMuted = Color(0xFFA8B0C4)

/** ظرف طلایی برای برچسب‌ها */
val GoldContainer = Color(0xFF3A2C10)
val RoseContainerText = Color(0xFFFFD9DF)
val OutlineFaint = Color(0x33D8B569)
val ScrimDeep = Color(0xCC05070F)

// ── وضعیت ─────────────────────────────────────────────────────────────────
val SuccessEmerald = Color(0xFF3FBF8F)
val SuccessEmeraldDeep = Color(0xFF1C7A58)
val DangerRose = Color(0xFFE2607A)
val DangerRoseDeep = Color(0xFF8E1B33)
val InfoSapphire = Color(0xFF5B8FE3)
val InfoSapphireDeep = Color(0xFF23407A)

// ── گرادیان‌های آماده ─────────────────────────────────────────────────────
/** گرادیان پس‌زمینه‌ی اصلی صفحه: سرمه‌ای سوخته → زغالی */
val ScreenGradient: List<Color> = listOf(MidnightDeep, Midnight, Charcoal, CharcoalDeep)

/** گرادیان کارت سرمه‌ای لوکس */
val NavyCardGradient: List<Color> = listOf(Color(0xFF19264A), NavyCardDeep)

/** گرادیان کارت زرشکی */
val BurgundyGradient: List<Color> = listOf(BurgundyLight, BurgundyDeep)

/** گرادیان طلایی برس‌خورده (دکمه‌ها، آیکون‌ها، حاشیه‌ها) */
val GoldGradient: List<Color> = listOf(GoldLight, Gold, GoldDeep)

/** گرادیان طلایی نرم برای حاشیه‌های مویی */
val GoldHairline: List<Color> = listOf(
    GoldBright.copy(alpha = 0.75f),
    Gold.copy(alpha = 0.30f),
    GoldBright.copy(alpha = 0.60f)
)

/** گرادیان خاموش (عناصر غیرفعال) */
val MutedGradient: List<Color> = listOf(Color(0xFF2C3450), Color(0xFF1A2036))

// ── سازگاری با کدهای قدیمی (Deprecated aliases) ───────────────────────────
// این نام‌ها هنوز در چند صفحه استفاده می‌شوند؛ به پالت جدید نگاشت شده‌اند.
val GlassGradientDark: List<Color> = GoldGradient
val GlassGradientLight: List<Color> = GoldGradient
val GlassGradientMutedDark: List<Color> = MutedGradient
val GlassGradientMutedLight: List<Color> = MutedGradient
val GlassGradientErrorDark: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientErrorLight: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientAmberDark: List<Color> = GoldGradient
val GlassGradientAmberLight: List<Color> = GoldGradient
