#!/usr/bin/env bash
# تست رگرسیون HTTP برای گارد «شناسه کسب‌وکار»
# باگ اصلی: اکشن‌هایی که به کسب‌وکار وابسته نیستند (get_my_businesses،
# cancel_booking، register_marketer و …) با خطای 400 «Business ID is required.»
# رد می‌شدند و صفحه «کسب‌وکارهای من» در اپ خطا می‌داد.
#
# پیش‌نیاز: سرور PHP روی 127.0.0.1:8090
set -euo pipefail

BASE="http://127.0.0.1:8090/webapp"
PASS=0; FAIL=0
BAD='Business ID is required'

check() {
  local name="$1" cond="$2"
  if eval "$cond"; then
    PASS=$((PASS+1)); echo "✅ $name"
  else
    FAIL=$((FAIL+1)); echo "❌ $name"
    [ -n "${GITHUB_ACTIONS:-}" ] && echo "::error::❌ $name"
  fi
}

PHONE="0912$(( RANDOM % 9000000 + 1000000 ))"
REG=$(curl -s -X POST "$BASE/mobile_auth.php" -H 'Content-Type: application/json' \
  -d "{\"action\":\"register\",\"phone\":\"$PHONE\",\"password\":\"secret123\",\"name\":\"کاربر گارد\",\"device\":\"ci\"}")
TOKEN=$(printf '%s' "$REG" | python3 -c "import json,sys; print(json.load(sys.stdin).get('token',''))" || true)
if [ -z "$TOKEN" ]; then
  echo "❌ ثبت‌نام برای تست گارد ناموفق بود: $REG"
  [ -n "${GITHUB_ACTIONS:-}" ] && echo "::error::ثبت‌نام برای تست گارد ناموفق بود: $REG"
  exit 1
fi
AUTH=(-H "X-App-Token: $TOKEN" -H 'Content-Type: application/json')

# --- GET بدون شناسه کسب‌وکار ---
MYBIZ=$(curl -s "$BASE/api.php?action=get_my_businesses" "${AUTH[@]}")
check "get_my_businesses بدون business_id رد نمی‌شود" "! echo '$MYBIZ' | grep -q '$BAD'"
check "get_my_businesses پاسخ success می‌دهد" "echo '$MYBIZ' | grep -q '\"status\":\"success\"'"

# --- POST بدون شناسه کسب‌وکار ---
for ACTION in register_marketer get_marketer_dashboard get_user_bookings get_subscription_plans; do
  RES=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d "{\"action\":\"$ACTION\"}")
  check "$ACTION بدون business_id رد نمی‌شود" "! echo '$RES' | grep -q '$BAD'"
  check "$ACTION خطای Access Denied نمی‌گیرد" "! echo '$RES' | grep -q 'Access Denied'"
done

CANCEL=$(curl -s -X POST "$BASE/api.php" "${AUTH[@]}" -d '{"action":"cancel_booking","booking_id":999999}')
check "cancel_booking فقط با booking_id رد نمی‌شود" "! echo '$CANCEL' | grep -q '$BAD'"

# --- برعکس: اکشن‌های وابسته به کسب‌وکار باید بدون شناسه رد شوند ---
NOID=$(curl -s "$BASE/api.php?action=get_services")
check "get_services بدون شناسه رد می‌شود" "echo '$NOID' | grep -q '$BAD'"
BADID=$(curl -s "$BASE/api.php?action=get_services&business_id=abc")
check "شناسه غیرعددی رد می‌شود" "echo '$BADID' | grep -q '$BAD'"

echo "----------------------------------------"
echo "PASS=$PASS FAIL=$FAIL"
[ -n "${GITHUB_ACTIONS:-}" ] && echo "::notice::تست رگرسیون گارد شناسه کسب‌وکار → PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
