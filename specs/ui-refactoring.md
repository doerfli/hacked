# Spec — UI refactoring to Jetpack Compose + Material 3

**Status:** proposed, not started · **Date:** 2026-09-03 · **Baseline:** v3.9.0 (121), branch `feature/refactoring-ui`

Mockups — all four screens in light and dark, palette options, icon exploration:
https://claude.ai/code/artifact/d79fb5e6-67cd-4e8d-8816-134477536825

## Context

The app is a Have I Been Pwned breach checker (`li.doerf.hacked`). Its UI is the original 2015-era AppCompat/XML stack: 13 layout XMLs, 6 fragments, 2 RecyclerView adapters, and a navigation graph, with a small amount of Compose already grafted in (`ui/composable/Breach.kt`, `ComposeView` in two layouts). It builds against compileSdk 37 but renders like an app from a decade ago.

Concretely, the analysis found:

- **Dark mode is not merely absent, it is impossible.** The theme is pinned to `Theme.AppCompat.Light.NoActionBar` (`res/values/styles.xml`), there is no `values-night`, and text colour is hardcoded to `@android:color/black` in `card_breached_site.xml`, `card_breached_site_compact.xml` and `fragment_account_details.xml`, plus `Color.Gray` / `Color.Red` inside `Breach.kt` and `Color.LightGray` dividers in `AccountDetailsFragment.kt`. Adding `values-night` alone would leave black text on a dark ground.
- **The overview cannot scroll.** `fragment_overview.xml` nests three fragments in a ConstraintLayout at fixed heights (`100dp` passwords, `150dp` breaches). Content clips on small screens and at large system font scales with no way to reach it.
- **Two image loaders ship at once** for the same breach logos: Picasso in `BreachedSitesAdapter`, deprecated `accompanist-coil 0.15.0` in `Breach.kt`.
- **State is expressed through visibility flags.** `PwnedPasswordFragment` drives its result through four independent `View.GONE` toggles, so contradictory states are representable. `AccountDetailsFragment` toggles a nine-view ConstraintLayout `Group`. `BreachedSitesAdapter` stores expansion state on the Room entity (`site.detailsVisible`) and refreshes with `notifyDataSetChanged()`.
- **Deprecated APIs throughout**: `setHasOptionsMenu`, `onCreateOptionsMenu(Fragment)`, `onActivityCreated`, framework `PreferenceFragment`, `LocalBroadcastManager`, `resources.getColor(int)`, `resources.configuration.locale`.

Intended outcome: a single Compose + Material 3 UI layer with one theme source, a dark theme that follows the system with an in-app override, a fixed brand palette derived from the existing green (no dynamic colour — status colour carries meaning in a security app), and the elimination of the XML/fragment/adapter layer.

## Decisions

| | |
|---|---|
| Scope | Full Compose migration, no XML layouts left |
| Dark mode | Follows system, with a Light / Dark / System override in Settings |
| Dynamic colour | No — fixed brand palette. Status colour carries meaning here |
| Palette | **Verdant in light, Deep teal in dark** (options A and D in the mockup) |
| Third tab name | **Leaks**, not Breaches |
| Long account names | End-truncate for now; known limitation, accepted |
| App icon | **Receding** — password characters shrinking out of the field (option 3c) |

## Approach

Sequenced so the app compiles and runs after every step. The theme lands first so each ported screen is verifiable in dark mode as it arrives.

### 1. Dependencies and theme foundation

`app/build.gradle` — add:

```
implementation platform("androidx.compose:compose-bom:$compose_bom_version")
implementation "androidx.compose.material3:material3"
implementation "androidx.compose.material:material-icons-extended"
implementation "androidx.compose.ui:ui-tooling-preview"
implementation "androidx.navigation:navigation-compose:$nav_version"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version"
implementation "androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version"
implementation "io.coil-kt:coil-compose:2.7.0"
debugImplementation "androidx.compose.ui:ui-tooling"
```

Remove: `accompanist-appcompat-theme`, `accompanist-coil`, `picasso`, `cardview`, and the explicit `androidx.compose.compiler:compiler` artifact (the Compose compiler Gradle plugin, already applied, supersedes it). Move version literals into `gradle.properties` alongside the existing ones. `kotlinOptions.jvmTarget` must go to 11+ for current Compose — bump `sourceCompatibility` / `targetCompatibility` / `jvmTarget` to 17.

New package `app/src/main/kotlin/li/doerf/hacked/ui/theme/`:

