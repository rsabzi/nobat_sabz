#!/usr/bin/env bash
# تست‌های دود API: احراز هویت موبایل + جستجو + رزرو + سوابق
# پیش‌نیاز: سرور PHP روی 127.0.0.1:8090 با MySQL تستی و جداول اولیه
set -euo pipefail

BASE="http://127.0.0.1:8090/webapp"
PASS=0; FAIL=0

check() {
  local name="$1" cond="$2"
  if eval "$cond"; then PASS=$((PASS+1)); echo "✅ $name"; else FAIL=$((FAIL+1)); echo "❌ $name"; fi
}

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }

# --- 1) ثبت‌نام ---
REG=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"register","phone":"09121111111","password":"secret123","name":"کاربر تست","device":"ci"
}')
check "ثبت‌نام موفق" "echo '$REG' | grep -q '\"success\"'"
check "توکن دریافت شد" "echo '$REG' | grep -q '\"token\"'"
TOKEN=$(echo "$REG" | json "d.get('token','')")
[ -z "$TOKEN" ] && { echo "بدون توکن — توقف"; exit 1; }

AUTH=(-H "X-App-Token: $TOKEN" -H 'Content-Type: application/json')

# --- 2) ورود مجدد و پروفایل ---
LOGIN=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' -d '{
  "action":"login","phone":"09121111111","password":"secret123","device":"ci"
}')
check "ورود موفق" "echo '$LOGIN' | grep -q '\"success\"'"
TOKEN=$(echo "$LOGIN" | json "d.get('token','')")

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

# --- 5) جستجو و جزئیات کسب‌وکار ---
SEARCH=$(curl -s "$BASE/search_api.php?action=search&query=آرایشگاه")
check "جستجو کار می‌کند" "echo '$SEARCH' | grep -q '\"success\":true'"

DETAILS=$(curl -s "$BASE/search_api.php?action=business_details&id=1")
check "جزئیات کسب‌وکار + خدمات" "echo '$DETAILS' | grep -q 'کوتاهی مو'"

# --- 6) برنامهٔ کاری مالک → تاریخ → ساعت → رزرو (مسیر اصلی اپلیکیشن) ---
SCHED=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{
  "action":"update_schedule","business_id":1,"slot_duration_minutes":30,
  "schedules":[
    {"day_of_week":0,"start_time":"09:00","end_time":"17:00","is_active":true},
    {"day_of_week":1,"start_time":"09:00","end_time":"17:00","is_active":true},
    {"day_of_week":2,"start_time":"09:00","end_time":"17:00","is_active":true},
    {"day_of_week":3,"start_time":"09:00","end_time":"17:00","is_active":true},
    {"day_of_week":4,"start_time":"09:00","end_time":"17:00","is_active":true},
    {"day_of_week":5,"start_time":null,"end_time":null,"is_active":false},
    {"day_of_week":6,"start_time":"09:00","end_time":"17:00","is_active":true}
  ]}')
check "ذخیرهٔ برنامهٔ کاری هفتگی" "echo '$SCHED' | grep -q '\"status\":\"success\"'"

GS=$(curl -s "$BASE/api.php?action=get_schedule&business_id=1" "${AUTH[@]}")
check "خواندن برنامهٔ کاری (get_schedule)" "echo '$GS' | grep -q '\"status\":\"success\"'"
check "روزهای کاری در پاسخ get_schedule" "echo '$GS' | grep -q '\"day_of_week\"'"
check "ستون is_active در پاسخ get_schedule" "echo '$GS' | grep -q '\"is_active\"'"

