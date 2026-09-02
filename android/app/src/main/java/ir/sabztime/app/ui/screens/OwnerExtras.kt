package ir.sabztime.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.sabztime.app.data.BusinessDetails
import ir.sabztime.app.data.BusinessBooking
import ir.sabztime.app.data.DaySchedule
import ir.sabztime.app.data.ScheduleData
import ir.sabztime.app.ui.components.GlassCard
import ir.sabztime.app.ui.components.GradientButton
import ir.sabztime.app.ui.components.StatusChip
import ir.sabztime.app.util.Jalali
import androidx.compose.material3.AlertDialog

/** ترتیب هفتگی فارسی با نگاشت به dayOfWeek سرور (0=یکشنبه … 6=شنبه). */
private val PERSIAN_WEEK: List<Pair<Int, String>> = listOf(
    6 to "شنبه", 0 to "یکشنبه", 1 to "دوشنبه", 2 to "سه‌شنبه",
    3 to "چهارشنبه", 4 to "پنج‌شنبه", 5 to "جمعه"
)

private val SLOT_OPTIONS = listOf(15, 30, 45, 60, 90)

/** کارت برنامهٔ کاری هفتگی: فعال/غیرفعال هر روز + ساعت شروع/پایان + مدت اسلات. */
@Composable
fun ScheduleCard(
    schedule: ScheduleData?,
    busy: Boolean,
    onSave: (List<DaySchedule>, Int) -> Unit
) {
    val loaded = schedule ?: ScheduleData(
        slotDurationMinutes = 30,
        schedules = PERSIAN_WEEK.map { (dow, _) ->
            DaySchedule(dayOfWeek = dow, startTime = "09:00", endTime = "18:00", isActive = if (dow == 5) 0 else 1)
        }
    )
    // با هر بار رسیدن دادهٔ سرور (schedule)، وضعیت ویرایش از سرور بازسازی می‌شود
    var days by remember(schedule) {
        mutableStateOf(
            PERSIAN_WEEK.map { (dow, _) ->
                loaded.schedules.firstOrNull { it.dayOfWeek == dow }
                    ?: DaySchedule(dayOfWeek = dow, startTime = "09:00", endTime = "18:00", isActive = if (dow == 5) 0 else 1)
            }
        )
    }
    var slot by remember(schedule) { mutableStateOf(loaded.slotDurationMinutes.coerceIn(5, 240)) }
    var pickDay by remember { mutableStateOf<Int?>(null) }
    var pickField by remember { mutableStateOf(0) } // 0=شروع، 1=پایان

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "برنامهٔ کاری",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                if (days.any { it.isActive == 1 }) "فعال" else "تعطیل",
                if (days.any { it.isActive == 1 }) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                if (days.any { it.isActive == 1 }) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { index, day ->
                val name = PERSIAN_WEEK[index].second
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(72.dp)
                    )
                    if (day.isActive == 1) {
                        TextButton(
                            onClick = { pickDay = index; pickField = 0 },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(Jalali.persianDigits(day.startTime ?: "—"), color = MaterialTheme.colorScheme.primary) }
                        Text("تا", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(
                            onClick = { pickDay = index; pickField = 1 },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(Jalali.persianDigits(day.endTime ?: "—"), color = MaterialTheme.colorScheme.primary) }
                    } else {
                        Text(
                            "تعطیل",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = day.isActive == 1,
                        onClick = {
                            if (!busy) {
                                days = days.mapIndexed { i, d ->
                                    if (i == index) d.copy(isActive = if (d.isActive == 1) 0 else 1) else d
                                }
                            }
                        },
                        label = {
                            Text(if (day.isActive == 1) "کاری" else "غیرفعال", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "مدت هر اسلات (دقیقه)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            SLOT_OPTIONS.forEach { opt ->
                FilterChip(
                    selected = slot == opt,
                    onClick = { if (!busy) slot = opt },
                    label = { Text(Jalali.persianDigits(opt)) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        GradientButton(
            onClick = { onSave(days, slot) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ برنامهٔ کاری", fontWeight = FontWeight.Bold)
        }
    }

    val pickIndex = pickDay
    if (pickIndex != null) {
        val current = if (pickField == 0) days[pickIndex].startTime else days[pickIndex].endTime
        TimePickerDialog(
            title = if (pickField == 0) "ساعت شروع" else "ساعت پایان",
            initial = current,
            onDismiss = { pickDay = null },
            onConfirm = { t ->
                days = days.mapIndexed { i, d ->
                    if (i == pickIndex) {
                        if (pickField == 0) d.copy(startTime = t) else d.copy(endTime = t)
                    } else d
                }
                pickDay = null
            }
        )
    }
}

/** انتخاب ساعت از چیپ‌ها: ساعت (۰ تا ۲۳) و دقیقه (۰۰/۱۵/۳۰/۴۵). */
@Composable
fun TimePickerDialog(
    title: String,
    initial: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initial?.split(":")
    var hour by remember { mutableStateOf((parts?.firstOrNull()?.toIntOrNull() ?: 9).coerceIn(0, 23)) }
    var minute by remember { mutableStateOf((parts?.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "ساعت",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    (0..23).forEach { h ->
                        FilterChip(
                            selected = hour == h,
                            onClick = { hour = h },
                            label = { Text(Jalali.persianDigits("%02d".format(h))) }
                        )
                    }
                }
                Text(
                    "دقیقه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        FilterChip(
                            selected = minute == m,
                            onClick = { minute = m },
                            label = { Text(Jalali.persianDigits("%02d".format(m))) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm("%02d:%02d".format(hour, minute)) }) { Text("تأیید ساعت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** کارت لینک اختصاصی: نمایش + کپی + ویرایش نامک. */
@Composable
fun LinkCard(
    currentAlias: String?,
    busy: Boolean,
    onEdit: () -> Unit,
    onCopy: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val fullLink = currentAlias?.let { "https://webnovo.ir/start/$it" }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "لینک اختصاصی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (fullLink != null) {
                IconButton(onClick = { onCopy(fullLink) }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "کپی لینک",
                        tint = cs.primary
                    )
                }
            }
            IconButton(onClick = onEdit, enabled = !busy) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "ویرایش لینک",
                    tint = cs.primary
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (fullLink != null) {
            Text(
                fullLink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.primary,
                maxLines = 1
            )
            Text(
                "این لینک را با مشتری‌ها به اشتراک بگذارید؛ با باز کردن آن مستقیم به صفحهٔ رزرو شما می‌رسند.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        } else {
            Text(
                "هنوز لینک اختصاصی ندارید. با ساختن یک نامک، لینک اختصاصی کسب‌وکار خود را داشته باشید.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            GradientButton(onClick = onEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("ساخت لینک اختصاصی", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** سطر رزرو در فهرست رزروها + دکمه‌های ثبت حضور/عدم حضور. */
@Composable
fun BookingRow(
    booking: BusinessBooking,
    busy: Boolean,
    onAttended: () -> Unit,
    onNoShow: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cancelled = booking.status == "cancelled"
    val attended = booking.attendanceStatus == "attended"
    val noShow = booking.attendanceStatus == "no_show"
    val canMarkAttendance = !cancelled && !attended && !noShow &&
        (booking.attendanceStatus == null || booking.attendanceStatus == "pending")

    GlassCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.alpha(if (cancelled) 0.65f else 1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        booking.customerName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    val whenText = listOfNotNull(
                        booking.dayName,
                        booking.jalaliDate?.let { Jalali.persianDigits(it) } ?: booking.bookingDate
                    ).joinToString("، ")
                    val timeText = if (booking.startTime.isNotBlank()) " • ${Jalali.persianDigits(booking.startTime)} تا ${Jalali.persianDigits(booking.endTime)}" else ""
                    Text(
                        whenText + timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
                booking.customerPhone?.let {
                    Text(
                        Jalali.persianDigits(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    when {
                        cancelled -> "لغو شده"
                        booking.status == "completed" -> "تکمیل شده"
                        else -> "رزرو شده"
                    },
                    when {
                        cancelled -> cs.onErrorContainer
                        booking.status == "completed" -> cs.onPrimaryContainer
                        else -> cs.onSurfaceVariant
                    },
                    when {
                        cancelled -> cs.errorContainer
                        booking.status == "completed" -> cs.primaryContainer
                        else -> cs.surfaceVariant
                    }
                )
                when {
                    attended -> StatusChip("🟢 حضور ثبت شده", cs.onPrimaryContainer, cs.primaryContainer)
                    noShow -> StatusChip("🔴 عدم حضور", cs.onErrorContainer, cs.errorContainer)
                }
                if (booking.customerCredit != 0) {
                    Text(
                        "⭐ امتیاز مشتری: ${Jalali.persianDigits(booking.customerCredit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            if (canMarkAttendance) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAttended,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🟢 ثبت حضور", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onNoShow,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔴 عدم حضور", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** دیالوگ ویرایش اطلاعات پایهٔ کسب‌وکار. */
@Composable
fun EditBusinessDialog(
    initial: BusinessDetails,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.businessType ?: "") }
    var phone by remember { mutableStateOf(initial.phone ?: "") }
    var province by remember { mutableStateOf(initial.province ?: "") }
    var city by remember { mutableStateOf(initial.city ?: "") }
    var address by remember { mutableStateOf(initial.address ?: "") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("ویرایش کسب‌وکار", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کسب‌وکار") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("نوع کسب‌وکار") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس (۰۹…)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("استان") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("شهر") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("نشانی (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name.trim(), type.trim(), phone.trim(), province.trim(), city.trim(), address.trim()) },
                enabled = !busy && name.isNotBlank() && type.isNotBlank() && phone.isNotBlank()
            ) { Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ تغییرات") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("انصراف") } }
    )
}

/** دیالوگ ساخت/تغییر نامک لینک اختصاصی. */
@Composable
fun LinkAliasDialog(
    initialAlias: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var alias by remember { mutableStateOf(initialAlias ?: "") }
    val valid = Regex("^[a-zA-Z0-9_]{3,50}$").matches(alias.trim())
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("لینک اختصاصی", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("نامک (مثلاً my_salon)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "فقط حروف انگلیسی، اعداد و خط زیر (_)؛ بین ۳ تا ۵۰ کاراکتر.\nلینک نهایی: webnovo.ir/start/<نامک>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(alias.trim()) }, enabled = !busy && valid) {
                Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ لینک")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("انصراف") } }
    )
}
