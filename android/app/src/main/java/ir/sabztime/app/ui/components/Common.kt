package ir.sabztime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.sabztime.app.core.SabzException
import ir.sabztime.app.core.UiState

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "در حال بارگذاری…") {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.glass.cardTop, MaterialTheme.glass.cardBottom)
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(MaterialTheme.glass.strokeTop, MaterialTheme.glass.strokeBottom)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.07f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(MaterialTheme.glass.strokeTop, MaterialTheme.glass.strokeBottom)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (kind == SabzException.Kind.NETWORK) Icons.Filled.CloudOff else Icons.Filled.ErrorOutline,
                    contentDescription = "خطا",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onRetry != null) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تلاش دوباره")
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
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EmojiBadge3D(icon, size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                Spacer(Modifier.height(20.dp))
                action()
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = modifier.fillMaxWidth(), content = { content() })
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color, container: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.AssistChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelMedium, color = color) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = container,
            labelColor = color
        ),
        modifier = Modifier.height(28.dp)
    )
}

@Composable
fun ListDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

/** کلیک‌ناپذیر ولی برای نمایش امضا (signature) */
@Composable
fun SpacerH(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun SpacerW(width: Int) = Spacer(Modifier.width(width.dp))

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this
