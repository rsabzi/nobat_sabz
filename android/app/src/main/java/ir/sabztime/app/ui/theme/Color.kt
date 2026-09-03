package ir.sabztime.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  «نوبت‌سبز» — پالت برند آبی + سبز سیگنال (نسخه ۳)
//  هویت بصری روشن: پس‌زمینه‌ی خنثی + هاله‌ی آبی، کارت‌های سفید/سطح‌نرم،
//  دکمه‌ها و قرص‌ها/چیپ‌ها با گرادیان آبی برند؛ سبز سیگنال فقط برای
//  «آزاد/تأیید/موفقیت»؛ کهربایی برای هشدار؛ قرمز برای خطا/لغو.
//  این پالت تنها منبع حقیقت رنگ در کل اپلیکیشن است.
// ═══════════════════════════════════════════════════════════════════════════

// ── آبی برند ──────────────────────────────────────────────────────────────
val BrandPrimary = Color(0xFF0B4F9E)   // آبی برند (primary)
val BrandDark = Color(0xFF08418A)      // آبی تیره
val BrandLight = Color(0xFF1E6FE0)     // آبی روشن
val BrandContainer = Color(0xFFE2EDFB) // کانتینر آبی
val BrandOnContainer = Color(0xFF062A54) // متن روی کانتینر آبی

// ── سبز سیگنال (فقط آزاد/تأیید/موفقیت) ────────────────────────────────────
val SignalGreen = Color(0xFF15803D)    // سبز سیگنال
val SignalGreenLight = Color(0xFF16A34A)
val SignalGreenContainer = Color(0xFFE7F4EC)

// ── قهوه‌ای/کهربایی (هشدار/اشتراک/امتیاز) ────────────────────────────────
val Amber = Color(0xFFB45309)
val AmberContainer = Color(0xFFFEF3C7)
val BrownAmberDeep = Color(0xFF92400E)

// ── قرمز (خطا/لغو) ─────────────────────────────────────────────────────────
val RedError = Color(0xFFC02525)
val RedErrorContainer = Color(0xFFFDE8E8)

// ── خنثی ───────────────────────────────────────────────────────────────────
val NeutralBg = Color(0xFFF5F7FA)        // پس‌زمینه
val SurfaceWhite = Color(0xFFFFFFFF)     // سطح
val SurfaceSoft = Color(0xFFEDF1F6)      // سطح نرم
val BorderColor = Color(0xFFD8DFE7)      // مرز
val TextPrimary = Color(0xFF111827)      // متن اصلی
val TextSecondary = Color(0xFF5B6472)    // متن فرعی

// ── تم تاریک ──────────────────────────────────────────────────────────────
val DarkBg = Color(0xFF0B1220)
val DarkSurface = Color(0xFF121A29)
val DarkPrimary = Color(0xFF9CC5FF)
val DarkOnSurface = Color(0xFFE6EBF2)
val DarkOnSurfaceVariant = Color(0xFF9AA7B8)

// ── زمینه‌ی رادیال آبی (پس‌زمینه‌ی سراسری) ───────────────────────────────
/** روشن‌ترین نقطه‌ی آبی — بالای صفحه (مرکز رادیال ~۳۰٪/۱۵٪) */
val GoldBgTop = Color(0xFFF3F7FC)
/** آبی میانی رادیال */
val GoldBgMid = Color(0xFFCFE2F7)
/** آبی عمیق رادیال */
val GoldBgDeep = Color(0xFF7FA8D9)
/** لبه‌ی تیره‌ی رادیال (گوشه‌ها) */
val GoldBgEdge = Color(0xFF4A74AD)

// ── فلز/گرادیان آبی برند (قرص‌ها، دکمه‌ها، چیپ‌های انتخاب‌شده) ────────────
/** لبه‌ی روشن فلز (Emboss highlight) */
val MetalGoldTop = Color(0xFF1E6FE0)
/** بدنه‌ی فلز */
val MetalGoldBottom = Color(0xFF0B4F9E)
/** آبی خالص (نقطه‌ی میانی) */
val Gold = Color(0xFF0B4F9E)
/** حلقه‌ی آبی (Ring) */
val RingGold = Color(0xFF1E6FE0)
/** قاب/خط تیره‌ی آبی (Trim) */
val GoldInk = Color(0xFF08418A)

// ── سفید/سطح و حاشیه‌های روشن ───────────────────────────────────────────
/** آبی روشن حاشیه‌ی کارت */
val GoldPale = Color(0xFFE2EDFB)
/** آبی روشن‌ترین (Gloss) */
val GoldTint = Color(0xFFEFF5FC)
/** سطح دکمه‌های آیکون دایره‌ای — بالا */
val IconCreamTop = Color(0xFFFFFFFF)
/** سطح دکمه‌های آیکون دایره‌ای — پایین */
val IconCreamBottom = Color(0xFFEDF1F6)

// ── متن (تیره روی زمینه‌ی روشن) ──────────────────────────────────────────
/** متن اصلی */
val Cocoa = Color(0xFF111827)
/** متن ثانویه */
val CocoaSoft = Color(0xFF5B6472)
/** متن روشن — روی پنل‌های رنگی (آبی/سبز/…) */
val CreamLight = Color(0xFFFFFFFF)

