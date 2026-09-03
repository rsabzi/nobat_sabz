package ir.sabztime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState

// ---------------------------------------------------------------------------
// وضعیت‌های بارگذاری / خطا / خالی
// ---------------------------------------------------------------------------

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "در حال بارگذاری…") {
    // fillMaxWidth + حداقل ارتفاع: هم در صفحه (با fillMaxSize از بیرون) و هم
    // داخل آیتم LazyColumn بی‌خطا کار می‌کند.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    kind: SabzException.Kind = SabzException.Kind.SERVER,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconTile(
                icon = if (kind == SabzException.Kind.NETWORK) Icons.Filled.CloudOff else Icons.Filled.ErrorOutline,
                tileSize = 68.dp,
                selected = false,
                palette = TilePalette.ERROR
            )
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onRetry != null) {
                Spacer(Modifier.height(20.dp))
                GradientButton(onClick = onRetry, modifier = Modifier.width(230.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تلاش دوباره", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: String = "📭",
    title: String = "موردی یافت نشد",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xCCFFFFFF), Color(0x99F3ECDD))
                        )
                    )
                    .border(
                        ir.sabztime.app.ui.theme.goldBorder(0.55f),
                        RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, style = MaterialTheme.typography.displayMedium)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (action != null) {
                Spacer(Modifier.height(18.dp))
                action()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// عناصر کوچک
// ---------------------------------------------------------------------------

/** کارت شیشه‌ای با عنوان داخلی (هم‌ارز قدیمی SectionCard) */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = GlassCard(modifier = modifier, content = content)

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusChip(text: String, color: Color, container: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/** آمار کوچک: عدد بزرگ + برچسب */
@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ListDivider() = GoldDivider()

@Composable
fun SpacerH(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun SpacerW(width: Int) = Spacer(Modifier.width(width.dp))

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this

// ---------------------------------------------------------------------------
// نگاشت نوع کسب‌وکار → آیکون
// ---------------------------------------------------------------------------

fun businessTypeIcon(type: String?): ImageVector = when {
    type.isNullOrBlank() -> Icons.Filled.Storefront
    type.contains("آرایش") || type.contains("ارایش") || type.contains("زیبایی") || type.contains("آرامش") -> Icons.Filled.Spa
    type.contains("باربر") || type.contains("مو") -> Icons.Filled.ContentCut
    type.contains("پزشک") || type.contains("دندان") || type.contains("کلینیک") || type.contains("درمان") || type.contains("بهداش") -> Icons.Filled.HealthAndSafety
    type.contains("پارکینگ") || type.contains("خودرو") || type.contains("تعمیر") || type.contains("مشاوره خودرو") -> Icons.Filled.DirectionsCar
    else -> Icons.Filled.Storefront
}
