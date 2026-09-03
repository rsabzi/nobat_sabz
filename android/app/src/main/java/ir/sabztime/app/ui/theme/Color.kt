package ir.sabztime.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  «نوبت‌سبز» — پالت لوکس طلایی (Gold Metallic Luxury Palette)
//  هویت بصری روشن: زمینه‌ی رادیال طلایی + قرص‌های طلای برجسته (Embossed Metal)
//  + دکمه‌های آیکون دایره‌ای کرم با حلقه‌ی طلایی + کارت‌های شیشه‌ای روشن.
//  این پالت تنها منبع حقیقت رنگ در کل اپلیکیشن است.
// ═══════════════════════════════════════════════════════════════════════════

// ── زمینه‌ی رادیال طلایی (پس‌زمینه‌ی سراسری) ─────────────────────────────
/** روشن‌ترین نقطه‌ی طلایی — بالای صفحه (مرکز رادیال ~۳۰٪/۱۵٪) */
val GoldBgTop = Color(0xFFF5E3A1)
/** طلای میانی رادیال */
val GoldBgMid = Color(0xFFD4AF37)
/** طلای عمیق رادیال */
val GoldBgDeep = Color(0xFFA8791F)
/** لبه‌ی تیره‌ی رادیال (گوشه‌ها) */
val GoldBgEdge = Color(0xFF6B4E12)

// ── فلز طلای برجسته (قرص‌ها، دکمه‌ها، چیپ‌های انتخاب‌شده) ────────────────
/** لبه‌ی روشن فلز (Emboss highlight) */
val MetalGoldTop = Color(0xFFE8CE8A)
/** بدنه‌ی فلز */
val MetalGoldBottom = Color(0xFFC9A227)
/** طلای خالص (نقطه‌ی میانی) */
val Gold = Color(0xFFD4AF37)
/** حلقه‌ی طلایی (Ring) */
val RingGold = Color(0xFFB8860B)
/** قاب/خط تیره‌ی طلایی (Trim) */
val GoldInk = Color(0xFF8B6914)

// ── کرم/کاغذ و حاشیه‌های روشن ───────────────────────────────────────────
/** کرم درخشان حاشیه‌ی کارت */
val GoldPale = Color(0xFFFDE9B8)
/** کرم روشن‌ترین (Gloss) */
val GoldTint = Color(0xFFFFF6D9)
/** سطح دکمه‌های آیکون دایره‌ای — بالا */
val IconCreamTop = Color(0xFFFFFFFF)
/** سطح دکمه‌های آیکون دایره‌ای — پایین */
val IconCreamBottom = Color(0xFFF3ECDD)

// ── متن (قهوه‌ای تیره روی زمینه‌ی روشن) ──────────────────────────────────
/** قهوه‌ای کاکائویی — متن اصلی */
val Cocoa = Color(0xFF4A2E10)
/** قهوه‌ای ملایم — متن ثانویه */
val CocoaSoft = Color(0xFF6B4E1E)
/** کرم روشن — متن روی پنل‌های رنگی (زرشکی/زمردی/…) */
val CreamLight = Color(0xFFFFF3D6)

// ── زرشکی برند (کاشی‌های رنگی) ───────────────────────────────────────────
val BurgundyLight = Color(0xFFB0303A)
val Burgundy = Color(0xFF9E2632)
val BurgundyDeep = Color(0xFF7A1F28)

// ── وضعیت ─────────────────────────────────────────────────────────────────
val SuccessEmerald = Color(0xFF1F7A4D)
val SuccessEmeraldDeep = Color(0xFF145A3B)
val DangerRose = Color(0xFFC4264A)
val DangerRoseDeep = Color(0xFF8E1630)
val InfoSapphire = Color(0xFF2E5DA8)
val InfoSapphireDeep = Color(0xFF1E3E77)

