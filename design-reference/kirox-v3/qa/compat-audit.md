# kiroX 1.0 — аудит совместимости с Telegram и Telegram Premium (compat, 2026-11-23)

Ветка `design/kirox-compose-v3`. Аккаунты владельца — **без Premium**, поэтому всё, что касается Premium,
проверено **по коду** и по семантике TDLib (на телефоне не проверялось). Пути — от
`app/src/main/java/com/kirox/app/`.

Статусы: **works** — работает; **partial** — работает не полностью; **missing** — нет; **broken** — ошибка;
**fixed** — исправлено в этом аудите. Колонка «Free-пользователь» — что видит пользователь без Premium
(требование: честное «доступно в Premium» или скрыто, никогда не фальшивый успех; чужой Premium-контент
должен отображаться и у free).

## 1. Telegram Premium

| Функция | Статус (Premium, по коду) | Free-пользователь | file:line | Что сделать / кто |
|---|---|---|---|---|
| Custom emoji в тексте сообщений (приём) | **partial**: сущность доходит до v3 (`KxEntityType.CustomEmoji`, id в `url`), но пузырь рисует только запасной обычный эмодзи — не пусто, но не сам custom emoji | то же (видит базовый эмодзи) | `ui/v3/data/TelegramKxStore.kt:761`, `ui/v3/screens/ChatScreen.kt:1161` (`bubbleText` игнорирует CustomEmoji) | **fixer-chat4**: в `bubbleText` для `CustomEmoji` добавить `appendInlineContent`/`InlineTextContent` с `ui/components/CustomEmojiImage(id)` — готовый рецепт в старом `ui/chat/MessageText.kt:62-98`. Анимация (TGS/WEBM) — потом через `KxLottie` |
| Custom emoji при отправке / копировании (`KxChat3Mapping.entity`) | **fixed**: раньше `CustomEmoji` выбрасывался при отправке entities; теперь → `TextEntityTypeCustomEmoji(id)` | TDLib сам уберёт custom emoji у free (Telegram так делает), фейка нет | `ui/v3/data/TelegramKxChat3.kt:40` | — |
| Custom emoji при **редактировании** сообщения | **broken**: правка идёт через markdown (`GetMarkdownText` → `ParseMarkdown`), custom emoji и упоминания-по-имени (MentionName) теряются | то же для MentionName | `telegram/TelegramMessageRepository.kt:661` (`editableText`), `:414`, `:456` (`edit`/`editIn`) | data: хранить исходные entities и переносить те, что не попали в markdown (CustomEmoji, MentionName), по совпадению подстроки. Не трогал — затрагивает композер правки (fixer-chat4) |
| Custom emoji в названиях папок | **works** (сохраняются, если название не меняли) | — | `ui/v3/data/TelegramKxAccount.kt:35` | показ — базовым эмодзи |
| Emoji-статус: свой (установка) | **works**: `GetDefaultEmojiStatuses` + `SetEmojiStatus` с длительностью; в сетке показан базовым эмодзи, не картинкой | TDLib вернёт ошибку → теперь «Доступно с Telegram Premium» (fixed в `TelegramErrors`), подпись на экране есть | `ui/v3/data/TelegramKxAccount.kt:375-386`, `ui/v3/screens/extra/StatusScreen.kt:132,185` | экран: рисовать `CustomEmojiImage` вместо `emoji` (свободная зона, мелочь) |
| Emoji-статус: чужой, в профиле | **fixed**: `KxPeer.emojiStatusId` из `profileInfo`, в профиле вместо звезды рисуется сам статус | видит (рендер не зависит от Premium) | `ui/v3/model/KxModels.kt:182`, `ui/v3/data/TelegramKxStore.kt:1438,1444`, `ui/v3/screens/ProfileScreen.kt:79` | — |
| Emoji-статус в списке чатов / шапке чата / своём профиле в Настройках | **missing** | — | `KxChat`/`KxChatHeader` без поля; `TelegramChatRepository.kt:693` знает только `isPremium` | список чатов: добавить `emojiStatusId` в `TelegramChatItem`/`KxChat` (UpdateUser), рисовать `CustomEmojiImage`. Шапка чата — **fixer-chat4** |
| Premium-бейдж (звезда) | **works** в профиле собеседника | видит чужие | `ui/v3/screens/ProfileScreen.kt:80` | в списке чатов нет |
| Premium-стикеры (полноэкранная анимация `premiumAnimation`) | **missing**: стикер играет как обычный (TGS/WEBM/WEBP работают), эффект-оверлей не проигрывается | free тоже должен видеть эффект при открытии — сейчас нет | `ui/v3/screens/KxMediaBubbles.kt:53` | **fixer-chat4** (пузырь стикера): `Sticker.fullType as StickerFullTypeRegular).premiumAnimation` → оверлей по тапу |
| Эффекты сообщений: отправка | **works** (список Telegram, Premium отмечены, `effectId` в `MessageSendOptions`) | Premium-эффект → тост «Этот эффект — с Premium» | `ui/v3/data/TelegramKxChat3.kt:203,263`, `ui/v3/screens/more/MsgEffectsScreen.kt:102` | — |
| Эффекты сообщений: приём (`Message.effectId`) | **missing** — эффект у полученного сообщения не проигрывается и не маппится | то же | `telegram/TelegramMessageRepository.kt` (toModel), `ui/v3/data/TelegramKxStore.kt:680` | data + **fixer-chat4** |
| Реакции: несколько на сообщение | **works** (toggle по ключу, TDLib сам держит лимит) | лимит TDLib, ошибка теперь честная | `telegram/TelegramMessageRepository.kt:441,506` | — |
| Реакции custom emoji: **показ** чужих | **broken**: ключ `ce:<id>` выводится текстом «ce:5368…» в пилюле реакции | **у всех** (free видит чужие custom-реакции в группах) | `ui/v3/screens/ChatScreen.kt:929-933`, `:1022-1026` | **fixer-chat4** (срочно): если `r.emoji.startsWith("ce:")` — `CustomEmojiImage(id, 16.dp)` + счётчик. Так же в `MediaRealViewer.kt:167` и «Кто отреагировал» |
| Реакции custom emoji: **выбор** (Premium) | **missing**: панель берёт только `ReactionTypeEmoji`, `allowCustomEmoji` лишь флаг | Premium-реакции заблокированы замком + тост «с Premium» — честно | `ui/v3/data/TelegramKxChat3.kt:334-338`, `ui/v3/screens/ChatInPlace.kt:244`, `ui/v3/screens/more/ReactPickerScreen.kt:69` | позже: вкладка custom emoji (GetRecentEmojiStatuses/стикер-паки emoji) |
| Теги Избранного | **works** для обычных эмодзи; **fixed**: теги custom emoji больше не теряются (`ce:` ключ туда-обратно) и рисуются картинкой | реакция в Избранном → тост «Теги … с Premium», подпись на экране | `telegram/TelegramMessageRepository.kt:603-623`, `ui/v3/screens/chat/SavedScreen.kt:116`, `ui/v3/screens/ChatScreen.kt:169` | фильтр по тегу в реальном Избранном — «пока не переносится» (тост) |
| Расшифровка голосовых/кружков | **works**, без клиентских лимитов (`RecognizeSpeech`, частичный текст) | `canRecognizeSpeech=false` → «нужен Premium (или бесплатные попытки этой недели закончились)» | `telegram/TelegramExtras.kt:36-58`, `ui/v3/data/TelegramKxMedia.kt:390-397` | — |
| Перевод сообщения | **works** (`TranslateText`, бесплатно у Telegram) | работает | `ui/v3/data/TelegramKxStore.kt:455`, `ui/v3/data/TelegramKxChat3.kt:439` | — |
| Перевод всего чата (Premium) | **missing** (`ToggleChatIsTranslatable`/плашка «Перевести чат») | — | — | после 1.0 |
| Лимит загрузки 4 ГБ / скорость скачивания | **works**: клиентских ограничений размера/скорости нет, всё решает TDLib | ошибка размера → «Файл больше лимита Telegram (2 ГБ, с Premium — 4 ГБ)» (fixed) | `telegram/TelegramErrors.kt:45` | — |
| Лимит закреплённых чатов | **works**: из опции `pinned_chat_count_max`, не захардкожен | ошибка TDLib → «Достигнут лимит закреплённых чатов. С Premium — больше» (fixed) | `ui/v3/data/TelegramKxChat3.kt:165`, `ui/v3/screens/more/PinnedScreen.kt:58` | мелочь: перечитывать опцию по `UpdateOption` (после покупки Premium лимит обновится только после перезапуска) |
| Лимит папок / каналов | **works**: клиент не ограничивает, пишет в TDLib | ошибка → «лимит папок / слишком много групп и каналов. С Premium — больше» (fixed) | `ui/v3/data/TelegramKxAccount.kt:270-271`, `telegram/TelegramErrors.kt:45` | — |
| Закреплённые истории | **works**: лимит из `pinned_story_count_max` | — | `ui/v3/data/TelegramKxMedia.kt:436` | фолбэк `?: 3` только если опции нет |
| Цвет имени / профиля | **works**: список от Telegram (`UpdateAccentColors`/`UpdateProfileAccentColors`), фон-эмодзи сохраняется | Premium-цвета подписаны «(Premium)», ошибка честная | `ui/v3/data/TelegramKxAccount.kt:42-50,366-372`, `ui/v3/screens/extra/NameColorScreen.kt:73,208` | выбор фонового custom emoji — **missing** |
| Чужие цвета имени (accentColorId) | **works** (colorIndex в списке/профиле) | видит | `telegram/TelegramChatRepository.kt:521,692` | — |
| Истории: режим инкогнито (stealth, Premium) | **missing** (`ActivateStoryStealthMode` не вызывается) | — | — | есть своя замена — ghost-истории (ниже) |
| Истории: постоянный список просмотров | **partial**: текст объясняет Premium-правило, данные от TDLib | честная подпись | `ui/v3/screens/media/StoryViewsScreen.kt:175` | — |
| Истории: ссылки/форматирование в подписи | **partial**: подпись выводится текстом | — | `ui/v3/data/TelegramKxMedia.kt:401` | после 1.0 |
| «Скрыть время прочтения» | **works**: `Get/SetReadDatePrivacySettings`, «Прочитано в …» с честными причинами | работает (бесплатная настройка) | `telegram/TelegramSettingsRepository.kt:247,254`, `ui/v3/data/TelegramKxChat3.kt:102-108` | — |
| Новые чаты только от контактов (Premium) | **works** (настройка в TDLib) | ошибка честная | `ui/v3/model/KxStore.kt:375` | — |
| Business: автоответ, часы, быстрые ответы | **missing**: экраны `Away`, `Hours`, `QuickReplies` — демо, из реального UI не открываются | не видны — честно | `ui/v3/screens/extra/AwayScreen.kt:44`, `HoursScreen.kt:46`, `chat/QuickRepliesScreen.kt:44` | после 1.0; не показывать в релизе |
| Списки задач (checklist, Premium) | **works** | подпись «с Premium» | `ui/v3/data/TelegramKxChat3.kt:233`, `ui/v3/screens/ChatInPlace.kt:337` | — |
| «Отправлять от имени» (Premium-каналы) | **works** (`needsPremium` прокинут) | помечено | `ui/v3/data/TelegramKxChat3.kt:246-253` | — |

