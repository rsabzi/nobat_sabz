package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.TilePalette

@Composable
fun HelpScreen(graph: AppGraph, navController: NavHostController) {
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = "راهنما",
                    onBack = { navController.popBackStack() }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpSection(
                    icon = Icons.Filled.Search,
                    title = "رزرو نوبت"
                ) {
                    "۱. در صفحه اصلی، کسب‌وکار موردنظر را جستجو کنید.\n" +
                        "۲. یکی از خدمات را انتخاب کنید.\n" +
                        "۳. تاریخ و ساعت خالی را انتخاب کنید.\n" +
                        "۴. اطلاعات تماس را وارد و رزرو را نهایی کنید."
                }
                HelpSection(
                    icon = Icons.Filled.ListAlt,
                    title = "مدیریت نوبت‌ها"
                ) {
                    "از بخش «نوبت‌های من» تمام رزروها را ببینید. تا یک ساعت قبل از شروع می‌توانید نوبت را لغو کنید (۱ امتیاز کسر می‌شود). رزرو موفق و یادآوری‌ها از طریق ربات تلگرام نیز اطلاع‌رسانی می‌شود."
                }
                HelpSection(
                    icon = Icons.Filled.Star,
                    title = "امتیاز اعتباری",
                    palette = TilePalette.AMBER
                ) {
                    "حضور به‌موقع در نوبت +۱ امتیاز و عدم حضور یا لغو -۱ امتیاز دارد. امتیاز بالاتر یعنی اعتبار بیشتر در رزروها."
                }
                HelpSection(
                    icon = Icons.Filled.Storefront,
                    title = "صاحبان کسب‌وکار"
                ) {
                    "از بخش «کسب‌وکارهای من» صف امروز را مدیریت کنید: مشاهده نوبت‌ها، فراخوانی نوبت بعدی و تکمیل خدمت. خرید اشتراک و مدیریت خدمات از طریق ربات تلگرام انجام می‌شود."
                }
                HelpSection(
                    icon = Icons.Filled.Campaign,
                    title = "بازاریابان",
                    palette = TilePalette.AMBER
                ) {
                    "لینک اختصاصی بازاریابی خود را از ربات دریافت کنید؛ با هر معرفی، کمیسیون در داشبورد این اپلیکیشن قابل مشاهده است."
                }
                HelpSection(
                    icon = Icons.Filled.CardMembership,
                    title = "درباره نوبت‌سبز"
                ) {
                    "نوبت‌سبز سیستم مدیریت نوبت‌دهی است؛ همین اپلیکیشن به همراه ربات تلگرام نوبت‌سبز از یک سیستم و دیتابیس مشترک استفاده می‌کنند. برای پشتیبانی، در ربات تلگرام پیام بدهید."
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HelpSection(
    icon: ImageVector,
    title: String,
    palette: TilePalette = TilePalette.GREEN,
    body: () -> String
) {
    GlassCard {
        Row {
            IconTile(icon = icon, tileSize = 44.dp, palette = palette, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(body(), style = MaterialTheme.typography.bodyMedium)
    }
}
