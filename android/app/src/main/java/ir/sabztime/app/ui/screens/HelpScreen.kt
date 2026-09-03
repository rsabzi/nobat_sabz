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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.sabztime.app.core.AppGraph
import ir.sabztime.app.ui.Routes
import ir.sabztime.app.ui.theme.GlassBackground
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GlassTopBar
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.IconTile
import ir.sabztime.app.ui.components.TilePalette
import ir.sabztime.app.util.Jalali

/**
 * راهنمای تعاملی — هم‌ساختار راهنمای ربات: گام‌به‌گام، با دکمهٔ رفتن به
 * همان بخش و در انتها راهنمای سریع.
 */
private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val content: String,
    val actionLabel: String?,
    val actionRoute: String?
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        icon = Icons.Filled.Stars,
        title = "خوش آمدید به نوبت‌سبز! 🎉",
        content = "نوبت‌سبز سیستم نوبت‌دهی هوشمند است؛ همین اپلیکیشن و ربات تلگرام نوبت‌سبز از یک سیستم و دیتابیس مشترک استفاده می‌کنند. در چند قدم کوتاه همهٔ قابلیت‌ها را با هم می‌بینیم.",
        actionLabel = null,
        actionRoute = null
    ),
    GuideStep(
        icon = Icons.Filled.Storefront,
        title = "ثبت کسب‌وکار 🏪",
        content = "برای شروع، کسب‌وکار خود را ثبت کنید: نام، نوع، شماره تماس، استان، شهر و نشانی. می‌توانید کسب‌وکارهایی که قبلاً در ربات ثبت کرده‌اید را همین‌جا ببینید و مدیریت کنید.",
        actionLabel = "رفتن به کسب‌وکارهای من",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.Work,
        title = "تعریف خدمات ⚙️",
        content = "هر خدمتی که ارائه می‌دهید را با نام، قیمت و مدت زمان تعریف کنید. مشتری‌ها دقیقاً همین خدمات را برای رزرو می‌بینند. افزودن و حذف خدمت از همان صفحهٔ مدیریت کسب‌وکار انجام می‌شود.",
        actionLabel = "مدیریت کسب‌وکار",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.Schedule,
        title = "برنامهٔ کاری 📅",
        content = "ساعات کاری هر روز هفته را تنظیم کنید: برای هر روز بازهٔ شروع و پایان تعیین می‌کنید یا آن را تعطیل می‌کنید. مدت هر اسلات (مثلاً ۳۰ دقیقه) را هم می‌توانید تغییر دهید.",
        actionLabel = "تنظیم برنامهٔ کاری",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.Link,
        title = "لینک اختصاصی 🔗",
        content = "برای کسب‌وکار خود یک نامک (مثلاً my_salon) انتخاب کنید تا لینک اختصاصی داشته باشید. این لینک را با مشتری‌ها به اشتراک بگذارید تا با باز کردن آن مستقیم به صفحهٔ رزرو شما برسند.",
        actionLabel = "ساخت لینک اختصاصی",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.Queue,
        title = "صف و حضور/غیبت ‍‍🧑",
        content = "در «صف امروز» نوبت‌ها را می‌بینید: نوبت بعدی را فراخوانی کنید و پس از خدمت، تکمیلش کنید. در «فهرست رزروها» برای هر مشتری می‌توانید حضور یا عدم حضور ثبت کنید؛ حضور +۱ و غیبت −۱ امتیاز مشتری می‌شود.",
        actionLabel = "مدیریت صف",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.ListAlt,
        title = "فهرست رزروها 📋",
        content = "همهٔ رزروهای کسب‌وکار — از امروز به بعد یا کل تاریخچه — با نام مشتری، شماره تماس، زمان و وضعیت حضور نمایش داده می‌شود تا هیچ نوبتی از دست نرود.",
        actionLabel = "دیدن فهرست رزروها",
        actionRoute = Routes.MY_BUSINESSES
    ),
    GuideStep(
        icon = Icons.Filled.Campaign,
        title = "حساب بازاریابی 📣",
        content = "می‌توانید با یک کلیک حساب بازاریابی خود را فعال کنید: کد اختصاصی بازاریابی و لینک معرفی برای شما ساخته می‌شود. هر کسب‌وکاری که با لینک شما ثبت شود، در داشبورد بازاریاب به‌همراه کمیسیون قابل مشاهده است.",
        actionLabel = "رفتن به داشبورد بازاریاب",
        actionRoute = Routes.MARKETER
    ),
    GuideStep(
        icon = Icons.Filled.CardMembership,
        title = "رزرو و امتیاز اعتباری ⭐",
        content = "مشتری‌ها از صفحهٔ اصلی اپ، کسب‌وکار را جستجو، خدمت، تاریخ و ساعت را انتخاب و رزرو می‌کنند. حضور به‌موقع +۱ و لغو یا غیبت −۱ امتیاز دارد. لغو نوبت تا یک ساعت قبل از شروع ممکن است.",
        actionLabel = null,
        actionRoute = null
    )
)