DATE=$(curl -s "$BASE/api.php?action=get_available_dates&id=1")
check "تاریخ‌های آزاد (پاسخ موفق)" "echo '$DATE' | grep -q '\"status\":\"success\"'"
# رگرسیون: پاسخ success اما data خالی → مشتری هیچ روزی برای انتخاب نمی‌دید
NDATES=$(echo "$DATE" | json "len(d.get('data') or [])")
check "دست‌کم یک تاریخ قابل انتخاب است" "[ \"$NDATES\" -gt 0 ]"
BDAY=$(echo "$DATE" | json "(d['data'][0]['date'] if d.get('data') else '')")
COUNT0=$(echo "$DATE" | json "(d['data'][0].get('slot_count', 0) if d.get('data') else 0)")
check "تاریخ اول چند زمان آزاد دارد" "[ \"$COUNT0\" -gt 0 ]"
check "هر تاریخ jalali_date و day_name دارد" "echo '$DATE' | grep -q '\"jalali_date\"' && echo '$DATE' | grep -q '\"day_name\"'"

if [ -n "$BDAY" ]; then
  SLOTS=$(curl -s "$BASE/api.php?action=get_available_slots&id=1&date=$BDAY")
  check "اسلات‌های آزاد (پاسخ موفق)" "echo '$SLOTS' | grep -q '\"status\":\"success\"'"
  NSLOTS=$(echo "$SLOTS" | json "len(d.get('data') or [])")
  check "دست‌کم یک زمان آزاد در تاریخ انتخابی" "[ \"$NSLOTS\" -gt 0 ]"
  # هر ردیف باید هم شروع و هم پایان داشته باشد تا شبکهٔ زمان‌ها ناقص نماند
  STARTS=$(echo "$SLOTS" | grep -o '"start_time"' | wc -l)
  ENDS=$(echo "$SLOTS" | grep -o '"end_time"' | wc -l)
  check "همهٔ اسلات‌ها start_time و end_time دارند" "[ \"$STARTS\" -gt 0 ] && [ \"$STARTS\" = \"$ENDS\" ]"
  S0=$(echo "$SLOTS" | json "(d['data'][0]['start_time'] if d.get('data') else '')")
  E0=$(echo "$SLOTS" | json "(d['data'][0].get('end_time') or '' if d.get('data') else '')")

  BK=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{
    \"action\":\"create_booking\",\"business_id\":1,\"date\":\"$BDAY\",
    \"start_time\":\"$S0\",\"end_time\":\"$E0\",
    \"customer_name\":\"کاربر تست\",\"customer_phone\":\"09121111111\",\"customer_notes\":\"تست CI\"
  }")
  check "ایجاد رزرو روی زمان انتخابی" "echo '$BK' | grep -q '\"status\":\"success\"'"
  check "کد پیگیری رزرو برگردانده شد" "echo '$BK' | grep -q '\"confirmation_code\"'"
  BID=$(echo "$BK" | json "d.get('booking_id',0)")

  # رگرسیون: رزروهای گذشته نباید با خطای ساختاری پاسخ بدهند
  MYBK=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"get_user_bookings"}')
  check "لیست رزروهای کاربر" "echo '$MYBK' | grep -q '\"status\":\"success\"'"
  check "رزرو کاربر در پاسخ" "echo '$MYBK' | grep -q 'آرایشگاه'"

  if [ -n "$BID" ] && [ "$BID" != "0" ]; then
    CANCEL=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{\"action\":\"cancel_booking\",\"booking_id\":$BID}")
    check "لغو رزرو" "echo '$CANCEL' | grep -q '\"status\":\"success\"' || echo '$CANCEL' | grep -q 'یک ساعت'"
  fi
fi

# --- 7) کسب‌وکارهای من و اشتراک ---
MYBIZ=$(curl -s "$BASE/api.php?action=get_my_businesses" "${AUTH[@]}")
check "get_my_businesses" "echo '$MYBIZ' | grep -q 'آرایشگاه تست'"

PLANS=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"get_subscription_plans"}')
check "پلن‌های اشتراک" "echo '$PLANS' | grep -q '\"status\":\"success\"'"

NOTIF=$(curl -s -X POST "$BASE/mobile_auth.php" "${AUTH[@]}" -d '{"action":"get_notifications"}')
check "اعلان‌ها" "echo '$NOTIF' | grep -q '\"status\":\"success\"'"

echo "----------------------------------------"
echo "PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
