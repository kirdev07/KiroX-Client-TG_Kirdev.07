# How to port a board (for porter agents)

Read `qa/README.md` first (ownership, build, devices). Then:

1. Read the mockup `design-reference/kirox-v3/<Board>.dc.html` completely: markup (inline styles = the spec), the
   `<style>` keyframes, and the JS class at the bottom (state, data, what every tap does).
2. Write `app/src/main/java/com/kirox/app/ui/v3/screens/<section>/<Board>Screen.kt`:
   - `@Composable fun <Board>Screen(ctx: KxScreenCtx)` — ctx gives store, back(), open("BoardName"), openChat(id), toast(text), arg.
   - Register it in YOUR section's `screens/<section>/Registry.kt`: `"<Board>" to { ctx -> <Board>Screen(ctx) }`.
   - px → dp 1:1, font px → sp 1:1, same colors (use `Kx.*` tokens from `ui/v3/theme/KxTheme.kt` when equal, raw `Color(0xFF…)` otherwise),
     radii, gaps, paddings, weights, letter-spacing, texts (exact Russian strings), icon names
     (`ms` = outlined look → `Icons.Outlined.*`; `ms fill` → `Icons.Rounded.*`/`Icons.Filled.*`).
   - CSS gradients with an angle: `cssAngleStart/End(deg, size)` (components/KxBasics.kt). Glows: `KxGlow(diameter, x, y, alpha, alignEnd)`.
   - Reuse components: KxIconButton (glass=true for the bordered round buttons), KxChip, KxBadge, KxAvatar(colorIndex), KxSwitch(width/height/knob/colors per board),
     KxButton, KxTopBar (sub-screen header), KxLine, kxClick, kxRise(delay), topHighlight, dashedBorder, KxTypingDots, LocalKxToast.
   - Animations: keyframes → Compose (`Animatable`, `animate*AsState`, `rememberInfiniteTransition`, Canvas). Decorative ones run 2 times then stop
     (as in the mockup after the fixes); process ones (spinners, equalizers, recording) loop. Respect `LocalKxReducedMotion`.
   - Status bar: top content uses `statusBarsPadding()`; bottom bars `navigationBarsPadding()`.
   - Behaviour = the JS: toggles, sheets (radius 30 top, solid, no glass), confirm dialogs for dangerous actions, toasts, undo.
   - Data: use `ctx.store` where it already has what you need (KxModels/KxStore). Board-specific data the store doesn't have:
     keep it as demo constants in your screen file marked `// demo: needs store` and add a line to `qa/tg-tasks.md`
     (`- [ ] **designer** <Board>: needs <field/action>`). Do NOT edit ui/v3/model/*, KxApp.kt, KxRegistry.kt or other sections' files.
   - Honest UI: no fake promises (the boards were already cleaned of them); Premium-only things carry `KxPremiumChip`.
3. Compile after every 2–3 boards: `JAVA_HOME="$LOCALAPPDATA/Programs/Eclipse Adoptium/jdk-21.0.9.10-hotspot" ./gradlew :app:compileDebugKotlin --offline -q`.
   One Gradle at a time — if locked, wait and retry. Fix only your own errors.
4. Append to `qa/status.md` after each board: `- HH:MM porter-<section>: <Board> done`.
5. Watch `qa/ui-tasks.md` for lines about your boards (the UI tester reviews them) and fix them.
6. No device installs (the UI tester does that), no commits, no secrets, no real accounts.