// ── آبی برند (کاشی‌های برند — جایگزین زرشکی) ────────────────────────────
val BurgundyLight = Color(0xFF1E6FE0)
val Burgundy = Color(0xFF0B4F9E)
val BurgundyDeep = Color(0xFF08418A)

// ── وضعیت ─────────────────────────────────────────────────────────────────
val SuccessEmerald = Color(0xFF15803D)     // سبز سیگنال — تأیید/موفقیت
val SuccessEmeraldDeep = Color(0xFF0E5C2B)
val DangerRose = Color(0xFFC02525)         // قرمز — خطا/لغو
val DangerRoseDeep = Color(0xFF8E1630)
val InfoSapphire = Color(0xFF1E6FE0)       // آبی — اطلاع/در جریان
val InfoSapphireDeep = Color(0xFF0B4F9E)

// ── گرادیان‌های آماده ─────────────────────────────────────────────────────
/** گرادیان پس‌زمینه (نمای نزدیک از رادیال سراسری) */
val ScreenGradient: List<Color> = listOf(GoldBgTop, GoldBgMid, GoldBgDeep, GoldBgEdge)

/** گرادیان آبی برند (قرص/دکمه/چیپ انتخاب‌شده) */
val MetalGoldGradient: List<Color> = listOf(MetalGoldTop, MetalGoldBottom)

/** گرادیان کارت شیشه‌ای روشن: سفیدِ شفاف → آبیِ شفاف */
val NavyCardGradient: List<Color> = listOf(
    Color(0x40FFFFFF), // rgba(255,255,255,0.25)
    Color(0x591E6FE0)  // rgba(30,111,224,0.35)
)

/** گرادیان کارت آبی (پنل برند) */
val BurgundyGradient: List<Color> = listOf(BurgundyLight, BurgundyDeep)

/** گرادیان آبی تزئینی (متن‌ها و حاشیه‌های درخشان) */
val GoldGradient: List<Color> = listOf(MetalGoldTop, Gold, GoldInk)

/** گرادیان آبی نرم برای حاشیه‌های مویی */
val GoldHairline: List<Color> = listOf(
    GoldPale,
    Gold.copy(alpha = 0.45f),
    GoldTint
)

/** گرادیان خاموش (عناصر غیرفعال) — آبیِ بی‌رنگ */
val MutedGradient: List<Color> = listOf(Color(0xFFAFC0D2), Color(0xFF8FA3B8))

// ═══════════════════════════════════════════════════════════════════════════
//  سازگاری با کدهای قدیمی (Deprecated aliases)
//  نام‌های تاریخیِ «سرمه‌ای تیره/طلایی» اکنون به معادل آبیِ برند نگاشت شده‌اند تا
//  همه‌ی مراجع قدیمی بدون تغییر منطق، روی هویت آبی درست دیده شوند.
// ═══════════════════════════════════════════════════════════════════════════
val MidnightDeep = Cocoa
val Midnight = GoldBgTop
val MidnightSoft = Color(0xFFEFF5FC)
val Charcoal = GoldBgMid
val CharcoalDeep = GoldBgTop

/** سطح کارت‌های روشن (جایگزین سرمه‌ای) */
val NavyCard = Color(0xFFFFFFFF)
val NavyCardDeep = Color(0xFFEDF1F6)
/** سطح سفید دکمه‌های آیکون دایره‌ای */
val NavyInk = IconCreamBottom

// ── طلای قدیمی → جدید (نگاشت به آبی) ───────────────────────────────────────
val GoldLight = MetalGoldTop        // #1E6FE0
val GoldBright = MetalGoldBottom    // #0B4F9E
val GoldDim = RingGold              // #1E6FE0
val GoldDeep = GoldInk              // #08418A
val GoldShadow = GoldBgEdge         // #4A74AD

// ── متن قدیمی → جدید (تیره روی روشن) ─────────────────────────────────────
val Cream = Cocoa
val CreamDim = CocoaSoft
val PureWhite = Color(0xFFFFFFFF)
val WhiteSoft = GoldTint
val TextMuted = Color(0xFF5B6472)

/** ظرف آبی برای برچسب‌ها */
val GoldContainer = GoldPale
val RoseContainerText = Color(0xFFFDE8E8)
val OutlineFaint = RingGold.copy(alpha = 0.22f)
val ScrimDeep = Color(0x99000000)

val GlassGradientDark: List<Color> = listOf(BrandLight, BrandPrimary)
val GlassGradientLight: List<Color> = listOf(BrandLight, BrandPrimary)
val GlassGradientMutedDark: List<Color> = MutedGradient
val GlassGradientMutedLight: List<Color> = MutedGradient
val GlassGradientErrorDark: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientErrorLight: List<Color> = listOf(DangerRose, DangerRoseDeep)
val GlassGradientAmberDark: List<Color> = listOf(Amber, BrownAmberDeep)
val GlassGradientAmberLight: List<Color> = listOf(Amber, BrownAmberDeep)
val GlassGradientGreenDark: List<Color> = listOf(SignalGreenLight, SignalGreen)
val GlassGradientGreenLight: List<Color> = listOf(SignalGreenLight, SignalGreen)
