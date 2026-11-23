# kiroX 1.0 — release plan (release-engineer, 2026-11-23)

Target: **1.0** by 2026-11-25, APK posted to the owner's Telegram channel. v3 UI is the main UI (hybrid: functions without a v3 board open the old screens — bridge by fixer-settings), Russian only.

## 1. Flip patch (ready, NOT applied)

File: `design-reference/kirox-v3/qa/release-1.0.patch` — regenerated against the working tree of 2026-11-23 14:25 (minSdk 29, arm64-only, qarelease build type); `git apply --check` passes. Not compile-checked (applying it in the shared tree would hit other agents) — compile right after applying.
Apply at the end: `git apply -p1 design-reference/kirox-v3/qa/release-1.0.patch` → `kxbuild.py compile` → `kxbuild.py test "com.kirox.app.*"` → release build.
If a hunk no longer applies (fixers still touch KxApp.kt / SettingsScreen.kt), make the same change by hand from this list:

| # | File | Change |
|---|------|--------|
| 1 | `app/build.gradle.kts` | `versionCode = 8 → 9`, `versionName = "1.0.0-rc.1" → "1.0"` (updater: `AppVersion.compare("1.0", "1.0.0-rc.1") > 0` ✔ — a release beats its rc) |
| 2 | `ui/v3/KxNewDesign.kt` | Always on: `enabled()` returns a constant `true` flow; `set()` removed; old pref `new_design_v3=false` from rc builds is ignored (phones that switched back get v3 too) |
| 3 | `ui/home/AppearanceScreens.kt` | Remove the «Новый дизайн» row (+ its divider) from old «Оформление» |
| 4 | `ui/v3/screens/SettingsScreen.kt` | Remove «Старый дизайн» from the Settings ⋮ menu |
| 5 | `ui/v3/KxApp.kt` | Settings `onOpen` no longer special-cases «Старый дизайн» (`KxNewDesign.set` is gone) |
| 6 | `ui/v3/screens/chat/CoachScreen.kt` | «Макет: показать снова» only when `ctx.store is KxDemoStore` (was visible on the real account) |
| 7 | `ui/v3/screens/tools/NightModeScreen.kt` | slider caption «Макет: время суток» → «Проверка по времени» on the real store |
| 8 | `ui/v3/screens/extra/WhatsNewScreen.kt` | Real store shows the real 1.0 changelog (`WnLogRelease`); the fake history (1.0.0-rc.1 / 0.9.5 «Игры с кубиком» / 0.9.0) stays for the demo only |
| 9 | `ui/home/WhatsNew.kt` | `CHANGES` = 1.0 list (this old sheet still pops once after the update from rc.1, on top of the v3 UI — KiroXApp.kt:~468) |

Not in the patch (lead decides):
- `KiroXApp.kt` still has the `newDesign == true` branches; with #2 the old `TelegramMainScreen` path becomes unreachable after sign-in (kept — the hybrid bridge may still use old screens; dead-code cleanup after 1.0).
- Demo-only controls audit («Макет: …»): all others are already gated (`!live`, `demo`, `chatId == null`, or the state is only reachable on the demo store): ChannelScreen:203, JoinQueueScreen:110, JoinRequestScreen:177, MsgInfoScreen:153, SendOptionsScreen:89, LockScreen:108, PasswordScreen:125, PowerScreen:125, LoginEmailScreen:164, TermsUpdateScreen:135, RateAppScreen:120 (done only on demo), ShareInScreen:161 (done only on demo).

### Release flags (checked, nothing to change)
- Test DC: `TELEGRAM_USE_TEST_DC=false` in defaultConfig; only `qadc` sets true; TDLib dir `tdlib/` vs `tdlib-testdc/`. ✔
- Debug logs: `Log.v/d` stripped by R8 (`-assumenosideeffects`), the remaining `Log.d` calls are also behind `BuildConfig.DEBUG`; TDLib `SetLogVerbosityLevel(1)` (errors only). ✔ (optional: add `i(...)` to the assumenosideeffects block)
- StrictMode: not used anywhere in app/src. ✔
- `debuggable` false for release (AGP default), `allowBackup="false"`, FileProvider authority `${applicationId}.files`. ✔
- `callsEnabled = mediaAvailable || DEBUG` — release depends on the engine only. ✔
- Demo app: `KX_V3_DEMO=false` outside `v3preview`. ✔
- Signing: `kirox.properties` has all four `KIROX_RELEASE_*` keys (values not read/printed); release APK is signed by the release config.

## 2. R8 / minify — see «R8 results» below