// ── گرادیان‌های آماده ─────────────────────────────────────────────────────
/** گرادیان پس‌زمینه (نمای نزدیک از رادیال سراسری) */
val ScreenGradient: List<Color> = listOf(GoldBgTop, GoldBgMid, GoldBgDeep, GoldBgEdge)

/** گرادیان فلز طلای برجسته (قرص/دکمه/چیپ انتخاب‌شده) */
val MetalGoldGradient: List<Color> = listOf(MetalGoldTop, MetalGoldBottom)

/** گرادیان کارت شیشه‌ای روشن: سفیدِ شفاف → کهرباییِ طلاییِ شفاف */
val NavyCardGradient: List<Color> = listOf(
    Color(0x40FFFFFF), // rgba(255,255,255,0.25)
    Color(0x59C8A03C)  // rgba(200,160,60,0.35)
)

/** گرادیان کارت زرشکی (پنل برند) */
val BurgundyGradient: List<Color> = listOf(BurgundyLight, BurgundyDeep)

/** گرادیان طلایی تزئینی (متن‌ها و حاشیه‌های درخشان) */
val GoldGradient: List<Color> = listOf(MetalGoldTop, Gold, GoldInk)

/** گرادیان طلایی نرم برای حاشیه‌های مویی */
val GoldHairline: List<Color> = listOf(
    GoldPale,
    Gold.copy(alpha = 0.45f),
    GoldTint
)

/** گرادیان خاموش (عناصر غیرفعال) — روشنِ بی‌رنگ */
val MutedGradient: List<Color> = listOf(Color(0xFFE9DFC4), Color(0xFFD9C89B))

// ═══════════════════════════════════════════════════════════════════════════
//  سازگاری با کدهای قدیمی (Deprecated aliases)
//  نام‌های تاریخیِ «سرمه‌ای تیره» اکنون به معادل طلاییِ روشن نگاشت شده‌اند تا
//  همه‌ی مراجع قدیمی بدون تغییر منطق، روی هویت طلایی درست دیده شوند.
// ═══════════════════════════════════════════════════════════════════════════
val MidnightDeep = Cocoa
val Midnight = GoldBgTop
val MidnightSoft = Color(0xFFFFF7E0)
val Charcoal = GoldBgMid
val CharcoalDeep = GoldBgTop

/** سطح کارت‌های کرم روشن (جایگزین سرمه‌ای) */
val NavyCard = Color(0xFFFFFDF6)
val NavyCardDeep = Color(0xFFF7E9BE)
/** سطح کرم دکمه‌های آیکون دایره‌ای */
val NavyInk = IconCreamBottom

// ── طلای قدیمی → جدید ─────────────────────────────────────────────────────
val GoldLight = MetalGoldTop        // #E8CE8A
val GoldBright = MetalGoldBottom    // #C9A227
val GoldDim = RingGold              // #B8860B
val GoldDeep = GoldInk              // #8B6914
val GoldShadow = GoldBgEdge         // #6B4E12

// ── متن قدیمی (کرم روشن روی سرمه‌ای → اکنون قهوه‌ای روی طلایی) ──────────
val Cream = Cocoa
val CreamDim = CocoaSoft
val PureWhite = Color(0xFFFFFFFF)
val WhiteSoft = GoldTint
val TextMuted = Color(0xFF7C5E31)

/** ظرف طلایی برای برچسب‌ها */
val GoldContainer = GoldPale
val RoseContainerText = Color(0xFFFBE2E4)
val OutlineFaint = RingGold.copy(alpha = 0.22f)
val ScrimDeep = Color(0x99000000)

val GlassGradientDark: List<Color> = GoldGradient
val GlassGradientLight: List<Color> = GoldGradient
val GlassGradientMutedDark: List<Color> = MutedGradient
val GlassGradientMutedLight: List<Color> = MutedGradient
val GlassGradientErrorDark: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientErrorLight: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientAmberDark: List<Color> = GoldGradient
val GlassGradientAmberLight: List<Color> = GoldGradient
