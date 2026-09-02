package ir.sabztime.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.sabztime.app.ui.theme.Gold
import ir.sabztime.app.ui.theme.GoldBright
import ir.sabztime.app.ui.theme.GoldDeep
import ir.sabztime.app.ui.theme.GoldLight
import ir.sabztime.app.ui.theme.InfoSapphire
import ir.sabztime.app.ui.theme.NavBarShape
import ir.sabztime.app.ui.theme.SuccessEmerald
import ir.sabztime.app.ui.theme.TextMuted
import ir.sabztime.app.ui.theme.neonGlow

/**
 * یک آیتم نوار پیمایش پایینی.
 * @param accent رنگ متریال اختصاصی آیکون سه‌بعدی (طلایی، سبز، آبی، بنفش)
 */
data class LuxNavItem(
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

/** چهار تب استاندارد اپ */
val DefaultNavItems: List<LuxNavItem> = listOf(
    LuxNavItem("خانه", Icons.Filled.Cottage, GoldLight),
    LuxNavItem("نوبت‌ها", Icons.Filled.CalendarMonth, SuccessEmerald),
    LuxNavItem("پیام‌ها", Icons.Filled.Forum, InfoSapphire),
    LuxNavItem("پروفایل", Icons.Filled.Person, Color(0xFFB98CE0))
)

/**
 * نوار پیمایش شناور با افکت شیشه‌ای (Glassmorphism) و حاشیه‌ی طلایی ظریف.
 * آیکون فعال دارای هاله‌ی نئونی طلایی و برچسب طلایی است؛ بقیه بدون برچسب.
 */
@Composable
fun LuxBottomBar(
    items: List<LuxNavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(22.dp, NavBarShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(NavBarShape)
                // شیشه: لایه‌ی سرمه‌ای نیمه‌شفاف + براقی از بالا
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xE61A2444),
                            Color(0xF00C1229)
                        )
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x26FFFFFF), Color(0x05FFFFFF))
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                GoldBright.copy(alpha = 0.55f),
                                Gold.copy(alpha = 0.18f),
                                GoldBright.copy(alpha = 0.45f)
                            )
                        )
                    ),
                    NavBarShape
                )
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                LuxNavButton(
                    item = item,
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LuxNavButton(
    item: LuxNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = tween(240),
        label = "navScale"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // هاله‌ی نئونی طلایی پیرامون آیکون فعال
            if (selected) {
                Box(
                    Modifier
                        .size(62.dp)
                        .neonGlow(color = GoldLight, alpha = 0.55f)
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                NavIcon3D(icon = item.icon, accent = item.accent, selected = selected, label = item.label)
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }
        }
    }
}

/**
 * آیکون «سه‌بعدی» نوار پیمایش — کره‌ی گرادیانی با متریال لوکس،
 * لبه‌ی طلایی و آیکون رنگ‌آمیزی‌شده با گرادیان اختصاصی هر تب.
 */
@Composable
private fun NavIcon3D(
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    label: String
) {
    val shape = CircleShape
    val tint = if (selected) {
        Brush.linearGradient(listOf(GoldLight, accent, GoldDeep))
    } else {
        Brush.linearGradient(listOf(TextMuted.copy(alpha = 0.85f), TextMuted.copy(alpha = 0.55f)))
    }
    Box(contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(BorderStroke(1.dp, GoldLight.copy(alpha = 0.35f)), shape)
            )
        }
        Box(
            Modifier
                .size(25.dp)
                .brushedGoldTint(tint)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                tint = Color.White
            )
        }
    }
}