## 2. Не ломать нормальное поведение Telegram

| Поведение | Статус | file:line | Примечание / fix |
|---|---|---|---|
| Прочтение (ViewMessages) при выключенном призраке | **fixed**: раньше открытый чат «читал» всё пришедшее, даже когда kiroX свёрнут / экран выключен. Теперь — только когда kiroX на экране; при возврате читается то, что пришло в фоне | `telegram/TelegramMessageRepository.kt:51-54,824-829`, `notify/MessageNotifications.kt:431-445` (`AppVisibility.watch`) | Остаётся: читается вся загруженная страница, а не только видимые на экране сообщения (официальные клиенты — только видимые). **fixer-chat4**: отдавать id видимых из LazyColumn → `ViewMessages` только по ним |
| Призрак: не читать | **works** (ни ViewMessages, ни «прослушано» OpenMessageContent) | `TelegramMessageRepository.kt:826`, `ui/v3/data/TelegramKxMedia.kt:385` | **fixed (честность)**: в «Функции kiroX» добавлено «Ответ или реакция всё равно отметят чат прочитанным — так работает Telegram» (`ui/v3/screens/settings/FeaturesScreen.kt:178`) |
| «печатает…» | **works**: шлётся только при реальном вводе, повтор ≤ раз в 4 с, отмена при очистке; восстановление черновика не шлёт; призрак — не шлёт | `telegram/TelegramChatActionRepository.kt:134-148`, `ui/v3/data/TelegramKxStore.kt:478-488` | — |
| «в сети» | **works**: online только пока kiroX виден; призрак — всегда offline | `notify/MessageNotifications.kt:73-76` | — |
| Просмотр историй | **works**: `OpenStory/CloseStory`; призрак — не шлётся | `telegram/TelegramStoryRepository.kt:60-61` | — |
| Удалённые сообщения («Сохранять удалённые») | **fixed (пометка)**: были в ленте как обычные — теперь `KxMessage.deleted`, время «удалено · 12:30», правка запрещена. Только локально (MessageArchive на телефоне), по умолчанию выкл. | `ui/v3/data/TelegramKxStore.kt:732-735,786`, `ui/v3/model/KxModels.kt:148`, `telegram/MessageArchive.kt` | **fixer-chat4**: нарисовать нормальную пометку (иконка корзины / приглушённый пузырь) по `m.deleted`, скрыть «Реакция/Ответить/Переслать» |
| История изменений | **works**: локально, по умолчанию выкл., в меню честный текст «прежний текст не сохранён…» | `ui/v3/screens/ChatScreen.kt:596-602`, `ui/v3/data/TelegramKxStore.kt:1149` | — |
| Авто-действия | **works**: нет авто-входа в чаты, авто-/start ботов, авто-чтения; `JoinChat` только по нажатию | `telegram/TelegramChatRepository.kt:239,261` | Нет кнопки «ЗАПУСТИТЬ» для бота (пишется /start вручную) — **fixer-chat4**, `TelegramKxStore.kt:288` |
| Автозагрузка медиа | **works**: правила «Автозагрузка медиа» по сети (фото, GIF ≤ 8 МБ, файлы/кружки ≤ лимита), большое — по нажатию; аватары и стикеры — всегда (как у Telegram) | `ui/v3/data/TelegramKxStore.kt:705-716` | — |
| Уведомления и mute | **works**: mute/исключения/превью — серверные (`SetChatNotificationSettings`, `SetScopeNotificationSettings`), показ — через TDLib notification API (учитывает mute и прочтение на других устройствах) | `ui/v3/data/TelegramKxChat3.kt:550-559`, `telegram/TelegramSettingsRepository.kt:195-213`, `notify/MessageNotifications.kt:61` | — |
| Папки / архив / закрепы | **works**: всё через TDLib (`Create/Edit/Delete/ReorderChatFolders`, `AddChatToList`, `ToggleChatIsPinned`) | `telegram/TelegramChatRepository.kt:56-59,134-148`, `ui/v3/data/TelegramKxAccount.kt:270` | — |
| FLOOD_WAIT | **works**: ошибки 429/FLOOD_WAIT показываются с временем; сканер дней рождения (50 своих контактов, раз за сессию) останавливается на первом 429 | `telegram/TelegramErrors.kt:5-14`, `ui/v3/data/TelegramKxChat3.kt:504-517` | — |
| api_id / api_hash | **works**: из `kirox.properties`/env (BuildConfig), без них клиент не стартует | `telegram/TelegramClient.kt:86,318`, `build.gradle.kts:40,45` | — |
| Скрапинг | **ok**: только свои данные (контакты, свои чаты) | — | — |
| Ошибки Premium-only | **fixed**: `PREMIUM_*`, лимиты закрепов/папок/каналов/реакций, размер файла → честный русский текст вместо «Не удалось выполнить запрос» | `telegram/TelegramErrors.kt:33,40-58`, тест `TelegramErrorsTest.premiumOnlyAndLimitsAreHonest` | — |

