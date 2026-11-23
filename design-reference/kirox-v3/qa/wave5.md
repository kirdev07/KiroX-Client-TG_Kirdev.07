# Wave 5 — every function checked once more + antivirus proposal

Rules: same as qa/wave4.md (builds only via kxbuild.py; no commits/push; never print secrets; never sign in; phone = Xiaomi 79ed61c9 only, only com.kirox.app.v3, only qa-chat uses it; never com.kirox.app; no Samsung; bugs fixed, design/UX changes only as proposals in qa/ux-proposals.md; no deleting files).

Scope: EVERY function of the app — old UI (app/src/main/java/com/kirox/app/ui/** outside v3, real-account flows read in code) and the new v3 UI (demo on the phone + real-store branches in code). For each function: does the entry point exist, does the action reach TDLib/the setting correctly, is the result shown, errors handled, state survives back/rotation, no fake success on the real account.

Bugs → `qa/wave5-bugs.md` (`- [ ] **sev** area — file:line — problem → fix — found by X`). Coverage table → `qa/wave5-coverage.md` (function | old UI | v3 | status ok/bug/needs-testdc/needs-android7).

Agents:
- **qa-chat** — chats list, chat, composer, message menu, links, search, archive, folders, forward/select, drafts, notifications. Owns the phone.
- **qa-media** — photos/videos/viewer, music, files/downloads, voice/round, stories, calls/group calls, location, stickers/GIFs/emoji.
- **qa-settings** — settings, privacy & security (passcode, hidden chats, sessions), customization (themes, chat look, night mode, launcher icons, app icon), accounts, groups/channels admin, contacts, profile, language, power saving, storage, proxy, updates.
- **fixer2** — fixes wave5-bugs.md items as they arrive.
- **antivirus** — research + written proposal only (`qa/antivirus-proposal.md`), no code.
