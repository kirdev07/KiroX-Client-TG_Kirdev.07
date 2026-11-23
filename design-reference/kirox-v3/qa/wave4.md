# Wave 4 — full check, code inspection, Android 7 + armeabi-v7a, test-server users

User decisions (2026-11-23):
- Bugs: fix them. Improvements that change design/UX: DO NOT apply — list them in `qa/ux-proposals.md` for the user to choose.
- Extra/unused files: DO NOT delete — list them in `qa/cleanup-candidates.md` with evidence.
- Android 7.0 (API 24) must run the app; no emulator; a real Android 7 phone comes later.
- TDLib: build only armeabi-v7a in addition to the existing arm64-v8a.
- Users that chat with each other: ONLY on Telegram's test servers (test DC) via a separate test build. Never the real Telegram.

## Global rules (every agent)
- Builds only via `python design-reference/kirox-v3/qa/kxbuild.py compile|test "<pattern>"|install|raw|rawfull` (queued, one Gradle at a time; never ./gradlew directly). `install` targets the Xiaomi 79ed61c9 and installs the v3preview demo app only.
- Phone: only the Xiaomi 79ed61c9 (ANDROID_SERIAL=79ed61c9). Never touch the Samsung R9WN31DDWMJ. Only packages com.kirox.app.v3 (demo) and, once it exists, the test-server build (com.kirox.app.testdc). NEVER open, screenshot, clear or uninstall com.kirox.app (the user's real account). Only qa-lead uses the phone (others ask qa-lead).
- No connectedAndroidTest. No git commits, no push (the lead commits). Never print secrets (kirox.properties, google-services.json, local.properties, keystore). Never sign in to any account and never type verification codes — the user does sign-ins.
- No new dangerous permissions without listing them in your report.
- Contract rules for ui/v3: additive KxStore members with defaults; no `x`+`setX` pairs; no non-local `return@key` in composables; demo behaviour only when `store is KxDemoStore`; destructive actions behind KxConfirm.
- Don't change the look of screens on your own (mockups in design-reference are the truth). Bugs = behaviour/crash/data/layout broken vs mockup.
- Write progress lines to `qa/status.md`: `- HH:MM <name>: …`. Findings go to `qa/wave4-bugs.md` (format: `- [ ] **sev** area — file:line — problem → fix — found by X` ; sev = high/medium/low). Fixers tick `[x]` with a note.

## Agents
- **architect** — owns build config, minSdk, ABIs, TDLib native build, test-server build type, performance.
- **inspector** — reads the old code (everything outside ui/v3) + v3 for bugs; fixes clear bugs in the old code; lists dead files.
- **ux** — usability review of every screen (old UI and v3) against Telegram conventions; writes proposals only.
- **qa-lead** — full functional/customization/icons check; only one using the phone; records bugs.
- **fixer** — fixes bugs from wave4-bugs.md in ui/v3 and where inspector didn't take them.
- (later) **user-sim-1..3** — chat as test users through the test build once the user has signed in test accounts.
