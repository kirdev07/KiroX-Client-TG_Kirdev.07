# Wave 5 coverage

| function | old UI | v3 | status |
|---|---|---|---|
| photo/video send (gallery) | ok | ok (video without w/h) | bug |
| photo/video viewer, album, save, share, forward, delete | ok | ok | bug (save on A7–9, stale paths) |
| spoiler / view-once receive | bug | bug | bug |
| view-once send | ok | ok (ViewOnce board) | ok |
| video player (speed, chapters, stream) | ok | ok | bug (audio focus) |
| GIF play / autoplay | ok | missing | bug (gap) |
| music player + library | ok | Music board ok; chat music no tap | bug (gap) |
| files: download/progress/cancel/open/share | ok | no chat path; Downloads/Files empty | bug (gap) |
| voice record | ok on A10+ | ok on A10+ | needs-android7 (broken API<29) |
| voice play / listened / transcribe in chat | ok | dead buttons | bug (gap) |
| round video record | ok | ok (wave4 fix) | ok |
| round video play | ok | placeholder | bug (gap) |
| stories: view photo | ok | ok | ok |
| stories: view video | ok | thumbnail only | bug (gap) |
| story post / reply / react / viewers | ok | ok | bug (>60 s) |
| story archive / pinned / hidden | ok | StoryArchive unreachable, Hidden ok | bug (gap) |
| 1:1 call out/in, emoji key, rating | ok | ok (behind the old overlay) | needs-testdc |
| call ringtone / audio route / proximity | ok | missing in v3 | bug (gap) |
| call permissions | ok | answer/camera bypass | bug (gap) |
| calls on 32-bit / Android 7 | fake active | fake active | needs-android7 |
| incoming call notification sound on A7 | silent | silent | needs-android7 |
| group video chat join/mute/camera | ok | join only from Calls tab / PlanCall | bug (gap) |
| screen share, raise hand | ok | missing | bug (gap) |
| plan video chat | — | ok | needs-testdc |
| live location | ok | demo only | bug (gap) |
| stickers / GIF / emoji panel | ok | unreachable | bug (gap) |
| auto-download settings | ok | fake toggles | bug (gap) |
| keep media / cache limit / clear cache | ok | ok | bug (freed number) |
| call data saving | ok | ok | ok |
| network traffic / net check | ok | boards unreachable | bug (gap) |
| **qa-settings** | | | |
| Passcode lock gate (start/autolock, back→background, FLAG_SECURE) | MainActivity + auth/PasscodeScreens ok | same gate covers v3 | ok |
| Passcode lockout 5 tries → 30 s…1 h, kept across restart | PasscodeLock ok | Lock/Hidden use store.passcodeWait | ok (low: wall-clock) |
| Passcode set/change/off, autolock, hide content, pattern | PrivacySettings.kt:230 ok | none (Lock = entry only) | bug (high gap) |
| Fingerprint unlock | ok (setting honoured) | Lock board honours biometric flag | ok / needs-android7 (Android 10+ only per architect) |
| Graphic key unlock | ok | digits only | bug |
| Hidden chats | — | Hidden board, main list only | bug (leaks, not per account) |
| Privacy rules (6 main) | ok | Privacy, real TDLib, exceptions kept | ok |
| Privacy rules (bio, birthdate, voice, find by phone, p2p, gifts, audio) + exceptions | ok | missing / toast | bug (high gap) |
| Read date, new chats to archive | ok | ok | ok |
| Privacy score / passcode tile | — | `pin = true` | bug |
| Sessions: list, terminate one/others, TTL | ok | Devices ok (sheet confirm) | ok |
| QR device login | ok | no camera | bug (high gap) |
| Cloud password set | ok | TwoStep wizard | ok (e-mail code missing: bug) |
| Cloud password change/disable/recovery | ok | none | bug (high gap) |
| Login e-mail | — | LoginEmail real, unreachable | bug (gap) / needs-testdc |
| Change number | ok (dialog) | ChangeNumber real + KxConfirm | needs-testdc |
| Delete account (password, confirm) | ok | DeleteAccount real, unreachable | bug (gap) / needs-testdc |
| Logout (+clear cache) | ok | Accounts confirm, Logout board unreachable | low race |
| Accounts add/switch | AccountsSheet ok | Accounts ok (max 3) | ok / needs-testdc |
| Themes / colour swatches | ThemesScreen | Custom THEME (v3-only storage) | ok (v3) |
| Own theme editor / import / export | ok | none («Своя» fixed) | bug (high gap) |
| Chat look: bubbles, text size, density, wallpaper | ok | ChatStyle, shared settings repo | ok |
| Per-chat wallpaper | ChatWallpapers | Wallpaper board via chat menu | ok |
| Night mode / auto theme | ok | NightMode writes autoTheme, v3 always dark | bug (gap) |
| Animations | AnimationsScreen | Anim board → reduced motion | ok |
| Accessibility (scale, bold, contrast, palette) | — | Access applied via Kx.look; only via search | ok (reachability gap) |
| Launcher icon (6 aliases) | AppIconDialog ok | AppIcon fake toast | bug (high gap) |
| Own icon (pinned shortcut) | CustomIconScreen ok (Android 8+) | none | bug (high gap) / needs-android7 (not supported <26, handled) |
| Dock tabs hide Calls/Stories | ok | Custom, shared KiroxTweaks | ok |
| Dock tab names | — | Language → I18n overrides | ok |
| Language RU/EN | ok (117 strings missing) | v3 Russian only | bug |
| Name packs / rename texts | ok | none | bug (high gap) |
| Power saving | ok | Power board unreachable, per-feature switches fixed | bug (gap) |
| Storage usage + clear cache, keep media | ok | Storage ok | ok |
| Auto-download, cache limit, network usage | ok | fake switches / missing | bug (high gap) |
| Proxy add/enable/disable | ok | Proxy real, unreachable | bug (high gap) |
| Update checker | ok | Update board unwired | bug (high gap) |
| Settings backup / restore | file | Backup → Saved Messages + restore | ok (file export gap) |
| Ghost mode (read/typing/offline/stories) | ok | Features, shared KiroxTweaks | ok |
| Deleted messages / edit history | ok | Features tiles | ok (clear-saved gap) |
| File guard, in-app browser, IDs, forward w/o author, confirm sends | ok | Features | ok |
| Browser settings (private, cookies, JS, clear) | ok | none | bug (gap) |
| Message filters | ok | WordFilter | bug (sync, people, blur <12) |
| Crash log / reset kiroX settings / sticker sets | ok | none | bug (gap) |
| Notifications scopes on/off + preview | ok | Notify real | ok |
| Notification in-app/reactions/sounds/exceptions/reset | ok | local fake | bug (high gap) |
| DND | — | Dnd real (KxDnd), unreachable | bug (gap) |
| Profile edit name/bio/username/birthday | ok | EditProfile real | ok |
| Profile colour / emoji status | — | NameColor/Status real, unreachable | bug (gap) |
| Contacts list / add | ok | Contacts, AddContact | ok (qa-chat owns details) |
| Group: info, slow mode, join by request, invite links create/revoke | ok | Group/Invite real | ok / needs-testdc |
| Group: admin rights | ok | promote default set | bug |
| Group: restrict/ban | ok | read-only forever only | bug |
| Group: members, requests, stats, boosts, admin log | ok | real loaders | needs-testdc |
| English completeness (old UI) | 117 missing | n/a | bug (low) |
| Chats list: rows, unread/mention badges, typing, draft line | TelegramMainScreen.kt (read) | ChatsScreen.kt — phone ✔ | ok |
| Folder chips (switch, counters) | FoldersScreen.kt / TelegramMainScreen | ChatsScreen.kt:84 — phone ✔ | ok |
| «+ Папка» chip | FoldersScreen «Создать новую папку» | ChatsScreen.kt:87 was `{}` | bug (fixer2 taking) |
| Swipe actions (read/pin/mute/archive/delete) | ChatListInteractions.kt | ChatSwipe.kt — phone ✔ (archive, unread) | bug low (labels), gap (no undo, Swipe board unreachable) |
| Selection: read/mute/pin/archive/delete | ChatListInteractions.kt | ChatsScreen.kt:106 — phone ✔ | ok (demo pin order low) |
| Pin / unpin chat | TelegramChatRepository.setPinned | store.setPinned — phone ✔ icon | ok real; demo order low |
| Mute / unmute chat | setChatNotificationSettings | store.setMuted — phone ✔ | ok |
| Archive: open, tip, select, return, settings | TelegramMainScreen «Архив» | ArchiveScreen.kt — phone ✔ | bug medium (Back on settings sheet) |
| Mark read / unread | TelegramChatRepository:295-305 | store.markRead/markUnread — phone ✔ | ok |
| Delete / leave chat | TelegramMainScreen (with undo) | ChatSwipe/Selection + DeleteChat board | gap (no undo) |
| Global search: chats, contacts, messages | ChatListInteractions.kt filters | SearchScreen.kt — phone ✔ | bug medium (demo recents on real, dead tabs), high (contact open), low (focus) |
| New message / contact / group / channel / secret | NewMessageScreen.kt | NewMsgScreen.kt — phone ✔ | bug high (CreatePrivateChat), demo low |
| Chat: send text (markdown) | TelegramMessageRepository.inputText/ParseMarkdown | store.send — phone ✔ demo | ok |
| Chat: edit | editableText + EditMessageText/Caption | ChatScreen.kt:331 — phone ✔ demo | bug medium ×2 (draft pollution, formatting lost) |
| Chat: reply | InputMessageReplyToMessage | ChatScreen.kt:330 | ok; reply-quote tap = bug medium |
| Chat: quote reply | «Цитировать» | Quote board unreachable | gap high |
| Chat: delete with undo (me / for all) | delete dialog | MessageMenu DeleteSheet + UndoBar — phone ✔ | ok |
| Chat: forward (one / many, without author) | ForwardMessages asCopy | ForwardScreen.kt | ok one; gap many; low fake success when source not open |
| Chat: select messages | selectedIds | MultiSelect board | ok (gesture hold = fake toast, medium) |
| Reactions (strip, all, toggle) | toggleReaction | MessageMenu strip + ReactPicker — phone ✔ | ok |
| Pinned bar (show / unpin / tap) | pin/unpin, jump | ChatScreen.kt:381 | bug medium (tap does nothing) |
| Links / mentions / hashtags / commands | ChatLinks | ChatLinks.kt (wave4 fix) | ok (code) |
| Formatting entities in bubbles | — | ChatScreen bubble | ok (code; demo shows raw **) |
| Attach sheet tiles | TelegramChatScreen.kt:977 | RealAttachSheet / demo AttachSheet — phone ✔ demo | gap high (camera, picker, music, spoiler, round, live loc) |
| Voice record (hold, lock, cancel) | VoiceNotes.kt | Composer — phone ✔ demo (0:02 sent) | ok |
| Round video record | VideoNoteRecorder.kt | real: old VideoNoteRecorder reused (attach «Кружок», hold mic in circle mode) | ok (fixer-attach, not phone-tested) |
| Stickers / GIF / emoji panel | StickerPanel.kt | composer button `{}` — phone ✔ dead | gap high |
| Scroll / paging / «Вниз» | requestOlder | loadOlder, ↓ button | ok |
| Jump to message (date, search hit, pinned, show in chat) | rows.indexOfFirst + jumpDone | not handled | bug high |
| Empty chat state | — | EmptyChatCard | ok (code) |
| Typing indicator (in / out) | TelegramChatActionRepository | header typing, setDraft→Typing | ok |
| Drafts (restore / save / clear / list) | TelegramChatScreen.kt:294 | setDraft only | bug high (restore), Drafts board unreachable |
| Chat ⋮ menu boards | TelegramChatScreen.kt:1308-1328 | ChatMenu.kt — phone ✔ | gaps (export, deleted msgs, scheduled) |
| Notifications per chat | NotificationSettings.kt | ChatNotify board — phone ✔ | ok (real: mute/preview/mentions/pinned saved) |
| Chat search | «Поиск» SearchChatMessages | ChatSearch board — phone ✔ demo | bug medium (from Profile no chat id); jump bug high |
| Jump to date | GetChatMessageByDate | JumpDate board | bug (jump not scrolled, duplicate chat route) |
| Scheduled messages | GetChatScheduledMessages | Schedule board (demo only) | gap high |
| Saved messages (+ tags) | tags, «По чатам» | plain chat; Saved board unreachable on real | gap high |
| Forum topics | topics list / new topic | Topics board unreachable | gap high |
| Kept deleted msgs / edit history / export | kiroX | toggles only | gap high |
| Stranger bar (add / block) | action bar | ContactBar unreachable | gap high |
