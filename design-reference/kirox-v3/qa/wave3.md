# Wave 3 — from mockup data to TDLib everywhere

Goal (user, 2026-11-23): every v3 board shows the real account's data when it runs on TelegramKxStore. KxDemoStore stays (do NOT delete demo); boards keep demo behaviour only when `ctx.store is KxDemoStore`.

Source of truth for open items: `qa/tg-tasks.md` lines `- [ ]`. Tick them there (`— wave3: done: store.xxx`) or mark `— wave3: stays demo: <reason>`.

## Rules (all agents)
- Never call ./gradlew directly. Build only through `python design-reference/kirox-v3/qa/kxbuild.py compile` / `test "com.kirox.app.v3data.*"`. One build at a time (the script queues).
- Do not run `install`, do not touch any phone, no connectedAndroidTest. The lead installs and checks the demo app.
- No git commits, no push. The lead commits.
- Never sign in, never send anything from a real account, never print secrets (kirox.properties, google-services.json, local.properties, keystore).
- Contract is additive: new KxStore members with defaults; KxDemoStore gets demo values; TelegramKxStore implements. Avoid `x` + `setX` pairs (JVM clash). No non-local `return@key` in inline composables (D8 crash).
- Destructive / paid actions on the real store (leave, delete, ban, report, spend stars, change number, terminate session) must go through a confirm (KxConfirm) in the screen.
- Honesty: on the real store show real data or an honest empty/“not available” state — never invented numbers.
- Reuse the existing telegram/** repositories (music player, files explorer, calls, stickers, folders…) instead of new TDLib code when they exist. Additive changes only there.
- Shared files (KxStore.kt, KxModels.kt, KxDemoStore.kt): re-read right before each edit, append in your own section block `// wave3-<you>` so edits don't collide.
- Put your TelegramKxStore implementation in your own file (named below) as extension/helper objects wired from TelegramKxStore with a one-line delegate.
- Append a line to `qa/status.md` after each batch: `- HH:MM <name>: …`.

## Ownership
### binder-media (file: ui/v3/data/TelegramKxMedia.kt; screens/media, Voice/Transcribe, calls)
Viewer/Album/VideoPlayer (chat media list, save/forward/delete), Music (old music library + player), Files/Gallery/Downloads (downloaded files, progress, remove from device), Voice + Transcribe (listened flag, recognizeSpeech), StoryArchive + StoryPost/Editor/VideoTrim (post story from picked media, archive, pin), VideoCall/GroupCall/VideoConf/Incoming/CallEnd/Pip/KeyCheck/CallWeak/CallWaiting/PlanCall (old call service state), Sounds, Traffic (getNetworkStatistics), LowSpace, NetCheck, StoryViews close friends + open unknown chat (tg-tasks 433–435).

### binder-chat (file: ui/v3/data/TelegramKxChat3.kt; screens/chat, screens/more, chat parts of screens/extra)
SendOptions, SendAs, MsgEffects, ChatTheme, InlineBot, EditMsg, MultiSelect, Pinned, MsgInfo, Seen, ReactPicker, QuickReact, ReplaceMedia, Drafts, Mentions, Hashtag, ChatPeek, NextUnread, Format (entities on send), LinkPreview, JumpDate, StickerPack, StickerManage, DiceGames, GiftOpen, ShareContact, ContactNote, ContactBar, Report, DeleteChat (leaveChat), AutoDelete, ChatNotify, Checklist, Memories, Explore, Birthdays, EmojiMix, Activity, MeetTime, BigEmoji, ViewOnce, Sensitive, LinkWarn, TypeTranslate, ClipPaste, FilePicker, ShareIn, SmartText, ReadAloud, Drive/Digest leftovers, UndoSend (local delay), Channel admin rights + verified/scam (413/415/440).

### binder-account (file: ui/v3/data/TelegramKxAccount.kt; screens/settings, screens/tools, account parts of screens/extra, screens/groups leftovers)
FolderEdit, FolderShare, PersonalChannel, ChangeNumber, LoginEmail, LoginAlert, TermsUpdate, SecurityCheck, Limited, AntiSpam, ContactSync, InviteFriends, NewGroup extras, Cleanup, Import, Export, NameColor (TDLib accent colours), Status (emoji list + expiry), CountryPicker, Welcome permissions, MyQr scanner, Hidden (BiometricPrompt), Remind, Backup, Dnd, WordFilter, ChatGestures/Swipe, Access, Widgets, Shortcuts, SettingsSearch recents, NightMode sunset/system (396), v3 tab visibility from the old settings, Wellbeing/YearRecap (local counting only if honest), Help/RateApp/BugReport (send via the existing bug-report path if one exists, else honest note).

### Stays demo (do not build; lead reports to the user)
Nearby, ListenTogether (need a kiroX server), Update (APK updater), Watch (Wear OS), Bubbles, PhotoSearch (OCR), CallCaptions (speech-to-text in calls), Away/Hours/QuickReplies (Business), AppIcon (assets), AppsCatalog if TDLib has no catalog call, Push/WhatsNew/Browser/Offline (nothing to bind).

### tg-tester
Reviews each binder batch against TDLib + Telegram behaviour, writes items into tg-tasks.md under `## wave3 review`, adds v3data unit tests for pure mappings, runs `kxbuild.py test "com.kirox.app.v3data.*"`. Tells binders by SendMessage what to fix.
