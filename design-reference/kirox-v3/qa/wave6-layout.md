# Wave 6 — layout on other screen sizes (qa-layout)

Harness: `app/src/test/java/com/kirox/app/v3/V3SizesScreenshotTest.kt`
(`python design-reference/kirox-v3/qa/kxbuild.py test "com.kirox.app.v3.V3Sizes*"`).
Output: `app/build/screenshots-v3/sizes/<config>/<Screen>.png`

| config | size (dp) | what |
|---|---|---|
| p393x873_20x9_ref | 393×873 | reference look (common 20:9) |
| p360x640_16x9 | 360×640 | small 16:9 |
| p360x840_21x9 | 360×840 | tall 21:9 |
| p320x568_compact | 320×568 | compact 4.7" |
| p430x932_large | 430×932 | large phone |
| fs13_360x640 | 360×640, font 1.3 | big system font |
| t600x960_fold | 600×960 | foldable / tablet portrait (core screens only) |
| t960x600_land | 960×600 | landscape (core screens only) |

Core screens (13) × 8 configs + every KxRegistry board (217) × 6 phone configs.

## Fixed (look unchanged at 393×873)
- [x] Splash (`extra/SplashScreen.kt`): art + wordmark used absolute 230/500dp offsets → on ≤ 700dp (16:9, compact, landscape, font 1.3) the wordmark sat on «Подключаемся…» / the loading bar. Offsets and art now scale by k = (height−190)/580, clamped to 1 (board look from ~770dp up).
- [x] Message menu (`MessageMenu.kt`): fixed top 170dp → on 360×640 / 320×568 / landscape «Изменить / Выбрать / Удалить…» were off-screen. Top is now min(170, height−520) (≥ 56dp) and the stack scrolls; a tap on empty space still closes. Reaction cells shrink (≤ 40dp) so the ⌄ «Все реакции» button is not cut at 320dp.
- [x] Profile (`ProfileScreen.kt`): action tile labels («Без звука» → «Без») and the «Подарки · 3» / «Общие группы» chips (wrapped into 3 lines inside a fixed 48dp chip) — single line with auto-shrink.
- [x] Contacts (`core/ContactsScreen.kt`): title «Контак/ты» on 320dp, «Пригласит/ь» at font 1.3 — single line with auto-shrink.
- [x] MyQr (`settings/MyQrScreen.kt`): QR card ran under the theme strip and buttons from 360×640 down (name/caption hidden). Card scales down from its top when the height is short (k = 1 on the reference).
- [x] Birthdays (`chat/BirthdaysScreen.kt`): «Поздрав/ить», «Позвони/ть», «Дмитри/й» wrapping at 320dp / font 1.3 — single line, auto-shrink / ellipsis.
- [x] Downloads (`media/DownloadsScreen.kt`): title «Загрузк/и» on 320dp — one line.

