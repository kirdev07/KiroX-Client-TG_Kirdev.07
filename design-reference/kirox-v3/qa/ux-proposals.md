# kiroX v3 — UX proposals (wave 4)

Author: ux agent, 2026-11-23. **Proposals only — nothing here is applied.** The user picks.
Pure bugs (dead links, unreachable boards, overlap) are in `wave4-bugs.md` → section «ux».

Material: v3 boards (screens/*/Registry.kt, 212 keys), screenshots `app/build/screenshots-v3/*.png` (224, built today),
mockups `design-reference/kirox-v3/*.dc.html`, old UI `ui/home/*`. Reference = Telegram Android 11.x conventions.

Effort: **S** < half a day · **M** 1–2 days · **L** several days / needs a mockup round first.

Summary: **P1 — 12**, **P2 — 17**, **P3 — 9** (38 total).

---

## P1 — hurts users now

### P1-1 Entry-point map for the ~100 boards nobody can open — M (plan) + L (doing it)
- **Screen:** whole app (see the list in wave4-bugs «v3 reachability»).
- **Problem:** almost half of the ported boards are only reachable from the test harness. A user cannot find gesture settings, sounds, DND, traffic, cleanup, accessibility, night mode, login email, security check, quick replies / away / hours, story archive, hidden chats, etc.
- **Proposal:** agree one parent per board before wiring. Suggested map (Telegram-like):
  - *Уведомления и звуки* → Sounds, Dnd, InAppBanner preview.
  - *Конфиденциальность* → SecurityCheck (tap on the «Аккаунт хорошо защищён» card), LoginEmail + LoginAlert + KeyCheck (inside TwoStep), DeleteAccount («Если я не захожу…» row at the bottom, like Telegram), Hidden.
  - *Данные и память* → Traffic, Cleanup, Downloads, LowSpace, NetCheck, Offline, Proxy (Telegram keeps Proxy here).
  - *Кастомизация* → NightMode, Bubbles, Wallpaper, ChatGestures + Swipe (one «Жесты» row), Anim, Access («Специальные возможности»), Widgets, Shortcuts.
  - *Функции kiroX* → Power, QuickReplies, Away, Hours, Translate defaults, Digest, Wellbeing, Import/Export.
  - *Профиль / EditProfile* → NameColor, Status, Username, Birthdays.
  - *Истории* header ⋮ → StoryArchive, HiddenStories.
  - *Group ⋮ / Управление* → Requests, JoinRequest, Boosts.
  - Boards that are really event states (Incoming, Update, RateApp, Coach, Limited, CallWeak, CallWaiting, Welcome, Splash) — decide which ones get a trigger; the rest go to cleanup-candidates.
- **Note:** every board with no parent after this decision should be removed from the menu-of-record, not kept as dead weight.

### P1-2 Settings search covers 10 items only — S
- **Screen:** SettingsSearch (tools/SettingsSearchScreen.kt:48-57).
- **Problem:** the index has ~10 hard-coded entries; for many boards (Access, NightMode, Sounds) search is the *only* way in, and most settings («Время захода», «Автоудаление», «Папки», «Прокси», «Иконка») can't be found. Telegram's settings search indexes every row.
- **Proposal:** generate the index from one table of (title, synonyms, path, board) shared with the P1-1 map; include synonyms (тема/цвет/ночь, звук/мелодия, пароль/код).

### P1-3 Chat row: status icons are silent for TalkBack — S
- **Screen:** Chats list (ChatsScreen.kt ChatRow ~200-225), Archive.
- **Problem:** pinned, muted, verified, secret, read/sent/sending/**failed** icons all have `contentDescription = null`; TalkBack reads «Анна, печатает…, 21:58, 2» and never says «не отправлено» or «без звука».
- **Proposal:** build one merged description per row: «Анна. Закреплён. Без звука. 2 непрочитанных. Последнее: …, 21:58, прочитано».

### P1-4 Swipe / long-press actions have no accessible alternative — M
- **Screen:** chat list (ChatSwipe, SwipeToRead), message bubbles (long-press menu, double-tap), story viewer.
- **Problem:** 0 uses of `customActions` in v3. TalkBack and switch-access users can't archive, pin, mark read or reply without gestures.
- **Proposal:** add `semantics { customActions = … }` on ChatRow (Прочитано, Закрепить, Архивировать, Без звука, Удалить) and on MessageBubble (Ответить, Реакция, Меню).

### P1-5 Touch targets under 48 dp — S
- **Screen:** global. `KxIconButton` defaults to 44 dp, the tab headers use 42 dp (Calls, Stories, Settings search/⋮), close «×» buttons are 28–34 dp (ArchiveScreen.kt:86, CommentsScreen.kt:210, QuoteScreen.kt:147, ChatSearchScreen.kt:115, AgendaScreen.kt:124, LiveBarsScreen.kt:106).
- **Proposal:** keep the drawn size but add `Modifier.minimumInteractiveComponentSize()` (or a 48 dp hit box) inside `kxClick` for icon-only targets. No visual change.

### P1-6 Switches don't announce on/off — S
- **Screen:** 8 files draw `KxSwitch` without toggleable semantics (ChatKitB, Poll, Drive, Hidden, QuickReact, Invite, NewGroup, Browser); KxSwitch itself has none.
- **Proposal:** give the row `Modifier.toggleable(value, role = Role.Switch)` (or put it into KxSwitch via an optional `onChange`), so TalkBack says «Вкл/Выкл».

### P1-7 Fixed-height rows clip at large font sizes — M
- **Screen:** settings rows (`height(52/58.dp)`), dock, chips, action tiles (Profile actions «Без звука», Group «Управление» are already at the edge at 100 %).
- **Problem:** with system font 130 %+ and the in-app Access scale, two-line labels are cut; profile action labels truncate.
- **Proposal:** switch to `heightIn(min = …)`, allow 2 lines for action-tile labels, test boards at fontScale 1.3 and 1.6 in the screenshot harness (add a variant to kxbuild test).

### P1-8 Rows that look like navigation but toggle in place — S
- **Screen:** Функции kiroX — «Двойной тап по сообщению», «Спрашивать перед отправкой», «Энергосбережение» (FeaturesScreen.kt:102-109).
- **Problem:** chevron «›» promises a new screen; a tap silently cycles to the next value. Users can't see the options and overshoot.
- **Proposal:** open a small bottom-sheet picker (radio list) — Telegram does the same for «Двойное нажатие». Energy saving should open the Power board.

### P1-9 Connection status is missing from the chat list — S
- **Screen:** Chats header (ChatsScreen.kt; `store.connection` exists in KxStore).
- **Problem:** Telegram users rely on «Соединение…» / «Обновление…» / «Ожидание сети…» in the title. In v3 the header always says kiroX, so a dead network looks like «nobody writes».
- **Proposal:** replace «kiroX» with the status text (or a small pill under it) while `connection != null`.

### P1-10 Empty states for chat folders and search — S
- **Screen:** Chats (empty folder, empty «Непрочитанные»), Archive empty, Calls «Пропущенные» empty, Contacts empty.
- **Problem:** only 14 boards have an empty state; an empty folder is a black screen above the dock.
- **Proposal:** one shared `KxEmpty` variant with an icon, one line and an action («Изменить папку», «Начать чат», «Пригласить друзей»).

### P1-11 Profile and group action rows disagree — S
- **Screen:** Profile (Звонок · Видео · **Без звука** · Поиск · Подарок) vs Group (**Звук** · Поиск · Видеочат · Управление).
- **Problem:** the mute action is labelled by its *result* in one place and by its *topic* in the other; Telegram labels it «Звук»/«Без звука» by current state consistently. «Видео» vs «Видеочат» also mean different things (1:1 call vs group voice chat) but look the same.
- **Proposal:** one rule: label = action that will happen («Выключить звук» → short «Без звука», toggles to «Со звуком»). Keep «Видеочат» only for groups, add the state to TalkBack.

### P1-12 Switching back to the old design is one tap with no warning — S
- **Screen:** Settings ⋮ → «Старый дизайн» (SettingsScreen.kt menu).
- **Problem:** item sits between «Устройства» and «Чаты и папки» and switches the whole app immediately; no explanation that it can be undone (the old UI says «вернуть можно в его настройках», v3 doesn't).
- **Proposal:** KxConfirm «Вернуть старый дизайн? Новый можно включить снова: Настройки → Оформление → Новый дизайн», and move the item to the bottom of Customization.

---

## P2 — nice to have

### P2-1 One-handed reach: primary actions live at the top — M
- **Screen:** Chats (compose pencil top-right), Calls (new call top-right), Stories (camera top-right).
- **Problem:** on 6.5″+ phones the most-used action is in the hardest corner; Telegram Android puts the compose FAB bottom-right above the nav.
- **Proposal:** optional «Кнопки внизу» toggle in Customization that moves the accent button to a FAB above the dock (right side); also pull-down on the list to open search (Telegram has swipe-down search in some clients).

### P2-2 Settings main list hides common destinations in ⋮ — S
- **Screen:** Settings tab.
- **Problem:** Устройства, Папки, Контакты are only in the ⋮ menu; Telegram lists «Папки с чатами» and «Устройства» as main rows. Discoverability for folders is low (and folders drive the chips on the Chats screen).
- **Proposal:** add «Папки» and «Устройства» to the main group (7 rows instead of 5), keep ⋮ for rarely used items.

### P2-3 Folder chips: «Работа» is cut and there's no edit entry — S
- **Screen:** Chats chips row.
- **Problem:** the last chip is clipped at the edge without a fade, nothing hints it scrolls; long-press on a chip (Telegram: «Изменить папку / Изменить порядок») — check, not found in code.
- **Proposal:** add a right fade + long-press menu on chips (Изменить, Отметить прочитанным, Порядок папок → Folders).

### P2-4 Gender-neutral «был(а)» → state wording — S
- **Screen:** everywhere last-seen is shown (data/TelegramKx*.kt ~144-158, Members, NewMsg, Seen: «прочитал(а)», service messages «закрепил(а)», Logout checklist «Я сохранил(а) нужные файлы»).
- **Problem:** «(а)» is how Telegram RU does it, but it reads clumsy in big lists and the app otherwise speaks well.
- **Proposal:** «в сети 5 мин назад», «в сети недавно», «в сети вчера в 21:14»; «Прочитано в 19:30»; «Не прочитано»; service: «Сообщение закреплено · Анна»; checklist «Нужные файлы сохранены». Keep «(а)» only where a verb with a subject can't be avoided.

### P2-5 «С возвращением» is shown to first-time users — S
- **Screen:** Login (AuthScreens.kt:117).
- **Problem:** a person who never used kiroX sees «С возвращением»; no first-run explanation that kiroX is a Telegram client, what data is stored, or what's different. Welcome/Splash boards exist but are never shown.
- **Proposal:** first launch → Welcome (2–3 short cards: «Это Telegram», «Своё оформление», «Функции kiroX») → Login with «Вход в Telegram»; «С возвращением» only when a previous account is known.

### P2-6 Login: contact-sync checkbox defaults to ON — S
- **Screen:** Login.
- **Problem:** consent is pre-checked; Telegram asks separately with a system dialog after login.
- **Proposal:** default OFF, or move it to ContactSync after sign-in with an explanation.

### P2-7 Customization preview vs. effect — S
- **Screen:** Custom, ChatStyle.
- **Problem:** ChatStyle text-size control is 4 segments with labels only at the ends; current value «Обычный» is far right and not tied to the slider; no live dock/list preview for «Плотность».
- **Proposal:** label each step (S/M/L/XL or 14/16/18/20), show a small chat-list row preview under «Плотность».

### P2-8 Color palette: 12 swatches without names — S
- **Screen:** Custom «Цвет».
- **Problem:** two greens look alike (1st and 11th); names appear only for the selected one; TalkBack reads nothing meaningful.
- **Proposal:** contentDescription with the color name per swatch, label under selected swatch, make the 11th visibly different or name it («Мята» / «Лес»).

### P2-9 «Видеочат» in the chat ⋮ opens PlanCall — S
- **Screen:** ChatMenu.kt:58.
- **Problem:** the label promises starting a video chat now; the board is «запланировать звонок».
- **Proposal:** rename to «Запланировать видеочат» or split: «Начать видеочат» (GroupCall) + «Запланировать».

### P2-10 Message menu: «Удалить…» and «Изменить» positions — S
- **Screen:** MsgMenu.
- **Problem:** for own messages Telegram puts «Изменить» near the top (right after Ответить/Копировать); in v3 it is 7th, below Переслать/В Избранное/Закрепить, so the most frequent own-message action needs a long reach to the bottom of a 9-item menu.
- **Proposal:** own message: Ответить, Изменить, Копировать, Перевести, Переслать, Закрепить, В Избранное, Выбрать, Удалить (verify against current Telegram Android before changing); keep «Удалить» last.

### P2-11 Chat header: no visible video-call / search — S
- **Screen:** Chat.
- **Problem:** only the voice call is in the header; video call and in-chat search are 2 taps deep in ⋮ (Telegram shows a video icon for 1:1 when available and search in ⋮ too — so mostly OK). The big round call button is 56 dp next to a 24 dp ⋮ – visually unbalanced, and makes it easy to start a call by accident.
- **Proposal:** smaller call button (44 dp glass like others) + confirm call on first use («Позвонить Анне?», Telegram has that option).

### P2-12 Round-video timestamp floats outside the bubble — S
- **Screen:** Chat (video note at 19:13 placed far from the circle).
- **Proposal:** anchor time to the circle's bottom-right as in Telegram.

### P2-13 Settings «Подарки · За звёзды» tile vs Premium — S
- **Screen:** Settings.
- **Problem:** two big monetisation tiles sit above Notifications/Privacy; the essentials are pushed down past the fold on small phones.
- **Proposal:** move Premium/Gifts under the main rows or make them one compact row (Telegram puts Premium in the lower group).

### P2-14 Privacy: missing Telegram rows — M
- **Screen:** Privacy.
- **Problem:** no «Автоудаление сообщений» (global), «О себе», «День рождения», «Подарки», «Голосовые сообщения», «Удаление аккаунта», «Пароли-ключи»; users looking for them will assume kiroX lacks them.
- **Proposal:** add the rows (most map to existing TDLib privacy keys and existing boards AutoDelete/DeleteAccount).

### P2-15 Contrast of secondary labels on tinted tiles — S
- **Screen:** ChatStyle wallpaper captions (Узор/Градиент/Ночь/Нет ~#6x grey on black), Group «был(а)», disabled toggles.
- **Problem:** `text4` #8A958F on #0A0C0B is fine (~6:1), but captions drawn with alpha .5–.6 on top of gradients drop below 4.5:1.
- **Proposal:** never apply alpha to text below `text4`; run a contrast check on screenshots.

### P2-16 Logout: the alternatives board is never used — S
- **Screen:** Accounts «Выйти из …» goes straight to a confirm dialog; the Logout board (alternatives: add account, set passcode, clear cache, «Я сохранил…») exists but is unreachable.
- **Proposal:** Accounts → Logout board → confirm, like Telegram's «Выход» screen. Reduces accidental logouts.

### P2-17 Calls row: tap target ambiguity — S
- **Screen:** Calls.
- **Problem:** whole row and the round button both call; Telegram: row → call info (duration, rating, «Написать»), button → call again. Accidental calls are easy.
- **Proposal:** row opens call info sheet; only the button calls.

---

## P3 — ideas

### P3-1 «Что нового» coach-marks for kiroX-only features — M
Point once at swipe actions, ghost mode, double-tap; reuse the Coach board as a 3-step tour after first login.

### P3-2 Customization presets — M
«Как Telegram», «Компактно», «Крупно» presets in Custom that set bubbles + density + text size at once (helps users who don't want to tune 6 controls).

### P3-3 Reachability-friendly dock — S
Option to hide labels in the dock and raise it 8 dp less, giving more list space; or let long-press on a tab open its quick actions (Chats → new message, Calls → new call).

### P3-4 Per-chat look entry from the chat itself — S
ChatTheme is in the chat ⋮ only for private chats; groups and channels also have wallpapers. Show «Тема чата» for all kinds where TDLib allows.

### P3-5 Settings search: recent + suggestions — S
Show «Часто ищут» (Тема, Уведомления, Пароль) before typing; voice input.

### P3-6 Ghost mode as a quick toggle — S
Add a Quick Settings tile / long-press on the Settings tab to toggle «Режим призрака» — it's the headline kiroX feature but 2 levels deep.

### P3-7 Haptics consistency — S
Same light haptic for long-press menu, swipe threshold, toggle; none on plain navigation.

### P3-8 Story card density — S
Stories tab shows 6 cards per screen; a list mode (Telegram-style rows with ring) fits 10+ and makes «hide/mute story» swipe-able.

### P3-9 Screenshot harness variants — S
Add fontScale 1.3, light-contrast and TalkBack-tree dumps to kxbuild test so every new board is checked for P1-5..P1-7 automatically.

## from qa-lead
- Apply the colour theme (Custom board) to the whole v3 UI: today `Kx.accent` is a constant, the 12 swatches only change the board preview. Needs `Kx` colours from a CompositionLocal (accent, accentGradient, bubble out colours, badges). Same for NightMode, Access (bold / contrast / outlines / big buttons / palettes) and Language — either apply or label the rows «скоро».
- Search screens: focus the field and open the keyboard on entry (global Search, Settings search) — Telegram does this; today the user must tap the field first.
- Calls tab «Поиск» opens the global chat search; Telegram searches the call log there → a call-log filter.
- «Специальные возможности» (Access) is reachable only via Settings search → add a row (Settings or Custom).
- Story viewer reply pill: «Ответить Анна…» → «Ответить…» (Russian case), and make it a real input (replyStory).
- Swipe action labels/icons in the chat list stay «Закрепить»/«Без звука» for an already pinned/muted chat → show «Открепить»/«Со звуком».
- Group profile: tapping a member row does nothing → open the member's profile (Telegram).
- Chat header «8 участников, 3 в сети» vs group profile «128 участников» (demo data) — make the demo consistent, it is the showcase app.
