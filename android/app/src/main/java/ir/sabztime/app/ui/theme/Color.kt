package ir.sabztime.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  «نوبت‌سبز» — پالت برند آبی + سبز سیگنال (نسخه ۳)
//  هویت بصری روشن: پس‌زمینه‌ی خنثی + هاله‌ی آبی، کارت‌های سفید/سطح‌نرم،
//  دکمه‌ها و قرص‌ها/چیپ‌ها با گرادیان آبی برند؛ سبز سیگنال فقط برای
//  «آزاد/تأیید/موفقیت»؛ کهربایی برای هشدار؛ قرمز برای خطا/لغو.
//  این پالت تنها منبع حقیقت رنگ در کل اپلیکیشن است.
//
//  توکن‌هایی که با پوسته (روشن/تاریک) تغییر می‌کنند (متن/سطح/زمینه) به‌صورت
//  متغیرهای «مشاهده‌شدنی» تعریف شده‌اند تا هر جایی که در UI خوانده می‌شوند،
//  با تغییر پوسته خودکار به‌روز و re-compose شوند.
// ═══════════════════════════════════════════════════════════════════════════

// ── آبی برند (ثابت — در هر دو پوسته یکسان) ───────────────────────────────
val BrandPrimary = Color(0xFF0B4F9E)
val BrandDark = Color(0xFF08418A)
val BrandLight = Color(0xFF1E6FE0)
val BrandContainer = Color(0xFFE2EDFB)
val BrandOnContainer = Color(0xFF062A54)

// ── سبز سیگنال (ثابت) ────────────────────────────────────────────────────
val SignalGreen = Color(0xFF15803D)
val SignalGreenLight = Color(0xFF16A34A)
val SignalGreenContainer = Color(0xFFE7F4EC)

// ── کهربایی (ثابت) ───────────────────────────────────────────────────────
val Amber = Color(0xFFB45309)
val AmberContainer = Color(0xFFFEF3C7)
val BrownAmberDeep = Color(0xFF92400E)

// ── قرمز (ثابت) ──────────────────────────────────────────────────────────
val RedError = Color(0xFFC02525)
val RedErrorContainer = Color(0xFFFDE8E8)

// ── خنثی (ثابت) ──────────────────────────────────────────────────────────
val NeutralBg = Color(0xFFF5F7FA)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceSoft = Color(0xFFEDF1F6)
val BorderColor = Color(0xFFD8DFE7)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF5B6472)

// ── تم تاریک (ثابتِ مرجع) ────────────────────────────────────────────────
val DarkBg = Color(0xFF0B1220)
val DarkSurface = Color(0xFF121A29)
val DarkPrimary = Color(0xFF9CC5FF)
val DarkOnSurface = Color(0xFFE6EBF2)
val DarkOnSurfaceVariant = Color(0xFF9AA7B8)

// ── فلز/گرادیان آبی برند (ثابت) ──────────────────────────────────────────
val MetalGoldTop = Color(0xFF1E6FE0)
val MetalGoldBottom = Color(0xFF0B4F9E)
val Gold = Color(0xFF0B4F9E)
val RingGold = Color(0xFF1E6FE0)
val GoldInk = Color(0xFF08418A)

// ── متنِ روشن روی پنل‌های رنگی (ثابت) ────────────────────────────────────
val CreamLight = Color(0xFFFFFFFF)
val PureWhite = Color(0xFFFFFFFF)

// ── وضعیت (ثابت) ─────────────────────────────────────────────────────────
val SuccessEmerald = Color(0xFF15803D)
val SuccessEmeraldDeep = Color(0xFF0E5C2B)
val DangerRose = Color(0xFFC02525)
val DangerRoseDeep = Color(0xFF8E1630)
val InfoSapphire = Color(0xFF1E6FE0)
val InfoSapphireDeep = Color(0xFF0B4F9E)

// ── آبی برند (کاشی‌ها) ──────────────────────────────────────────────────
val BurgundyLight = Color(0xFF1E6FE0)
val Burgundy = Color(0xFF0B4F9E)
val BurgundyDeep = Color(0xFF08418A)

// ═══════════════════════════════════════════════════════════════════════════
//  توکن‌های «وابسته به پوسته» — مشاهده‌شدنی (مقدارهای پیش‌فرض = حالتِ روشن)
// ═══════════════════════════════════════════════════════════════════════════

// ── زمینه‌ی رادیال آبی ───────────────────────────────────────────────────
/** روشن‌ترین نقطه‌ی آبی — بالای صفحه */
var GoldBgTop: Color by mutableStateOf(Color(0xFFF3F7FC))
/** آبی میانی رادیال */
var GoldBgMid: Color by mutableStateOf(Color(0xFFCFE2F7))
/** آبی عمیق رادیال */
var GoldBgDeep: Color by mutableStateOf(Color(0xFF7FA8D9))
/** لبه‌ی تیره‌ی رادیال (گوشه‌ها) */
var GoldBgEdge: Color by mutableStateOf(Color(0xFF4A74AD))

