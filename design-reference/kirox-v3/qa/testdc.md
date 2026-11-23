# kiroX test — Telegram test servers (test DC) + simulated users

A separate app for QA that talks **only to Telegram's test servers**, never the real Telegram.

| | |
|---|---|
| Gradle build type | `qadc` (AGP forbids build-type names starting with "test") |
| Package | `com.kirox.app.testdc` (installed next to `com.kirox.app` and `com.kirox.app.v3`) |
| Launcher label | **kiroX test** |
| TDLib | `useTestDc = true` (`BuildConfig.TELEGRAM_USE_TEST_DC`; false in every other build type) |
| Databases | own app data, TDLib under `files/tdlib-testdc/<session>` |
| Push (Firebase) | off (no google-services client for this package) |
| Harness | `app/src/qadc/` — receiver `com.kirox.app.testdc.SimReceiver`, only in this build |

## Build and install (Xiaomi 79ed61c9 only)

```
python design-reference/kirox-v3/qa/kxbuild.py installtest
```

Only qa-lead uses the phone. Never touch `com.kirox.app` (the user's real account).

## What the user does once (by hand — agents never sign in or type codes)

Test accounts use numbers **+99966XYYYY**: X = test DC number (1–3), YYYY = any 4 digits, e.g. `+99966 1 0001`.
The login code for a test number is the DC digit repeated: X=1 → `11111` (5 digits). No SMS is sent.

1. Open **kiroX test**, sign in with the first test number (e.g. `+9996610001`). This account is "you" in the UI.
2. Add more accounts in the app's account switcher (Settings → add account): `+9996610002`, `+9996610003`, …
   Switch back to the first account afterwards so the UI shows it.
3. Optional: give each test account a name and a username so the others can find it (`@kx_sim_a`…).
   Test accounts can only talk to other test accounts.

The simulated users are the **signed-in accounts that are not open in the UI**. The harness opens each
one's own TDLib database in the background (like a notification peek) and closes it after 3 minutes idle.

## Simulated users — `adb shell am broadcast`

Common prefix (the `-p` makes the broadcast reach the app; the receiver accepts only adb shell / system):

```
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc <extras>
```

Read results:

```
adb -s 79ed61c9 logcat -s KxSim
```

Every line starts with `[rid]` when you pass `--es rid <id>`; each command ends with `OK …` or `ERROR …`.
If the app was force-stopped, launch it once (or add `-f 0x20`).

Extras:

| extra | meaning |
|---|---|
| `--es cmd` | `accounts` · `chats` · `dump` · `send` · `reply` · `react` · `typing` · `read` · `edit` · `delete` · `join` · `online` · `close` |
| `--es as` | which account acts: session name, **last digits of its phone** (e.g. `0002`), or its name |
| `--es chat` | chat title (as this account sees it) · `@username` · `+phone` (opens a private chat) · numeric chat id · `me` |
| `--es text` | message text (`--ez md true` parses Markdown V2) |
| `--el msg` | target message id (from `dump`) |
| `--ei back` | target the N-th newest message (0 = newest) |
| `--es emoji` | reaction emoji (default 👍); `--ez remove true` removes it, `--ez big true` big reaction |
| `--es action` | for `typing`: `typing` `voice` `video` `videonote` `photo` `document` `sticker` `location` `contact` `game` `cancel` |
| `--ei secs` | for `typing`: how long (1–40, default 5); repeated every 4 s, then cancelled |
| `--ei limit` | for `dump` / `chats` / `read` (default 20 / 30 / 20) |
| `--es value` | for `online`: `true` / `false` |
| `--es rid` | request id echoed in the log lines |

Default targets: `react` / `reply` → the newest message **not** sent by the acting account; `edit` / `delete` → its own newest message.

### Examples

```
# who is signed in (session, name, last 4 digits; "(open in UI)" marks the UI account)
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd accounts

# account …0002 writes to the UI user by username
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd send --es as 0002 --es chat @kx_main --es text "Привет! Как дела?"

# typing for 8 s, then a reply to the newest incoming message
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd typing --es as 0002 --es chat @kx_main --ei secs 8
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd reply --es as 0002 --es chat @kx_main --es text "Отвечаю на это"

# react ❤️ to a specific message, then read the chat (read receipts for the UI user)
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd react --es as 0002 --es chat @kx_main --el msg 1048576 --es emoji "❤"
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd read --es as 0002 --es chat @kx_main

# edit / delete own newest message
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd edit --es as 0002 --es chat @kx_main --es text "исправлено"
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd delete --es as 0002 --es chat @kx_main

# last 30 messages of a chat as account 0003 sees it (ids, sender, reply->id, reactions, out/edited)
adb -s 79ed61c9 shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd dump --es as 0003 --es chat "QA group" --ei limit 30
```

Quoting: from Windows PowerShell wrap the whole remote command in single quotes so the device shell sees the inner
double quotes, e.g. `adb -s 79ed61c9 shell 'am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd send --es as 0002 --es chat @kx_main --es text "hi there"'`.
Non-ASCII text (Cyrillic, emoji) passes through `am` fine on Android 7+.

## Notes and limits

- The account open in the UI can also be driven (`--es as` = its digits); the harness then uses the app's own client.
- When the UI switches to an account the harness holds, the harness client closes first (same lock as notification peeks).
- A command times out after 50 s. Sends are logged again when the server confirms: `sent <session>: #temp -> #final`.
- No voice/video/media sending and no calls from the harness ("voice-less" actions only).
- The harness never signs in, never logs out and never prints phone numbers beyond the last 4 digits.