## 3. ABIs — owner decision 14:20: 1.0 = arm64-v8a only, Android 10–17 (minSdk 29, targetSdk 37)
The lead removed `jniLibs/armeabi-v7a/libtdjni.so` and set minSdk 29; native's v7a/kiroxopus work is reverted (voip/CMakeLists.txt = HEAD). Verify before the release build:
1. `aapt2 dump badging app-release.apk` → `minSdkVersion:'29'`, `native-code: 'arm64-v8a'` only.
2. APK `lib/` = only `lib/arm64-v8a/…` (libtdjni, libkiroxvoip + 4 small AndroidX libs).
3. TDLib CI commit 05a87c6 (TDLib 4c6377aa rebuilt at API 24) stays for arm64: TdApi.java unchanged by it (same TDLib commit) — only the .so was rebuilt; qarelease smoke below covers it loading on the Xiaomi.
4. If the TDLib workflow runs again it will re-add `armeabi-v7a/libtdjni.so` (`.github/workflows/tdlib.yml` builds "arm64-v8a armeabi-v7a") and gradle would pick it up only if the `if (file(...v7a...).exists())` line is still there — after 1.0 either drop v7a from the workflow or delete the .so again after each run.
5. One APK per release. The in-app updater (`update/UpdateCenter.kt:162`) takes the **first `.apk` asset** of the GitHub release without an ABI check — fine for one arm64 APK; never upload per-ABI splits.
6. Updater channel: it reads **GitHub releases** (`api.github.com/repos/…/releases`), not the Telegram channel. To let rc users update in-app, also publish GitHub release `v1.0` (not pre-release) with the same APK; otherwise they only update from the channel post. Version check: `"1.0"` > `"1.0.0-rc.1"` ✔.
7. Phones on Android 7–9 that installed rc builds (minSdk 24) cannot install 1.0 (INSTALL_FAILED_OLDER_SDK) — say so in the post if rc testers had such phones.
## 4. Release texts (RU) — see below

## 5. Checklist for the lead (release day)
- [ ] fixer-settings bridge merged; `kxbuild.py compile` green
- [ ] section 3 checks (arm64-only, minSdk 29)
- [ ] `git apply -p1 design-reference/kirox-v3/qa/release-1.0.patch` (or the table by hand); compile + `test "com.kirox.app.*"` green
- [ ] `kxbuild.py installrel` → qarelease smoke on the Xiaomi: launch, login screen, no FATAL/ClassNotFound/NoSuchMethod in logcat
- [ ] `kxbuild.py rawfull :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (signed with the release key; check `apksigner verify --print-certs` shows the same cert as rc.1)
- [ ] keep `app/build/outputs/mapping/release/mapping.txt` next to the APK (CrashLog stack traces are obfuscated) — copy to `kirox-releases/1.0/`
- [ ] rename `kiroX-1.0.apk`, sha256 → `kiroX-1.0.apk.sha256`
- [ ] GitHub release `v1.0` (APK + .sha256, notes = changelog below) so the in-app updater sees it
- [ ] channel post (below) with the APK
- [ ] after release: bump to `1.0.1-dev`/versionCode 10 on the branch

## Release texts (RU, gender-neutral, brand «kiroX»)

### Changelog — WhatsNew board / updater / GitHub release `v1.0`
**kiroX 1.0 — первый выпуск**

- **Новый дизайн.** Тёмный интерфейс с мягким зелёным: чаты, звонки, истории и настройки нарисованы заново.
- **Темы и вид чатов.** Готовые темы, конструктор из одного цвета, свои обои для любимых чатов, ночной режим по расписанию или по солнцу.
- **Файлы и музыка.** Всё скачанное в одном месте; музыка из чатов с плеером, очередью и таймером сна.
- **Проверка файлов.** Перед открытием kiroX ищет замаскированные программы, макросы и опасные архивы.
- **Защита.** Код-пароль, скрытые чаты, управление сеансами и устройствами.
- **Голосовые и звонки.** Голосовые сообщения, кружки и звонки со сквозным шифрованием прямо из чата.
- Часть редких настроек пока открывается в прежнем оформлении — их перенесём в следующих версиях.

Требования: Android 10 и новее, 64-битные телефоны. Интерфейс пока только на русском.

### Channel post (owner's Telegram channel)
**Вышел kiroX 1.0** 🎉

Первая стабильная версия — с новым дизайном: тёмный интерфейс с мягким зелёным, свои темы и обои для каждого чата, ночной режим, файлы и музыка в одном месте, проверка файлов перед открытием, код-пароль и скрытые чаты.

📱 Нужен Android 10 или новее и 64-битный телефон (почти все телефоны с 2019 года).
Важно знать:
• приложение пока только на русском;
• несколько редких настроек пока открываются в прежнем оформлении — перенесём их в следующих версиях.

Установка: скачайте APK ниже и откройте его. Если уже стоит kiroX, он обновится, чаты и вход сохранятся.
Нашли ошибку — напишите в комментариях или через «Помощь» в приложении.
