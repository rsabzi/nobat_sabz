package ir.sabztime.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.sabztime.app.ui.theme.BurgundyGradient
import ir.sabztime.app.ui.theme.Cream
import ir.sabztime.app.ui.theme.CreamDim
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBright
import ir.sabztime.app.ui.theme.GoldDeep
import ir.sabztime.app.ui.theme.GoldDim
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.LuxFieldShape
import ir.sabztime.app.ui.theme.MidnightDeep
import ir.sabztime.app.ui.theme.NavyCardGradient
import ir.sabztime.app.ui.theme.NavyInk
import ir.sabztime.app.ui.theme.PureWhite
import ir.sabztime.app.ui.theme.TextMuted
import ir.sabztime.app.ui.theme.goldBorder
import ir.sabztime.app.ui.theme.goldRing

// ═══════════════════════════════════════════════════════════════════════════
//  اجزای امضای طراحی «پرایم»: هدر، سرچ‌بار، فیلترها، کارت کسب‌وکار، منوی پروفایل
// ═══════════════════════════════════════════════════════════════════════════

/** متن با رنگ‌آمیزی طلایی برس‌خورده (گرادیانی) */
@Composable
fun GoldText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1
) {
    Box(modifier.brushedGoldTint(BrushedGold)) {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = PureWhite,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * سرصفحه‌ی خوش‌آمدگویی — «درود، {نام} 👋» + تاریخ جلالی + اکشن‌های سمت چپ.
 */
@Composable
fun WelcomeHeader(
    name: String,
    dateLine: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "درود، $name",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Cream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text("👋", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                dateLine,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = PureWhite.copy(alpha = 0.62f)
            )
        }
        actions()
    }
}

/** دکمه‌ی گرد شیشه‌ای برای اکشن‌های هدر (اعلان، تازه‌سازی، …) */
@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Gold,
    size: Dp = 42.dp,
    badge: Boolean = false
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))))
                .border(goldBorder(0.26f), CircleShape)
                .clickable(onClick = onClick)
        )
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(GoldLight)
            )
        }
    }
}

/**
 * سرچ‌بار لوکس — کارت سرمه‌ای تیره با حاشیه‌ی طلایی برس‌خورده،
 * آیکون ذره‌بین در سمت راست (RTL) و متن راهنمای کرم.
 */
@Composable
fun LuxSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "جستجوی کسب‌وکار یا خدمات...",
    onSearch: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(LuxFieldShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF16203C), NavyInk)))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.65f), Gold.copy(alpha = 0.25f), GoldBright.copy(alpha = 0.5f)))), LuxFieldShape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Cream,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(color = Cream)
                ),
                cursorBrush = SolidColor(Gold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x1FFFFFFF))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "پاک کردن", tint = CreamDim, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/**
 * دکمه‌ی فیلتر (استان / دسته‌بندی) — سرمه‌ای شفاف با آیکون طلایی؛
 * وقتی مقداری انتخاب شده باشد، سطح طلایی می‌شود.
 */
@Composable
fun FilterButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (active) Brush.verticalGradient(listOf(GoldLight, GoldDim))
                else Brush.verticalGradient(listOf(Color(0xFF16203C), NavyInk))
            )
            .border(goldBorder(if (active) 0.0f else 0.30f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) MidnightDeep else Gold,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MidnightDeep else Cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = if (active) MidnightDeep.copy(alpha = 0.7f) else CreamDim.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/** ردیف استاندارد دو دکمه‌ی فیلتر: راست = استان، چپ = دسته‌بندی */
@Composable
fun FilterRow(
    province: String,
    category: String,
    onProvince: () -> Unit,
    onCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterButton(
            label = province.ifBlank { "استان" },
            icon = Icons.Filled.Place,
            active = province.isNotBlank(),
            onClick = onProvince,
            modifier = Modifier.weight(1f)
        )
        FilterButton(
            label = category.ifBlank { "دسته‌بندی" },
            icon = Icons.Filled.Category,
            active = category.isNotBlank(),
            onClick = onCategory,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * کارت کسب‌وکار (VIP) — پس‌زمینه‌ی سرمه‌ای با بافت خطوط طلایی،
 * کاشی آیکون سه‌بعدی در سمت چپ، عنوان طلایی و توضیحات سفید.
 */
@Composable
fun VipBusinessCard(
    title: String,
    location: String,
    services: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        pattern = true,
        contentPadding = 18.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldText(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        GoldBadge(badge)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    location,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (services.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        services,
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            IconTile(icon = icon, tileSize = 58.dp, contentDescription = null)
        }
    }
}

/** برچسب کوچک طلایی (VIP، ممتاز، …) */
@Composable
fun GoldBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(GoldLight, GoldDim)))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MidnightDeep
        )
    }
}

/** آواتار دایره‌ای با حلقه‌ی طلایی */
@Composable
fun GoldAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val initial = name.trim().take(1).ifBlank { "؟" }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B2748), NavyInk)))
            .goldRing(CircleShape, 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
    }
}

/**
 * ردیف منوی پروفایل — آیکون طلایی سمت راست، متن کرم، فلش کوچک سفید سمت چپ.
 */
@Composable
fun LuxMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: TilePalette = TilePalette.GOLD,
    trailing: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tileSize = 40.dp, palette = palette, contentDescription = null)
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Cream,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = GoldDim,
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = PureWhite.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** جداکننده‌ی مویی طلایی */
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Gold.copy(alpha = 0.28f), Color.Transparent)
                )
            )
    )
}

/** عنوان بخش — خط طلایی کوتاه + متن کرم */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(GoldLight, GoldDeep)))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Cream,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/**
 * کارت زرشکی داشبورد پروفایل — ردیف هویت (ستاره، نام، امتیاز، آواتار)
 * به‌همراه محتوای دلخواه (لیست منو).
 */
@Composable
fun ProfileHeroCard(
    name: String,
    rating: String,
    level: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        gradient = BurgundyGradient,
        contentPadding = 18.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Filled.Star,
                tileSize = 44.dp,
                palette = TilePalette.GOLD,
                contentDescription = null
            )
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Cream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        level,
                        style = MaterialTheme.typography.labelMedium,
                        color = CreamDim.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.width(6.dp))
                    GoldText(
                        text = rating,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            GoldAvatar(name = name, size = 58.dp)
        }
        Spacer(Modifier.height(14.dp))
        GoldDivider()
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** نوار اطلاع‌رسانی نرم (آفلاین، هشدار) */
@Composable
fun SoftBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x33D8B569), Color(0x14D8B569))))
            .border(goldBorder(0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Cream
        )
    }
}

/** فضای خالی استاندارد پایین صفحه تا زیر نوار پیمایش شناور */
@Composable
fun BottomBarSpacer() = Spacer(Modifier.height(112.dp))
