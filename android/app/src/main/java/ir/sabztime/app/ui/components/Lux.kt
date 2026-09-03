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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import ir.sabztime.app.ui.theme.Cocoa
import ir.sabztime.app.ui.theme.CocoaSoft
import ir.sabztime.app.ui.theme.CreamLight
import ir.sabztime.app.ui.theme.DangerRose
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBgEdge
import ir.sabztime.app.ui.theme.GoldInk
import ir.sabztime.app.ui.theme.GoldPale
import ir.sabztime.app.ui.theme.IconCreamBottom
import ir.sabztime.app.ui.theme.IconCreamTop
import ir.sabztime.app.ui.theme.LuxCardShape
import ir.sabztime.app.ui.theme.LuxFieldShape
import ir.sabztime.app.ui.theme.MetalGoldGradient
import ir.sabztime.app.ui.theme.PureWhite
import ir.sabztime.app.ui.theme.RingGold
import ir.sabztime.app.ui.theme.goldBorder
import ir.sabztime.app.ui.theme.goldGradientBorder
import ir.sabztime.app.ui.theme.goldRing
import ir.sabztime.app.ui.theme.topGloss

// ═══════════════════════════════════════════════════════════════════════════
//  اجزای امضای طراحی «طلایی لوکس»: هدر، سرچ‌بار، فیلترها، کارت کسب‌وکار، منو
// ═══════════════════════════════════════════════════════════════════════════

/** متن با رنگ‌آمیزی طلایی برس‌خورده (گرادیانی درخشان) */
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
 * سرصفحه‌ی خوش‌آمدگویی — «روز بخیر» + تاریخ جلالی + اکشن‌های سمت چپ
 * (دو دکمه‌ی دایره‌ای کرم با حلقه‌ی طلایی).
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
            Text(
                "روز بخیر 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Cocoa,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val sub = if (name.isNotBlank() && name != "مهمان") "$name • $dateLine" else dateLine
            Text(
                sub,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = CocoaSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        actions()
    }
}

/** دکمه‌ی گرد کرم با حلقه‌ی طلایی برای اکشن‌های هدر (اعلان، تازه‌سازی، …) */
@Composable
fun GhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Cocoa,
    size: Dp = 42.dp,
    badge: Boolean = false
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .creamCircle(CircleShape)
                .clickable(onClick = onClick)
        )
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.46f)
        )
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFE05B5B), DangerRose)))
                    .border(BorderStroke(1.5.dp, PureWhite), CircleShape)
            )
        }
    }
}

/**
 * سرچ‌بار لوکس — کارت کرمِ شیشه‌ای روشن با قاب گرادیانی طلایی،
 * آیکون ذره‌بین در سمت راست (RTL) و متن قهوه‌ایِ تیره.
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
            .shadow(
                elevation = 8.dp,
                shape = LuxFieldShape,
                ambientColor = GoldBgEdge.copy(alpha = 0.4f),
                spotColor = GoldBgEdge.copy(alpha = 0.45f)
            )
            .clip(LuxFieldShape)
            .background(Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0xC9EDF1F6))))
            .topGloss(0.5f)
            .border(goldGradientBorder(1.4.dp), LuxFieldShape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = CocoaSoft,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CocoaSoft.copy(alpha = 0.62f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(color = Cocoa)
                ),
                cursorBrush = SolidColor(GoldInk),
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
                    .background(Brush.verticalGradient(listOf(Color(0xCCFFFFFF), Color(0xAAEDF1F6))))
                    .border(goldBorder(0.4f), CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "پاک کردن", tint = CocoaSoft, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/**
 * دکمه‌ی فیلتر (استان / دسته‌بندی) — کرم شفاف با آیکون قهوه‌ای؛
 * وقتی مقداری انتخاب شده باشد، قرص فلز طلاییِ برجسته می‌شود.
 */
@Composable
fun FilterButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (active) Brush.verticalGradient(MetalGoldGradient)
                else Brush.verticalGradient(listOf(Color(0xB3FFFFFF), Color(0x8CEDF1F6)))
            )
            .topGloss(if (active) 0.3f else 0.4f)
            .border(
                if (active) ir.sabztime.app.ui.theme.metalPillBorder() else goldBorder(0.5f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) Cocoa else CocoaSoft,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Cocoa else CocoaSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = if (active) Cocoa.copy(alpha = 0.7f) else CocoaSoft.copy(alpha = 0.7f),
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
 * کارت کسب‌وکار (VIP) — شیشه‌ی روشن با بافت طلایی،
 * عنوان قهوه‌ایِ تیره و کاشی برندِ طلایی.
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cocoa,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    color = CocoaSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (services.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        services,
                        style = MaterialTheme.typography.bodySmall,
                        color = CocoaSoft.copy(alpha = 0.8f),
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

/** برچسب کوچک طلایی (VIP، ممتاز، …) — قرص فلز برجسته */
@Composable
fun GoldBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(MetalGoldGradient))
            .border(ir.sabztime.app.ui.theme.metalPillBorder(0.8.dp), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Cocoa
        )
    }
}

/** آواتار دایره‌ای کرم با حلقه‌ی طلایی */
@Composable
fun GoldAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val initial = name.trim().take(1).ifBlank { "؟" }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(IconCreamTop, IconCreamBottom)))
            .topGloss(0.5f)
            .goldRing(CircleShape, 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Cocoa
        )
    }
}

/**
 * ردیف منوی پروفایل — کاشی رنگی سمت راست، متن قهوه‌ای (از LocalContentColor)،
 * فلش کوچک سمت چپ. داخل کارت زرشکی، رنگ متن به‌صورت خودکار کرم می‌شود.
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
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = LocalContentColor.current.copy(alpha = 0.6f),
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
                    listOf(Color.Transparent, RingGold.copy(alpha = 0.45f), Color.Transparent)
                )
            )
    )
}

/** عنوان بخش — میله‌ی طلایی کوتاه + متن قهوه‌ای */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 19.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(MetalGoldGradient))
                .border(ir.sabztime.app.ui.theme.metalPillBorder(0.7.dp), RoundedCornerShape(50))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Cocoa,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/**
 * کارت زرشکی داشبورد پروفایل — پنل رنگی برند با قاب طلایی؛
 * ردیف هویت (آواتار، نام، امتیاز) به‌همراه محتوای دلخواه (لیست منو).
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
        CompositionLocalProvider(LocalContentColor provides CreamLight) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // در RTL: تصویر پروفایل سمت راست، ستاره‌ی طلایی سمت چپ
                GoldAvatar(name = name, size = 58.dp)
                Spacer(Modifier.width(14.dp))
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CreamLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            level,
                            style = MaterialTheme.typography.labelMedium,
                            color = CreamLight.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldPale
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                IconTile(
                    icon = Icons.Filled.Star,
                    tileSize = 44.dp,
                    palette = TilePalette.GOLD,
                    contentDescription = null
                )
            }
            Spacer(Modifier.height(14.dp))
            GoldDivider()
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

/** نوار اطلاع‌رسانی نرم (آفلاین، هشدار) — کهربایی روشن */
@Composable
fun SoftBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0x66E2EDFB), Color(0x590B4F9E))
                )
            )
            .border(goldBorder(0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Cocoa
        )
    }
}

/** فضای خالی استاندارد پایین صفحه تا زیر نوار پیمایش شناور */
@Composable
fun BottomBarSpacer() = Spacer(Modifier.height(104.dp))