## 3. Для занятых зон (не трогал)

**fixer-chat4** (ChatScreen, меню, медиа/истории):
1. `ChatScreen.kt:929-933` и `:1022-1026` — реакции `ce:<id>` выводятся текстом. Рисовать `com.kirox.app.ui.components.CustomEmojiImage(id, 16.dp)`. Касается и free-пользователей. **Важно для 1.0.**
2. `ChatScreen.kt:1161` `bubbleText` — custom emoji в тексте: inline-картинка вместо базового эмодзи (рецепт — `ui/chat/MessageText.kt:62-98`).
3. Пометка удалённых: `KxMessage.deleted` уже приходит; нарисовать знак и убрать действия, кроме «Копировать»/«Удалить у меня».
4. Прочтение только видимых сообщений (сейчас — вся загруженная страница, пока kiroX на экране).
5. Premium-анимация стикеров и эффекты полученных сообщений.
6. Правка сообщения теряет custom emoji и MentionName (markdown-путь).
7. Кнопка «ЗАПУСТИТЬ» для ботов вместо ручного /start.

**fixer-chat4 — сделано (по 60b2709, на телефоне не проверено):**
1. Реакции `ce:<id>` — `ReactionPill` в ChatScreen (пузырь и альбом) рисует `CustomEmojiImage` + счётчик; тост во вьюере без сырого ключа.
2. Custom emoji в тексте пузыря — inline-картинка (`customEmojiInline` + аннотация inlineContent в `bubbleText`), базовый эмодзи до загрузки.
3. Удалённые (`m.deleted`): пузырь приглушён, иконка корзины у «удалено · время», нет свайпов/двойного тапа/реакций; в меню только Копировать / Перевести / Выбрать / «Убрать с экрана» (локально, без запроса в Telegram — `deleteIn` убирает сохранённые).
4. Прочтение только видимых: `TelegramMessageRepository.visibleOnly` (включает TelegramKxStore) + `reportVisible` из LazyColumn (`store.messagesOnScreen`); до первого отчёта ничего не читается.
5. Эффекты полученных сообщений (`effectId` → `GetMessageEffect` → анимация) и полноэкранная анимация Premium-стикера (`premiumAnimation`) — один раз при приходе, пока чат открыт (`ChatEffectsLayer`, TGS). Повтор по тапу на стикер не сделан.
6. Правка сохраняет CustomEmoji и MentionName: `editedText` переносит их из исходного текста по совпадению подстроки (без дублей).
7. Бот без истории: `KxComposer.Start` → кнопка «ЗАПУСТИТЬ», /start только по нажатию.

