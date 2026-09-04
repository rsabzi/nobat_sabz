#!/usr/bin/env bash
# تست‌های دود API: احراز هویت موبایل + ساخت کسب‌وکار از طریق خود API + جستجو + رزرو + حذف حساب
# پیش‌نیاز: سرور PHP روی 127.0.0.1:8090 با MySQL تستی و جداول اولیه
# نکته: -e عمداً حذف شده تا شکست یک درخواست، بقیه‌ی چک‌ها را نکشد و همه در CI دیده شوند
set -uo pipefail

BASE="http://127.0.0.1:8090/webapp"
PASS=0; FAIL=0

FAILDUMP=""
check() {
  local name="$1" cond="$2" resp="${3:-}"
  if eval "$cond"; then PASS=$((PASS+1)); echo "✅ $name"
  else
    FAIL=$((FAIL+1)); echo "❌ $name"
    # در CI به‌صورت annotation نمایش داده می‌شود تا بدون دانلود لاگ هم دیده شود
    [ -n "${GITHUB_ACTIONS:-}" ] && echo "::error::❌ $name"
    # جمع‌آوری پاسخ برای یک annotation واحد در پایان (سقف annotation گیت‌هاب ۱۰ است)
    FAILDUMP+="⟨$name⟩ $(echo "$resp" | head -c 220 | tr '\n' ' ') "
  fi
}

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }

# --- 1) ثبت‌نام ---
REG=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"register","phone":"09121111111","password":"secret123","name":"کاربر تست","device":"ci"
}')
check "ثبت‌نام موفق" "echo '$REG' | grep -q '\"success\"'"
check "توکن دریافت شد" "echo '$REG' | grep -q '\"token\"'"
TOKEN=$(echo "$REG" | json "d.get('token','')")
[ -z "$TOKEN" ] && { echo "::error::بدون توکن — ثبت‌نام شکست خورد"; exit 1; }

AUTH=(-H "X-App-Token: $TOKEN" -H 'Content-Type: application/json')

# --- 2) ورود مجدد و پروفایل ---
LOGIN=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"login","phone":"09121111111","password":"secret123","device":"ci"
}')
check "ورود موفق" "echo '$LOGIN' | grep -q '\"success\"'"
TOKEN=$(echo "$LOGIN" | json "d.get('token','')")
AUTH=(-H "X-App-Token: $TOKEN" -H 'Content-Type: application/json')

ME=$(curl -s -X POST "$BASE/mobile_auth.php" "${AUTH[@]}" -d '{"action":"me"}')
check "پروفایل (me) با توکن" "echo '$ME' | grep -q '\"success\"'"
check "نام کاربری در پروفایل" "echo '$ME' | grep -q 'کاربر تست'"

# --- 3) ورود با رمز اشتباه رد شود ---
BAD=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"login","phone":"09121111111","password":"wrongpass"
}')
check "رد ورود با رمز اشتباه" "echo '$BAD' | grep -q '\"error\"'"

# --- 4) دسترسی بدون توکن رد شود ---
NOME=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{"action":"me"}')
check "me بدون توکن → 401" "echo '$NOME' | grep -q 'AUTH_REQUIRED'"

# --- 5) ساخت کسب‌وکار از طریق خود API (همان مسیر واقعی اپ؛ مالک = کاربر احرازشده) ---
ADD=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{
  "action":"add_business","name":"آرایشگاه تست","type":"آرایشی و زیبایی",
  "phone":"09120000000","province":"تهران","city":"تهران","address":"خیابان تست","marketer_code":""
}')
check "ایجاد کسب‌وکار (add_business)" "echo '$ADD' | grep -q '\"success\"'"
BID=$(echo "$ADD" | json "d.get('business_id',0)")
echo "📋 business_id=$BID"