@Composable
fun HelpScreen(graph: AppGraph, navController: NavHostController) {
    var stepIndex by remember { mutableStateOf(0) }
    val finished = stepIndex >= GUIDE_STEPS.size

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar(
                    title = if (finished) "راهنمای سریع" else "راهنمای تعاملی",
                    onBack = { navController.popBackStack() },
                    actions = {
                        if (!finished) {
                            TextButton(onClick = { navController.popBackStack() }) {
                                Text("رد کردن راهنما")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (finished) {
                QuickHelpList(modifier = Modifier.fillMaxSize().padding(padding))
            } else {
                val step = GUIDE_STEPS[stepIndex]
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        "گام ${JalaliPersian(stepIndex + 1)} از ${JalaliPersian(GUIDE_STEPS.size)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (stepIndex + 1).toFloat() / GUIDE_STEPS.size },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    GlassCard {
                        IconTile(icon = step.icon, tileSize = 56.dp, contentDescription = null)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            step.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            step.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (step.actionLabel != null && step.actionRoute != null) {
                            Spacer(Modifier.height(14.dp))
                            GradientButton(
                                onClick = {
                                    navController.navigate(step.actionRoute) {
                                        popUpTo(Routes.MAIN)
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(step.actionLabel, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(
                            onClick = { if (stepIndex > 0) stepIndex-- },
                            enabled = stepIndex > 0
                        ) {
                            Text("گام قبل")
                        }
                        Spacer(Modifier.weight(1f))
                        GradientButton(
                            onClick = { stepIndex++ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (stepIndex > 0) {
                                androidx.compose.material3.Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.width(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (stepIndex == GUIDE_STEPS.size - 1) "پایان و راهنمای سریع" else "گام بعد",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickHelpList(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.Search, tileSize = 44.dp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("رزرو نوبت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "۱. در صفحه اصلی، کسب‌وکار موردنظر را جستجو کنید.\n" +
                        "۲. یکی از خدمات را انتخاب کنید.\n" +
                        "۳. تاریخ و ساعت خالی را انتخاب کنید.\n" +
                        "۴. اطلاعات تماس را وارد و رزرو را نهایی کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.ListAlt, tileSize = 44.dp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("مدیریت نوبت‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "از بخش «نوبت‌های من» تمام رزروها را ببینید. تا یک ساعت قبل از شروع می‌توانید نوبت را لغو کنید (۱ امتیاز کسر می‌شود). رزرو موفق و یادآوری‌ها از طریق ربات تلگرام نیز اطلاع‌رسانی می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.Stars, tileSize = 44.dp, palette = TilePalette.AMBER, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("امتیاز اعتباری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "حضور به‌موقع در نوبت +۱ امتیاز و عدم حضور یا لغو −۱ امتیاز دارد. امتیاز بالاتر یعنی اعتبار بیشتر در رزروها.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.Storefront, tileSize = 44.dp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("صاحبان کسب‌وکار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "از بخش «کسب‌وکارهای من» کسب‌وکار خود را مدیریت کنید: مشاهده نوبت‌ها و صف امروز، فراخوانی نوبت بعدی، ثبت حضور/غیبت، ویرایش و حذف کسب‌وکار، برنامهٔ کاری، لینک اختصاصی و خدمات.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.Campaign, tileSize = 44.dp, palette = TilePalette.AMBER, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("بازاریابان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "حساب بازاریابی خود را از داشبورد بازاریاب فعال کنید؛ کد اختصاصی و لینک معرفی برای شما ساخته می‌شود و کمیسیون هر معرفی در همان داشبورد قابل مشاهده است.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Row {
                    IconTile(icon = Icons.Filled.CardMembership, tileSize = 44.dp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("درباره نوبت‌سبز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "نوبت‌سبز سیستم مدیریت نوبت‌دهی است؛ همین اپلیکیشن به همراه ربات تلگرام نوبت‌سبز از یک سیستم و دیتابیس مشترک استفاده می‌کنند. برای پشتیبانی، در ربات تلگرام پیام بدهید.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** عدد فارسی ساده برای شمارش گام‌ها. */
private fun JalaliPersian(n: Int): String = Jalali.persianDigits(n)
