package ir.sabztime.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(graph: AppGraph, navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("راهنما", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Text("🌿 نوبت‌سبز چیست؟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "نوبت‌سبز سیستم مدیریت نوبت‌دهی است؛ همین اپلیکیشن به همراه ربات تلگرام نوبت‌سبز از یک سیستم و دیتابیس مشترک استفاده می‌کنند.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionCard {
                Text("🔍 رزرو نوبت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "۱. در صفحه اصلی کسب‌وکار موردنظر را جستجو کنید.\n" +
                        "۲. یکی از خدمات را انتخاب کنید.\n" +
                        "۳. تاریخ و ساعت خالی را انتخاب کنید.\n" +
                        "۴. اطلاعات تماس را وارد و رزرو را نهایی کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionCard {
                Text("📋 مدیریت نوبت‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "از بخش «نوبت‌های من» تمام رزروها را ببینید. تا یک ساعت قبل از شروع می‌توانید نوبت را لغو کنید (۱ امتیاز کسر می‌شود). رزرو موفق و یادآوری‌ها از طریق ربات تلگرام نیز اطلاع‌رسانی می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionCard {
                Text("⭐ امتیاز اعتباری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "حضور به‌موقع در نوبت +۱ امتیاز، عدم حضور و لغو -۱ امتیاز دارد. امتیاز بالاتر یعنی اعتبار بیشتر در رزروها.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionCard {
                Text("🏪 صاحبان کسب‌وکار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "از بخش «کسب‌وکارهای من» صف امروز را مدیریت کنید: مشاهده نوبت‌ها، فراخوانی نوبت بعدی و تکمیل خدمت. خرید اشتراک و مدیریت خدمات از طریق ربات تلگرام انجام می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionCard {
                Text("📣 بازاریابان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "لینک اختصاصی بازاریابی خود را از ربات دریافت کنید؛ با هر معرفی، کمیسیون در داشبورد این اپلیکیشن قابل مشاهده است.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