- `Color.kt` — the light and dark role values. Note the two schemes come from **different palettes**: light is verdant (`primary #2E6B34`, `surface #F9FBF4`), dark is deep teal (`primary #4FD8DF`, `surface #0E1414`). This is deliberate — teal holds up better against a near-black ground, and in dark mode it keeps the primary clear of the green "clean" status, which verdant did not.
- `StatusColors.kt` — `data class StatusColors(breached, acknowledged, clean, unchecked)` plus `val LocalStatusColors = staticCompositionLocalOf { … }`. This replaces the four `@color/account_status_*` entries and every `resources.getColor()` call in `AccountsAdapter` and `AccountDetailsFragment`. Per-theme values: light `breached #B3261E`, `acknowledged #D69A00`, `clean #2E6B34`, `unchecked #767D73`; dark `#FFB4AB`, `#F0C048`, `#8FD695`, `#8E958A`. Status must never be the only signal — every row that uses one also states it in text.
- `Type.kt` — Material 3 `Typography`, replacing the `TextTitle` / `TextBody` / `TextNotice` XML styles.
- `Theme.kt` — `HackedTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` providing both `MaterialTheme` and `LocalStatusColors`, with `MaterialTheme.statusColors` as a convenience accessor.

Keep a minimal `AppTheme` in `values/styles.xml` parented to `Theme.Material3.DayNight.NoActionBar` (the activity window still needs a theme), and add `values-night/` for the window background only.

### 2. Theme preference

`SettingsFragment.java` currently extends the **framework** `android.preference.PreferenceFragment`, which is deprecated and cannot host a modern preference. Port it to `androidx.preference.PreferenceFragmentCompat` (the `preference-ktx` dependency is already present) as Kotlin, keeping the existing sync-preference logic and the `version` / `device` / `clean_token` handlers verbatim.

Add to `res/xml/preferences.xml` a new `PreferenceCategory` with a `ListPreference` (System / Light / Dark) plus the matching entries in `values/arrays.xml` and strings in `values/strings.xml`, `values-de`, `values-fr`. Apply it via `AppCompatDelegate.setDefaultNightMode(...)` both on change and at startup — startup application belongs in `HackedApplication.onCreate()`, next to the existing initialisation.

Note: `activity_settings.xml` and `SettingsActivity.java` can stay as-is through this step; fold Settings into the Compose nav graph only if it proves cheap in step 4, otherwise leave it.

### 3. Single Compose activity

`NavActivity.kt` becomes:

```kotlin
setContent { HackedTheme { HackedApp() } }
```

with a `NavHost` (navigation-compose) and a `NavigationBar` carrying three destinations — Accounts, Passwords, Leaks — replacing the fixed-height nesting in `fragment_overview.xml` entirely. `enableEdgeToEdge()` is already present; keep it and consume insets via `Scaffold`.

The RxJava `PublishProcessor<NavEvent>` bus (`HackedApplication.navEvents`, consumed in `NavActivity.setupNavigation`, produced in both adapters and four fragments) is replaced by direct `NavController` calls passed as lambdas. Delete `util/NavEvent.kt`, the `navEvents` field on `HackedApplication`, and `res/navigation/nav_graph.xml`; drop the `navigation-safeargs` plugin once the generated `*Directions` classes have no callers.

First-use routing (`OverviewFragment.isFirstUse()`, reading `PREF_KEY_FIRST_USE_SEEN` and the account count) moves into the nav host's start-destination decision, backed by a small `AppViewModel`.

`FirstUseFragment` becomes a `FirstUseScreen` composable; its HTML-link body text (`firstuse_p4`) needs `AnnotatedString` handling rather than `LinkMovementMethod`.

### 4. Screens, one per commit

Each ported screen deletes its fragment, its layout XML and its adapter in the same commit. New package `ui/screens/`.

**AccountsScreen** — replaces `OverviewFragment`, `AccountsFragment`, `AccountsAdapter`, `fragment_overview.xml`, `fragment_accounts.xml`, `card_account.xml`. Status banner with the unresolved-breach count at the top; account rows carry a status dot plus a count badge (state is also in the supporting text, never hue alone — and the count appears once, in the badge); add-account moves from the inline `Group` toggle to a FAB opening a `ModalBottomSheet`, preserving the `MAX_ACCOUNTS = 50` check and the `AccountService.addAccount` call. Refresh keeps enqueuing `HIBPAccountCheckerWorker` and shows a `Snackbar` via `SnackbarHostState`.