if [ -n "$BID" ] && [ "$BID" != "0" ]; then
  # خدمات (فرمت یکسان با SabzRepository.updateServices)
  SVCS=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{
    \"action\":\"update_services\",\"business_id\":$BID,
    \"services\":[
      {\"id\":0,\"name\":\"کوتاهی مو\",\"description\":\"\",\"price\":50000,\"duration_minutes\":30,\"is_active\":true},
      {\"id\":0,\"name\":\"رنگ مو\",\"description\":\"\",\"price\":200000,\"duration_minutes\":120,\"is_active\":true}
    ]
  }")
  check "ثبت خدمات (update_services)" "echo '$SVCS' | grep -q '\"success\"'"

  # برنامه‌ی کاری هفتگی (فرمت یکسان با SabzRepository.saveSchedule)
  SCHED=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{
    \"action\":\"update_schedule\",\"business_id\":$BID,\"slot_duration_minutes\":30,
    \"schedules\":[
      {\"day_of_week\":0,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":1,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":2,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":3,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":4,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":5,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true},
      {\"day_of_week\":6,\"start_time\":\"09:00\",\"end_time\":\"17:00\",\"is_active\":true}
    ]
  }")
  check "ثبت برنامه‌ی کاری (update_schedule)" "echo '$SCHED' | grep -q '\"success\"'"
fi

# --- 6) جستجو و جزئیات کسب‌وکار ---
SEARCH=$(curl -s "$BASE/search_api.php?action=search&query=آرایشگاه")
check "جستجو کار می‌کند" "echo '$SEARCH' | grep -q '\"success\":true'" "$SEARCH" "$SEARCH"

if [ -n "$BID" ] && [ "$BID" != "0" ]; then
  DETAILS=$(curl -s "$BASE/search_api.php?action=business_details&id=$BID")
  check "جزئیات کسب‌وکار + خدمات" "echo '$DETAILS' | grep -q 'کوتاهی مو'"

  # --- 7) تاریخ/ساعت و رزرو ---
  DATE=$(curl -s "$BASE/api.php?action=get_available_dates&id=$BID")
  check "تاریخ‌های آزاد" "echo '$DATE' | grep -q '\"status\":\"success\"'"
  BDAY=$(echo "$DATE" | json "d['data'][0]['date'] if d.get('data') else ''")

  SLOTS=$(curl -s "$BASE/api.php?action=get_available_slots&id=$BID&date=$BDAY")
  check "اسلات‌های آزاد" "echo '$SLOTS' | grep -q '\"status\":\"success\"'"

  if [ -n "$BDAY" ]; then
    BK=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{
      \"action\":\"create_booking\",\"business_id\":$BID,\"date\":\"$BDAY\",
      \"start_time\":\"09:00\",\"end_time\":\"09:30\",
      \"customer_name\":\"کاربر تست\",\"customer_phone\":\"09121111111\",\"customer_notes\":\"تست CI\"
    }")
    check "ایجاد رزرو" "echo '$BK' | grep -q '\"status\":\"success\"'" "$BK" "$BK"
    BKKID=$(echo "$BK" | json "d.get('booking_id',0)")

    MYBK=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"get_user_bookings"}')
    check "لیست رزروهای کاربر" "echo '$MYBK' | grep -q '\"status\":\"success\"'" "$MYBK" "$MYBK"
    check "رزرو کاربر در پاسخ" "echo '$MYBK' | grep -q 'آرایشگاه تست'"

    if [ -n "$BKKID" ] && [ "$BKKID" != "0" ]; then
      CANCEL=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{\"action\":\"cancel_booking\",\"booking_id\":$BKKID}")
      check "لغو رزرو" "echo '$CANCEL' | grep -q '\"status\":\"success\"' || echo '$CANCEL' | grep -q 'یک ساعت'"
    fi
  fi
fi

# --- 8) کسب‌وکارهای من و گارد شناسه ---
MYBIZ=$(curl -s "$BASE/api.php?action=get_my_businesses" "${AUTH[@]}")
check "get_my_businesses" "echo '$MYBIZ' | grep -q 'آرایشگاه تست'"
check "get_my_businesses بدون خطای Business ID" "! echo '$MYBIZ' | grep -q 'Business ID is required'"

MARKETER=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"register_marketer"}')
check "register_marketer بدون خطای Business ID" "! echo '$MARKETER' | grep -q 'Business ID is required'"

MKDASH=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"get_marketer_dashboard"}')
check "داشبورد بازاریاب بدون خطای Business ID" "! echo '$MKDASH' | grep -q 'Business ID is required'"

PLANS=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"get_subscription_plans"}')
check "پلن‌های اشتراک" "echo '$PLANS' | grep -q '\"status\":\"success\"'"

NOTIF=$(curl -s -X POST "$BASE/mobile_auth.php" "${AUTH[@]}" -d '{"action":"get_notifications"}')
check "اعلان‌ها" "echo '$NOTIF' | grep -q '\"status\":\"success\"'"

# --- 9) حذف حساب (الزام فروشگاه‌ها): کاربر موقت + حذف + تأیید مرگ حساب ---
REG2=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"register","phone":"09123333333","password":"temp123456","name":"کاربر حذفی","device":"ci"
}')
check "ثبت‌نام کاربر موقت" "echo '$REG2' | grep -q '\"success\"'"
TOKEN2=$(echo "$REG2" | json "d.get('token','')")

if [ -n "$TOKEN2" ]; then
  DEL=$(curl -s -X POST "$BASE/mobile_auth.php" -H "X-App-Token: $TOKEN2" -H 'Content-Type: application/json' \
        -d '{"action":"delete_account"}')
  check "حذف حساب موفق" "echo '$DEL' | grep -q '\"success\"'"
  # توکن قدیمی باید مرده باشد
  ME2=$(curl -s -X POST "$BASE/mobile_auth.php" -H "X-App-Token: $TOKEN2" -H 'Content-Type: application/json' -d '{"action":"me"}')
  check "توکن پس از حذف بی‌اعتبار" "echo '$ME2' | grep -q '\"error\"'"
  # ورود مجدد باید رد شود
  LOGIN2=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
    "action":"login","phone":"09123333333","password":"temp123456","device":"ci"
  }')
  check "ورود حساب حذف‌شده رد می‌شود" "echo '$LOGIN2' | grep -q '\"error\"'"
fi

# --- 10) صفحه‌ی حریم خصوصی در دسترس است ---
PRIV=$(curl -s "$BASE/privacy.php")
check "صفحه‌ی حریم خصوصی" "echo '$PRIV' | grep -q 'حریم خصوصی'"

echo "----------------------------------------"
echo "PASS=$PASS FAIL=$FAIL"
[ -n "${GITHUB_ACTIONS:-}" ] && [ -n "$FAILDUMP" ] && echo "::error::DUMP: ${FAILDUMP:0:1000}"
[ "$FAIL" -eq 0 ]