// ── سفید/سطح ────────────────────────────────────────────────────────────
/** آبی روشن حاشیه‌ی کارت */
var GoldPale: Color by mutableStateOf(Color(0xFFE2EDFB))
/** آبی روشن‌ترین (Gloss) */
var GoldTint: Color by mutableStateOf(Color(0xFFEFF5FC))
/** سطح دکمه‌های آیکون دایره‌ای — بالا */
var IconCreamTop: Color by mutableStateOf(Color(0xFFFFFFFF))
/** سطح دکمه‌های آیکون دایره‌ای — پایین */
var IconCreamBottom: Color by mutableStateOf(Color(0xFFEDF1F6))
/** سطح کارت‌های روشن */
var NavyCard: Color by mutableStateOf(Color(0xFFFFFFFF))
var NavyCardDeep: Color by mutableStateOf(Color(0xFFEDF1F6))
/** سطحِ نرم (ظرف دیالوگ و…) */
var MidnightSoft: Color by mutableStateOf(Color(0xFFEFF5FC))

// ── متن ──────────────────────────────────────────────────────────────────
/** متن اصلی */
var Cocoa: Color by mutableStateOf(Color(0xFF111827))
/** متن ثانویه */
var CocoaSoft: Color by mutableStateOf(Color(0xFF5B6472))
/** متن محو */
var TextMuted: Color by mutableStateOf(Color(0xFF5B6472))

// ── گرادیان‌های آماده (getter تا با پوسته به‌روز شوند) ───────────────────
/** گرادیان پس‌زمینه (نمای نزدیک از رادیال سراسری) */
val ScreenGradient: List<Color>
    get() = listOf(GoldBgTop, GoldBgMid, GoldBgDeep, GoldBgEdge)

/** گرادیان آبی برند (قرص/دکمه/چیپ انتخاب‌شده) — ثابت */
val MetalGoldGradient: List<Color> = listOf(MetalGoldTop, MetalGoldBottom)

/** گرادیان کارت شیشه‌ای روشن — ثابت */
val NavyCardGradient: List<Color> = listOf(
    Color(0x40FFFFFF),
    Color(0x591E6FE0)
)

/** گرادیان کارت آبی (پنل برند) — ثابت */
val BurgundyGradient: List<Color> = listOf(BurgundyLight, BurgundyDeep)

/** گرادیان آبی تزئینی — ثابت */
val GoldGradient: List<Color> = listOf(MetalGoldTop, Gold, GoldInk)

/** گرادیان آبی نرم برای حاشیه‌های مویی — پویا */
val GoldHairline: List<Color>
    get() = listOf(GoldPale, Gold.copy(alpha = 0.45f), GoldTint)

/** گرادیان خاموش — ثابت */
val MutedGradient: List<Color> = listOf(Color(0xFFAFC0D2), Color(0xFF8FA3B8))

// ═══════════════════════════════════════════════════════════════════════════
//  سازگاری با کدهای قدیمی (Deprecated aliases) — getter تا پویا بمانند
// ═══════════════════════════════════════════════════════════════════════════
val MidnightDeep: Color get() = Cocoa
val Midnight: Color get() = GoldBgTop
val Charcoal: Color get() = GoldBgMid
val CharcoalDeep: Color get() = GoldBgTop

/** سطح کارت‌های روشن (alias) */
val NavyInk: Color get() = IconCreamBottom

// ── طلای قدیمی → جدید (ثابت) ─────────────────────────────────────────────
val GoldLight = MetalGoldTop
val GoldBright = MetalGoldBottom
val GoldDim = RingGold
val GoldDeep = GoldInk
val GoldShadow: Color get() = GoldBgEdge

// ── متن قدیمی → جدید (alias پویا) ───────────────────────────────────────
val Cream: Color get() = Cocoa
val CreamDim: Color get() = CocoaSoft
val WhiteSoft: Color get() = GoldTint

/** ظرف آبی برای برچسب‌ها */
val GoldContainer: Color get() = GoldPale
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

/**
 * اعمالِ پوسته روی توکن‌های مشاهده‌شدنی. مقادیر از [LuxPaletteLight] / [LuxPaletteDark]
 * خوانده می‌شوند تا یک منبعِ حقیقت داشته باشیم. هر جایی که این توکن‌ها در ترکیب
 * (composition) خوانده شده باشند، با این تغییر خودکار دوباره ترکیب می‌شوند.
 */
fun setLuxTheme(dark: Boolean) {
    val p = if (dark) LuxPaletteDark else LuxPaletteLight
    GoldBgTop = p.bgTop
    GoldBgMid = p.bgMid
    GoldBgDeep = p.bgDeep
    GoldBgEdge = p.bgEdge
    GoldPale = p.GoldPale
    GoldTint = p.GoldTint
    IconCreamTop = p.IconCreamTop
    IconCreamBottom = p.IconCreamBottom
    NavyCard = p.NavyCard
    NavyCardDeep = p.NavyCardDeep
    MidnightSoft = p.MidnightSoft
    Cocoa = p.Cocoa
    CocoaSoft = p.CocoaSoft
    TextMuted = p.TextMuted
}