## Filed (screen owned by another porter right now — please fix there)
- [ ] **Chats header** (ChatsScreen, nav/Chats tabs) — 320×568: kiroX + «2 истории» + search + compose don't fit; search circle slides under the compose button, compose touches the right edge. `sizes/p320x568_compact/Chats.png`. Suggest: hide «N истории» text (keep avatars) below ~340dp, or weight(1f, fill=false) + ellipsis on it.
- [ ] **Settings** (port-settings) — 320×568 and font 1.3: «Подарк/и / За / звёзды» wraps in the gifts tile, «Изм…» profile button, «Кастомиза/ция» wraps in the row title. `sizes/p320x568_compact/Settings.png`, `sizes/fs13_360x640/Settings.png`. Suggest maxLines=1 + autoSize/ellipsis in the tiles and row titles. Notify header «Уведомле…» also cut by the «Проверка» chip: `sizes/p320x568_compact/Notify.png`.
- [ ] **Code** (onboarding, `core/AuthScreens.kt`) — 320×568: 5th code box cut off (5 × 54dp + gaps > 272dp), and «Продолжить» is squeezed to a sliver at the bottom (column with Spacer(weight) is not scrollable). `sizes/p320x568_compact/Code.png`. Suggest boxes `weight(1f).widthIn(max = 54.dp)` + `aspectRatio`, and verticalScroll + heightIn(min = maxHeight) like LoginScreen.
- [ ] **Login** (onboarding) — the chat-bubble art (TopEnd offsets) covers «С возвращением» from 360dp down; worst on 320×568 / landscape. `sizes/p320x568_compact/Login.png`, `sizes/t960x600_land/Login.png`. Suggest: scale/move art with width, or put the title under the art.
- [ ] **Welcome** (onboarding) — 320×568: «Начать» button sits on the «kiroX» title. `sizes/p320x568_compact/Welcome.png`.
- [ ] **Lock / Passcode keypad** (port-security) — 320×568: keypad rows 1–3 overlap the lock icon and «Введите код-пароль». `sizes/p320x568_compact/Lock.png`. Hidden chats keypad: bottom row «0» cut — `sizes/p320x568_compact/Hidden.png`. Password: hint/«Забыли пароль?» at the bottom edge — `sizes/p320x568_compact/Password.png`. SecurityCheck: «Почта для восстановлен/ия» wraps — `sizes/p320x568_compact/SecurityCheck.png`.
- [ ] **Incoming call** (port-calls) — 320×568: «Ответить текстом / Напомнить» buttons overlap the caller name; swipe pill covers «Входящий звонок». `sizes/p320x568_compact/Incoming.png`.
- [ ] **Call controls rows** (port-calls) — 320dp: last button (red end call) cut at the right in GroupCall and VideoCall. `sizes/p320x568_compact/GroupCall.png`, `sizes/p320x568_compact/VideoCall.png`.
- [ ] **KeyCheck** (port-calls) — 320×568: 4th emoji cut, steps list pushed off-screen (not scrollable), «Не совпадают» → «Не». `sizes/p320x568_compact/KeyCheck.png`, `sizes/fs13_360x640/KeyCheck.png`.
- [ ] **Wide screens** (nav / KxApp root) — 960×600 landscape and 600dp foldable: every screen stretches edge to edge (chat rows with time 1500px away from the name, Settings tiles, Profile actions); MsgMenu in landscape was cut (fixed by scroll). Orientation is not locked in the manifest. Suggest one change at the app root: content `widthIn(max = 600.dp)` centered with Kx.bg filling the sides when width ≥ 700dp. `sizes/t960x600_land/*.png`.

## Font 1.3 (fs13_360x640) — extra findings
- Lock keypad overlaps «Введите код-пароль» / «Макет: код 1234» already at 360×640 with font 1.3 (port-security): `sizes/fs13_360x640/Lock.png`.
- LinkWarn: «Всё равно открыть» button text clipped in a fixed-height button: `sizes/fs13_360x640/LinkWarn.png`.
- Agenda: time column «19:0/0» wraps: `sizes/fs13_360x640/Agenda.png`. AutoDelete: «Выключен/о» chip wraps, the timer ring overlaps the sample bubbles: `sizes/fs13_360x640/AutoDelete.png`.
- General pattern: buttons/chips with fixed `height(..)` + text without `maxLines` wrap and clip at 1.3. Fix recipe used above: `maxLines = 1` + `autoSize = TextAutoSize.StepBased(minFontSize, maxFontSize = <board size>)` — identical on the reference.

## Minor (not fixed — nice to have)
- Attach sheet: the «Нет доступа к галерее…» tile is a fixed 216dp item; on 320dp the text runs to the row edge (`sizes/p320x568_compact/Attach.png`).
- Single-word wraps at 320dp in headers/chips: ChatSearch «Списко/м», StickerManage title on 2 lines, QuickReplies «Быстрые» cut by chips, Invite «Подели…», Memories «К/переписке», MeetTime «Назначить на сб 28,» cut, DeleteAccount TTL chip «6».