Account names truncate at the end (`maxLines = 1`, `TextOverflow.Ellipsis`) for now. Known limitation, deliberately accepted: accounts sharing a long local part and differing only in domain will render identically in the list, and the full name is only readable on the detail screen. Revisit after the migration lands — middle ellipsis is the cheapest fix if it becomes a real annoyance.

Reuse as-is: `AccountViewModel` (`accountList`, `lastChecked`) — expose the existing `LiveData` through `collectAsStateWithLifecycle()`, no rewrite needed.

**AccountDetailScreen** — replaces `AccountDetailsFragment`, `fragment_account_details.xml`, and rewrites `ui/composable/Breach.kt`. The 10dp `Spacer` painted with `Color(statusColor)` becomes a 4dp leading edge on the card. The nine-view "what now?" `Group` becomes one expandable card. Data classes render as `AssistChip`s rather than a comma-joined string. Logos via `AsyncImage` (Coil). The acknowledge flow (`handleAcknowledgeClicked` → `setBreachAcknowledged` → `updateAccountIsHacked`) moves out of the fragment into a ViewModel unchanged in behaviour — it currently runs Room writes on `CoroutineScope(Job())`; move to `viewModelScope` + `Dispatchers.IO`. `deleteAccount()` and `resetAcknowledged()` currently use `runBlocking` on the main thread — fix while moving.

Reuse as-is: `BreachViewModel.getBreachList(accountId)`, `AccountHelper.updateBreachCounts`.

**PasswordScreen** — replaces `PwnedPasswordFragment`, `fragment_pwned_password.xml`. The four visibility toggles collapse into one sealed state: `Idle | Checking | Safe | Pwned(count) | Error`. The `LocalBroadcastManager` round-trip in `remote/pwnedpasswords/PwnedPassword.kt` is replaced by a suspending `check(password): Result` consumed by the ViewModel; the k-anonymity request logic itself is untouched. Result renders as a container-coloured card, not a full-bleed red/green strip. Keep `StringHelper.addDigitSeperator` and the existing `Analytics.trackCustomEvent` calls.

**LeaksScreen** — replaces `AllBreachesFragment`, `BreachesFragment`, `BreachedSitesAdapter`, `fragment_all_breaches.xml`, `fragment_breaches.xml`, `card_breached_site.xml`, `card_breached_site_compact.xml`, `hibp_info.xml`.

The tab is labelled **Leaks**, not Breaches: it shows HIBP's catalogue of breached sites — reference data — while the user's own exposure is reported on Accounts, and one label for both reads as the same thing. Needs a new `title_leaks` string in `values/strings.xml`, `values-de` and `values-fr`; `all_breaches` ("List of all breaches") stays for the screen title, `title_recent_breaches` becomes unused. Composable and file names follow the label (`LeaksScreen`, `ui/screens/leaks/`); Room entities and DAOs keep `BreachedSite` — the label is a UI decision, not a data-model one.

One `BreachRow` composable with an `expanded` flag replaces the `compactView` boolean and its two layouts. Expansion state moves off the Room entity (`BreachedSite.detailsVisible`) into `rememberSaveable` screen state — note this field may then be removable from the entity, which touches the Room schema; check `db/entities/BreachedSite.java` before deleting it, and leave it in place if it is persisted. Sorting moves from the overflow menu to visible `FilterChip`s bound to the existing `BreachedSitesViewModel.orderByName/Count/Date`. Search binds to `setFilter`. Counts use locale grouping (replacing `resources.configuration.locale`, deprecated) and tabular figures.

Reuse as-is: `BreachedSitesViewModel` in full, `AllBreachesFragment.reloadBreachedSites` (move to the ViewModel as a static-equivalent), `ui/HibpInfo.java` behaviour as a small composable attribution line.

### 5. App icon

Independent of the Compose work — can land at any point, including first.

The icon is **Receding**: three password asterisks on a field underline, each smaller than the last, the smallest in the accent colour and slipping past the end of the field. Draw it as a `VectorDrawable` on a 108×108 viewport with all artwork inside the central 72dp safe zone.

- New `res/drawable/ic_launcher_foreground.xml` (vector), replacing the `app_icon_foreground.png` in all five mipmap buckets — the PNGs then get deleted.
- `res/values/app_icon_background.xml` currently sets `#FFFFFF`; change to the icon's ink ground and add a `values-night` counterpart only if the launcher is found to respect it (most do not — verify before relying on it).
- Add a `<monochrome>` layer to both `mipmap-anydpi-v26/app_icon.xml` and `app_icon_round.xml` pointing at the same vector. The app currently has no monochrome layer, so themed icons on Android 13+ fall back to a flat generic tile.
- While here: `FirebaseMessagagingService.java:78` uses `android.R.drawable.sym_def_app_icon` and `HIBPAccountResponseWorker.kt:197` uses `android.R.drawable.ic_dialog_info` as notification small icons. Both are platform placeholders. Add `res/drawable/ic_notification.xml` — the same glyph as a solid white silhouette, since Android masks notification icons to alpha — and point both at it.
- Regenerate the 512×512 Play Store icon from the same vector.

