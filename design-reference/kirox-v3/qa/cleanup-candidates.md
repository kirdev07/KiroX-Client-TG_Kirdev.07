# Cleanup candidates (wave 4, inspector)

Nothing here was deleted. Each line: what, size, evidence, suggestion. Evidence comes from a whole-repo
token scan of `app/src/**` (main + test + androidTest + qadc, TdApi.java excluded) plus `git ls-files` / grep.
A name counted once in the whole source tree = declared and never used.

Result in short: the old Kotlin code has **no unreferenced files** (every old file has at least one top-level
name used elsewhere); dead code is limited to single functions/values. The big weight is outside `app/`.

## A. Repository files (biggest first)

| # | What | Size | Evidence | Suggestion |
|---|------|------|----------|------------|
| A1 | `design-reference/fusion-v2/` (12 md drafts, preview.html, theme-tokens.json, check_approved_reference.py, 7 PNG in assets/) | 14 MB | The previous ("Fusion v2") design, superseded by kirox-v3 (memory: "redo design system → new design"). Only reference from code: a comment in `ui/theme/FusionTheme.kt:103`. No build/CI use. | Keep while the old UI ships; archive (separate branch/tag) once v3 replaces the old UI. |
| A2 | `design-reference/KIROX_*_SCREENS.png` (3), `KIROX_INTERACTIVE_MOCKUP.html`, `KIROX_UI_SPEC.md`, `CODEX_START_PROMPT.md` | ~0.9 MB | First-generation design brief/prompt for another tool; not referenced by code, CI or kirox-v3 docs (grep for the file names: only `CODEX_START_PROMPT.md` itself and `fusion-v2/APPROVED_CORE_VISUAL_EXACT.md`). | Archive with A1. |
| A3 | `voip/prebuild/include/wallet-engine/` | 680 KB | No reference in `voip/CMakeLists.txt`, `voip/voip/CMakeLists.txt` or workflows (grep `wallet-engine`: none). Looks copied along with the Telegram prebuilt tree. | Remove after a native build confirms nothing includes it. |
| A4 | `voip/prebuild/include/x86`, `x86_64` | tiny | `ndk.abiFilters` is arm64-v8a (+ armeabi-v7a coming); x86 folders are never selected by `prebuild/include/${ANDROID_ABI}`. | Remove (keep armeabi-v7a — architect is adding it). |
| A5 | `voip/tgnet/FileLog.h` (single header in its own folder) | 1 KB | Shim included by webrtc sources (`jvm_android.cc`, `video_stream_encoder.cc`). **Used** — listed only so nobody deletes it as "stray". | Keep. |
| A6 | `app/src/main/assets/licenses/Pacifico-OFL.txt` + Pacifico section in `THIRD_PARTY_NOTICES.md` / `README.md:62` | 4 KB | The font file it licenses, `app/src/main/res/font/pacifico_regular.ttf`, no longer exists (no `res/font` dir; grep `pacifico` in code: none). The notice now points at a missing file. | Drop asset + notice lines, or restore the font if it is still wanted. |
| A7 | `screenshots.request` (root) | 12 B | Only a trigger for `.github/workflows/screenshots.yml` (paths filter); content is a random number. Used. | Keep, or move under `.github/`. |
| A8 | `.kotlin/` (root, untracked) | build cache | Kotlin 2.x compiler session data; not in `.gitignore`, so it shows in every `git status`. | Add `.kotlin/` to `.gitignore`. |
| A9 | `docs/releases/1.1.0-draft.md`, `docs/audit/*`, `docs/design/fusion-final-report.md` | small | Historical; not referenced by code/CI. `fusion-final-report.md` belongs to A1. | Keep releases; archive design/audit with A1. |
| A10 | `app/src/androidTest/` | 9 KB | connectedAndroidTest is banned for the team (wave4 rules) and never run in CI (`android.yml` runs unit tests/lint). | Keep but mark as not run, or move the checks to Robolectric. |

## B. Old Kotlin code (outside ui/v3) — declared, never used

| # | Where | Evidence (whole-source token count = 1) | Note |
|---|-------|-----------------------------------------|------|
| B1 | `notify/KeepConnectedService.kt:92` `setMode()` (+ `KeepMode.On/Off`, the `kirox_push/keep_mode` pref) | `mode()` always returns `KeepMode.Auto` ("choice was removed from the settings"); setMode has no caller. | Remove setMode, collapse KeepMode to Auto-only. |
| B2 | `phone/PhoneEntryState.kt:81` `showExternalError()` | no caller | Remove. |
| B3 | `telegram/TelegramMessageRepository.kt:106` `stopPoll()` | no caller in old UI or v3 | Remove or wire to a «Остановить опрос» action (v3 has its own path). |
| B4 | `ui/components/FusionKit.kt:69` `FusionTextAction`, `:84` `FusionChip` | no caller | Remove. |
| B5 | `ui/theme/FusionTokens.kt:12,18,19,53,55` spacing `xxs`, `xxl`, `xxxl`, `iconSmall`, `listRow` | no reader | Remove tokens. |
| B6 | `ui/chat/ChatModels.kt:53` `hasAvatar`, `:68` `animationSpeed` | property never read | Remove. |
| — | `ui/chat/WebAppSheet.kt:61` `postEvent` | **false positive**: @JavascriptInterface called from JS. | Keep. |
| — | `TelegramSettingsRepository.kt:377` `PasswordState.toState`, `TelegramUserRepository.kt:222` `UserStatus.toStatus` | **false positives** (extension functions; the scan saw the receiver name). | Keep. |

Whole-file check (all top-level names of a file unused outside it): **0 files** in the old code.

## C. ui/v3 (light pass; owners: fixer/ui agents)

`components/KxBasics.kt:277 KxSectionTitle`, `screens/ChatsScreen.kt:252 SwipeToRead`, `screens/extra/ExtraKit.kt:176 ExHeader`, `:265 ExDialog`, `screens/extra/ExtraKit2.kt:54 ExDp0`, `screens/settings/SetCommon.kt:53 RiseEase`, `:95 SHint`, `:214 wBorder`, `screens/tools/ToolKit.kt:134 TDarkSwitch`, `theme/KxTheme.kt:93 nameBold`, `model/KxModels.kt:114 unreadSeparator`, `:118 minithumb`, `data/TelegramKxAccount.kt:129 ownsText`, `data/TelegramKxMedia.kt:113 sizeLine` — each declared once, never used (14). Check `unreadSeparator`/`minithumb` before removing: they may be planned contract members (KxStore additive rule).

## D. Resources

All drawables, mipmaps, colors, styles and xml in `app/src/main/res` are referenced (manifest aliases, adaptive icons, `CustomIcon.kt`, `PrivacySettings.kt`, `AuthKit.kt`, splash style). `assets/i18n/en.json` is read by `I18n.kt`. Nothing to remove there except A6.

Totals: 10 repo/file items (A1–A4, A6, A8 actionable), 6 old-code dead-code items (B1–B6, 13 declarations), 14 v3 declarations.
