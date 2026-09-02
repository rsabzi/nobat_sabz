# CI workflows

The two workflow files in this directory are ready-to-use GitHub Actions
workflows:

- `android-build.yml` — Gradle 8.7 + Temurin 17: unit tests, release APK build,
  emulator smoke test (api 30, `continue-on-error`), and commits the APK back
  to the branch as `release/sabztime-v1.0.0.apk`.
- `backend-test.yml` — MySQL 8 service, `php -l`, schema seed
  `create_tables.sql` + `database_init.php`, then `php -S 127.0.0.1:8090` plus
  `tests/api_smoke.sh` end-to-end API run.

They are intentionally kept outside `.github/workflows/` because the agent's
GitHub App token is not granted the `workflows` scope, and GitHub refuses any
ref that touches `.github/workflows/*` in that case (verified: the same 403 is
enforced through the Git Database API). Activate them with a token that has
`workflows: write`:

    mkdir -p .github/workflows
    cp ci/android-build.yml .github/workflows/android-build.yml
    cp ci/backend-test.yml .github/workflows/backend-test.yml
    git add .github/workflows && git commit -m "ci: activate workflows"
    git push

## 🚀 فعال‌سازی (ساده‌ترین راه — بدون توکن)

1. در GitHub به ریپو بروید: **Settings → Actions → General → Actions permissions** → **Allow all actions** → ذخیره کنید
2. در همان صفحه: **Settings → Actions → General → Workflow permissions** → **Read and write permissions** → ذخیره کنید
3. یا به سادگی: **Settings → Actions → Enable** (اگر دکمه فعال باشد)

سپس در تب **Actions**، workflow **Android Build & Test** را انتخاب و **Run workflow** (روی شاخه `arena/01a05c23-nobat-sabz`) بزنید.

## 🚀 فعال‌سازی (راه دوم — با PAT)

```bash
# 1) یک PAT با scope "workflow" بسازید
# 2) آن را به عنوان رمز ذخیره کنید، سپس:
git remote set-url origin https://ghp_XXX@github.com/rsabzi/nobat_sabz.git
mkdir -p .github/workflows
cp ci/android-build.yml .github/workflows/android-build.yml
cp ci/backend-test.yml .github/workflows/backend-test.yml
git add .github/workflows && git commit -m "ci: activate workflows"
git push origin arena/01a05c23-nobat-sabz
```

پس از activate، در تب **Actions** اجرای **Android Build & Test** را مشاهده می‌کنید (واحد تست → APK → شبیه‌ساز). خروجی نهایی APK در شاخه: **`android/release/sabztime-v1.0.0.apk`**