### 6. Sweep

Remove `buildFeatures.viewBinding`, the `cardview` / `recyclerview` / `legacy-support-v4` dependencies, `res/navigation/`, `RecyclerViewHolder.kt`, remaining `values/dimens.xml` card metrics and `values/styles.xml` text styles, `drawable/section_header.xml` and `chevron_right_black.xml`, and every remaining hardcoded colour in `values/colors.xml` (keep only what notifications still need — `NotificationHelper` / `OreoNotificationHelper` reference `bg_notification`; verify before deleting).

## Critical files

| Path | Action |
|---|---|
| `app/build.gradle`, `gradle.properties` | deps, jvmTarget 17 |
| `app/src/main/kotlin/li/doerf/hacked/ui/theme/*` | new — Color, Type, Theme, StatusColors |
| `app/src/main/kotlin/li/doerf/hacked/activities/NavActivity.kt` | rewrite to `setContent` + NavHost |
| `app/src/main/kotlin/li/doerf/hacked/ui/screens/*` | new — four screens |
| `app/src/main/kotlin/li/doerf/hacked/ui/fragments/*` (6 files) | delete as each screen lands |
| `app/src/main/kotlin/li/doerf/hacked/ui/adapters/*` (3 files) | delete |
| `app/src/main/java/li/doerf/hacked/ui/fragments/SettingsFragment.java` | port to PreferenceFragmentCompat (Kotlin) |
| `app/src/main/kotlin/li/doerf/hacked/remote/pwnedpasswords/PwnedPassword.kt` | broadcast → suspending result |
| `app/src/main/res/layout/*` (13 files), `res/navigation/nav_graph.xml` | delete |
| `app/src/main/res/values/{colors,styles,dimens}.xml` | reduce to what notifications need |
| `app/src/main/res/xml/preferences.xml`, `values*/strings.xml`, `values/arrays.xml` | theme preference + translations |
| `app/src/main/res/mipmap-anydpi-v26/app_icon*.xml`, `values/app_icon_background.xml` | new vector foreground + monochrome layer |
| `app/src/main/res/mipmap-*/app_icon_foreground.png` (5 buckets) | delete, replaced by vector |

ViewModels (`AccountViewModel.java`, `BreachViewModel.java`, `BreachedSitesViewModel.kt`), the Room layer, the workers, `AccountService`, `AccountHelper`, `Analytics`, `RatingHelper` and `SynchronizationHelper` are **not** part of this refactor and stay as they are.

## Verification

After each step:

1. `./gradlew assembleDebug` — must stay green; the build currently succeeds, so any new failure is from this work.
2. `./gradlew lint` — watch specifically for new hardcoded-colour and deprecation warnings, and confirm the old ones drop.

End-to-end on a device or emulator (`startEmulator.sh` in the repo root), in **both** themes, toggled through the new Settings preference and again through the system setting to confirm the override wins:

3. First launch with no accounts → first-use screen → add an account → lands on Accounts.
4. Accounts tab: add via FAB, refresh from the overflow (snackbar appears), tap through to detail, acknowledge a breach (status dot goes red → amber, badge count drops), delete the account.
5. Passwords tab: check a known-pwned password (e.g. `password`) → count renders with separators; check a random string → safe state; enable airplane mode → error state, and confirm only one of the three can be on screen.
6. Leaks tab: search filters the list, all three sort chips reorder it, a row expands and collapses, logos load.
7. Rotate on each screen — expansion, search text and result state must survive (this is the `rememberSaveable` check).
8. Set system font size to the largest setting and re-check Accounts and Leaks — this is the specific regression the current fixed-height overview has.
9. Confirm background sync still schedules after changing the sync preferences (`SynchronizationHelper.scheduleSync` is called from the ported settings screen).
10. Icon: install and check the launcher tile against a round mask, a squircle mask, and with themed icons switched on in Android 13+ settings — the monochrome layer must resolve to a legible silhouette, not a filled square. Trigger a breach notification and confirm the small icon is the new glyph rather than the platform placeholder.