**music / shield** — замечаний по совместимости нет.

## 4. Фишки kiroX (есть в коде и работают сегодня)

- **Режим призрака** — не читать, не «печатает», быть офлайн, тайно смотреть истории; каждое по отдельности (`settings/KiroxTweaks.kt`, «Функции kiroX»).
- **Сохранять удалённые сообщения** и **историю изменений** — локально на телефоне, по умолчанию выкл. (`telegram/MessageArchive.kt`).
- **Проверка файлов (FileGuard)** — предупреждение об опасных файлах, zip-бомбах, двойных расширениях (`files/FileGuard.kt`).
- **Встроенный браузер** с приватным режимом, экономией трафика, блокировкой сторонних cookies (`KiroxTweaks` browser*).
- **Фильтры сообщений** — скрывать в группах/каналах сообщения со словами или от людей (`settings/MessageFilters.kt`).
- **Пересылка без автора**, **подтверждение** перед стикером/GIF/голосовым/звонком, **ID в профилях**.
- **Музыкальный плеер** по музыке из Telegram с плейлистами (`telegram/TelegramMusic.kt`, `settings/Playlists.kt`, док-плеер).
- **Уведомления всех аккаунтов** — фоновый «подгляд» неактивных аккаунтов (`notify/AccountPeek.kt`, `telegram/SessionLocks.kt`).
- **Код-пароль приложения** (`security/PasscodeLock.kt`); **«Щит»** (`security/shield`, в работе у shield).
- **Дни рождения** контактов, **перевод при наборе** (TypeTranslate), **«Мало места»**/политика хранилища (`telegram/StoragePolicy.kt`), **энергосбережение** (`settings/PowerSaver.kt`).
- **Оформление**: свои темы, значки приложения, обои, сезонные слои (`settings/CustomThemes.kt`, `AppIcons.kt`, `ChatWallpapers.kt`, `ui/components/SeasonalLayer.kt`); скрыть вкладки «Звонки»/«Истории».
- **Обновления внутри приложения** (`update/UpdateCenter.kt`), резервная копия настроек (`settings/SettingsBackup.kt`).

## 5. Что изменено в этом аудите

- `telegram/TelegramMessageRepository.kt` — прочтение только при видимом kiroX + дочитывание при возврате; теги Избранного с custom emoji.
- `notify/MessageNotifications.kt` — `AppVisibility.watch` (несколько слушателей).
- `telegram/TelegramErrors.kt` (+ тест) — честные тексты Premium-only/лимитов.
- `ui/v3/data/TelegramKxChat3.kt` — custom emoji сохраняются при отправке entities.
- `ui/v3/data/TelegramKxStore.kt`, `ui/v3/model/KxModels.kt` — `KxPeer.emojiStatusId`, `KxMessage.deleted` + пометка «удалено ·», запрет правки удалённого.
- `ui/v3/screens/ProfileScreen.kt` — emoji-статус собеседника.
- `ui/v3/screens/chat/SavedScreen.kt` — теги custom emoji картинкой.
- `ui/v3/screens/settings/FeaturesScreen.kt` — честная подпись режима призрака.
