# kiroX v3 port — team board (overnight 2026-11-22 → 23)

Goal: the approved canvas mockups (`design-reference/kirox-v3/*.dc.html`, 222 boards, order in `canvas.json`)
become Kotlin + Jetpack Compose (+ Canvas animations), looking exactly like the mockups, then get wired to TDLib.

## Who owns what (never edit another owner's files)
| Owner | Files |
|---|---|
| Designer (main session) | `app/src/main/java/com/kirox/app/ui/v3/**` except `ui/v3/data/**`; `MainActivity.kt`, `app/build.gradle.kts`, `AndroidManifest.xml` |
| TDLib binder | `app/src/main/java/com/kirox/app/ui/v3/data/**` (new); may *read* everything; changes in `telegram/**` only when a missing call is needed, kept small and additive |
| UI tester | `app/src/test/java/com/kirox/app/v3/**` (screenshot tests), `design-reference/kirox-v3/qa/ui-*.md` |
| Telegram/TDLib tester | `app/src/test/java/com/kirox/app/v3data/**`, `design-reference/kirox-v3/qa/tg-*.md` |

Nobody commits: the main session commits each verified stage.

## Task files
- `qa/ui-tasks.md` — UI tester → designer. One `- [ ] <Screen>: <what is wrong> → <what to change>` per line. Designer ticks `[x]`.
- `qa/tg-tasks.md` — TG tester → binder (and designer when the UI model lacks a field). Same format.
- `qa/status.md` — each agent appends a short line per finished stage (time UTC, what, result).

## Rules for everyone
- Contract between UI and data: `ui/v3/model/KxModels.kt` + `KxStore.kt`. Need a new field/action? Write it in the task file; designer adds it.
- Build: `JAVA_HOME="$LOCALAPPDATA/Programs/Eclipse Adoptium/jdk-21.0.9.10-hotspot"`, `./gradlew ... --offline`. One Gradle at a time: if the build lock is busy, wait and retry.
- Devices: ONLY the Xiaomi `79ed61c9` (`adb -s 79ed61c9 ...`, adb at `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`). NEVER the Samsung `R9WN31DDWMJ`.
  Gradle installs to EVERY connected device unless told otherwise, and the Samsung is plugged in too: ALWAYS run installs as `ANDROID_SERIAL=79ed61c9 ./gradlew :app:installV3preview --offline` (or `adb -s 79ed61c9 install -r app/build/outputs/apk/v3preview/app-v3preview.apk`).
  On the Xiaomi install/test ONLY the demo app `com.kirox.app.v3`. Never open, uninstall, clear or screenshot the real `com.kirox.app` — it holds the owner's account.
- Never sign in to any real account, never send messages, never print secrets (kirox.properties, google-services.json, keystore).
- Texts in Russian as in the mockups; brand is written «kiroX»; no real personal names (demo names only).
- Tests: no connectedAndroidTest. Robolectric unit/screenshot tests (`./gradlew :app:testDebugUnitTest --tests ...`) are fine.

> **BUILD RULE (23:40 local): the PC ran out of memory with many parallel Gradle builds. From now on build ONLY via `python design-reference/kirox-v3/qa/kxbuild.py compile` (or `test "<pattern>"`, `install`). It queues builds one at a time. Never call ./gradlew directly. Compile less often: after every 3–4 boards.**
