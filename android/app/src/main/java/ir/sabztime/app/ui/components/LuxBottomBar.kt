package ir.sabztime.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.sabztime.app.ui.theme.Cocoa
import ir.sabztime.app.ui.theme.CocoaSoft
import ir.sabztime.app.ui.theme.NavBarShape
import ir.sabztime.app.ui.theme.goldBorder

/**
 * یک آیتم نوار پیمایش پایینی.
 * @param accent برای سازگاری نگه داشته شده؛ در طراحی جدید فقط آیکونِ کرم روی طلاست.
 */
data class LuxNavItem(
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

/** سه تب استاندارد اپ: خانه، نوبت‌ها، پروفایل */
val DefaultNavItems: List<LuxNavItem> = listOf(
    LuxNavItem("خانه", Icons.Filled.Cottage, Color(0xFFD4AF37)),
    LuxNavItem("نوبت‌ها", Icons.Filled.CalendarMonth, Color(0xFFD4AF37)),
    LuxNavItem("پروفایل", Icons.Filled.Person, Color(0xFFD4AF37))
)

/**
 * نوار پیمایش شناور — قرص فلز طلای برجسته (#E8CE8A→#C9A227) با سه آیکون
 * دایره‌ایِ کرم (۴۶dp) و حلقه‌ی طلایی؛ بدون برچسب متنی.
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .metalPill(NavBarShape)
                .padding(vertical = 12.dp, horizontal = 10.dp),
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
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = tween(220),
        label = "navScale"
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // دایره‌ی کرم ۴۶dp با حلقه‌ی طلایی — فعال: حلقه‌ی پُررنگ، غیرفعال: محو
        val ringWidth = if (selected) 2.2.dp else 1.2.dp
        val ringAlpha = if (selected) 1f else 0.45f
        Box(
            Modifier
                .size(if (selected) 48.dp else 44.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF3ECDD)
                        )
                    )
                )
                .border(goldBorder(ringAlpha, ringWidth), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) Cocoa else CocoaSoft.copy(alpha = 0.85f),
                modifier = Modifier.size(if (selected) 22.dp else 20.dp)
            )
        }
    }
}
