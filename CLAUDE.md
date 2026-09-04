# Hacked?

Android app (`li.doerf.hacked`) that checks accounts and passwords against Have I Been Pwned.

## Stack

- Kotlin + Java (mixed; new code is Kotlin), AGP 9.4.0, Gradle 9.7.1, Kotlin 1.9.24
- Jetpack Compose + Material 3 (BOM `compose_bom_version` in `gradle.properties`) — the UI layer is being migrated to Compose on `feature/ui-redesign-v4`; see "UI redesign" below
- Room for local storage (`db/`), RxJava2 (`room-rxjava2`) still present for some DAOs, newer code uses `LiveData`/`StateFlow`
- WorkManager for background sync (`remote/hibp/*Worker.kt`)
- `compileSdk`/`targetSdk` 37, `minSdk` 23, `jvmTarget`/source/target compatibility 17 (app's compiled bytecode target — separate from the Gradle daemon's own runtime, pinned to JDK 21 via `gradle/gradle-daemon-jvm.properties`, which CI's `setup-java` step matches directly)

## Build & verify

```
./gradlew :app:assembleDebug   # compile check — do this after every non-trivial change
./gradlew lint                 # watch for new hardcoded-colour / deprecation warnings
```

No unit or instrumentation tests exist in this repo (`app/src/test`, `app/src/androidTest` are empty). `startEmulator.sh` at repo root launches an emulator for manual device testing — there is no way to verify Compose UI changes without a device/emulator; always ask the user to rebuild and check on-device rather than assuming a compile pass means the UI looks right.

## UI redesign (in progress, branch `feature/ui-redesign-v4`)

Migrating from AppCompat/XML/Fragment/RecyclerView-Adapter to Compose + Material 3, dark mode support, and a new "Receding" app icon. Full plan and design rationale: `specs/ui-refactoring.md`. That spec links a Claude-generated mockup artifact (all four screens, light + dark); a local copy of its rendered HTML/CSS is saved at `specs/ui-refactoring-mockup.html` — read it directly (it's plain HTML/CSS, open in a browser or just grep/read it) rather than re-fetching the artifact URL. It's the source of truth for exact colors, spacing, and iconography (e.g. bottom nav uses Person/Lock/split-Shield glyphs, not Material's default AccountCircle/Password/Warning icons).

- `ui/theme/` — `Color.kt` (verdant light palette / deep teal dark palette, deliberately different hue per theme, not just lightness), `StatusColors.kt` (`LocalStatusColors` — breached/acknowledged/clean/unchecked, status color must never be the sole signal, always paired with text), `Theme.kt` (`HackedTheme`), `Type.kt`
- `ui/screens/{accounts,passwords,leaks,firstuse,settings}/` — the Compose screens; `ui/HackedApp.kt` hosts the `NavHost` + bottom `NavigationBar` for the first four. `SettingsScreen` is not in that NavHost — it's launched from `AppOverflowMenu` as its own `SettingsActivity` (`setContent { HackedTheme { SettingsScreen(...) } }`), since threading it into the shared NavHost would require plumbing a navigation callback through every screen's overflow menu for one destination. Same pattern for any other Compose screen reached only via `startActivity` rather than the bottom nav.
- `ui/composable/` — shared pieces (`BreachCard`, `LabeledValue`, `HtmlLinkText`, `AppOverflowMenu`)
- Legacy Java ViewModels (`AccountViewModel`, `BreachViewModel`) are bridged into Compose via `observeAsState()` (needs `runtime-livedata`); new Kotlin ViewModels use `StateFlow` + `collectAsStateWithLifecycle()`. Don't rewrite the legacy ones just to switch bridging style — it's working, low-value churn.

### Compose gotchas hit during this migration (read before touching layout code)

- **Nested `Scaffold`s double up on inset padding.** `HackedApp`'s outer `Scaffold` has no `topBar`, so its default `contentWindowInsets` reserves status-bar space; every screen also has its own `Scaffold` + `TopAppBar` which reserves the *same* space again — this shows up as a big blank gap above the screen title. Fixed by setting the outer Scaffold's `contentWindowInsets = WindowInsets(0, 0, 0, 0)` and letting each screen's own `TopAppBar` be the only thing that consumes that inset.
- **Getting a fixed-width colored bar to span a sibling's real height took three attempts — none of the "obvious" ones work:**
  1. `Modifier.fillMaxHeight()` silently collapses to zero inside an unbounded-height parent. A `Row`/`Column` measured with unbounded height (e.g. as the content of a `Card` inside a `LazyColumn`) passes `Constraints.Infinity` for height to its children; `fillMaxHeight()` on a sibling then has no bound to fill against and renders at 0dp instead of erroring. (Rendered as an invisible 4dp × 0dp sliver.)
  2. `Modifier.height(IntrinsicSize.Min)` on the parent Row is the commonly-suggested fix, but it's only an *estimate* — it can under-measure once content wraps unpredictably (multi-line text, `FlowRow` chips), silently clipping later content. (This ate the Acknowledge button on some breach cards.)
  3. `Box` + `Modifier.matchParentSize()` on the bar looks like the right tool, but chaining `.width(4.dp)` after it does **not** carve out a narrow bar — `matchParentSize()` forces the child to exactly match the Box in *both* dimensions, ignoring any width/height modifiers chained on the same target either before or after it. (Tinted the entire card the status color.)
  - **What actually works:** `Modifier.drawBehind { drawRect(color = edgeColor, size = size.copy(width = edgeWidthPx)) }` on the Box that wraps the real content — it draws using that Box's own actual measured size at draw time, so there's no sibling-height-matching or intrinsic-estimation involved at all. See `ui/composable/BreachCard.kt`.
- Material3 `Card`/`Surface` clips its content to its own `shape` — a colored edge bar inside a `Card(shape = RoundedCornerShape(16.dp))` automatically gets rounded top/bottom corners for free; no manual clipping needed on the bar itself.
- Vector drawable icons: when the built-in Material icon (e.g. `Icons.Filled.Shield`) doesn't match the mockup's exact glyph (the mockup's Leaks icon is a *split* shield — half outline, half filled), port the mockup's raw SVG `pathData` string directly into a `res/drawable/*.xml` `<vector>` — Android's path parser accepts the same grammar as SVG path data, including the shorthand/relative forms Google's Material SVGs use, so it's a straight copy-paste rather than a translation. Reference via `painterResource()` and `Icon(painter = ..., contentDescription = ...)`.

## Icon design notes

The launcher/notification icon ("Receding" — password asterisks shrinking, smallest in accent color) is a hand-built `VectorDrawable`, not a generated asset. If touching `res/drawable/ic_launcher_foreground.xml` or `ic_notification.xml` again: keep each asterisk's stroke-width-to-arm-length ratio below ~1:2 with round line caps, or short thick strokes visually collapse into flower/blob shapes instead of reading as sharp asterisks — this took three iterative rounds of on-device screenshot feedback to get right. Notification small icons are alpha-masked by Android to a single system-tinted silhouette; embedded colors in `ic_notification.xml` are cosmetic-only in the editor preview.

## Working style notes for this repo

- The user tests exclusively on-device/emulator and reports back with screenshots — always ask them to rebuild and check after a UI change rather than declaring it fixed from a compile pass alone.
- When a UI complaint doesn't match what the code looks like it should already do (e.g. "the border isn't there" when the code clearly adds one), suspect a Compose layout/measurement bug before assuming the feature is unimplemented or asking for more screenshots — see the gotchas above for two real examples of exactly this.
