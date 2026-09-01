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
