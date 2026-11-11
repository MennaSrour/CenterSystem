# Dashboard presentation and localization

`DashboardScreen(state, onEvent)` renders caller-owned state and emits typed events.
`DashboardState.language` defaults to Arabic. The screen provides both
`LocalAppLanguage` and the matching `LocalLayoutDirection`: Arabic uses RTL, English
uses LTR. `DashboardEvent.LanguageSelected` delegates the choice to the owner.

The dashboard catalog is a portable Kotlin map in `DashboardStrings.kt`, backed by
`core:ui`'s `StringResources` and `LocalizedText`. Every key has Arabic and English
text. Labels, placeholders, status text, accessibility descriptions, table headers,
and demo messages are single-language. Language names in the toggle intentionally
remain “عربي” and “English” so either choice is recognizable.

`desktopApp/App.kt` owns the selected language for the running app and passes it to
`DashboardRoute`. The demo route localizes the user/date/messages without rebuilding
its editable state, so toggling preserves the entered student code. It dismisses an
old-language snackbar when switching. The activity table chooses either the Arabic
name or English name, and retains LTR direction for IDs and times.

The sidebar remains physically left in both modes; content order, alignment, and
text direction follow the selected locale. The shared heading and navigation-item
APIs now choose one localized label rather than rendering paired languages. Other
feature-specific resource catalogs are outside this dashboard migration.

Future domain-backed callers must supply names, dates, formatted values, and error
messages appropriate to the active language; the UI does not translate user data or
unknown backend messages. The portable string catalog validates required arguments
and fails visibly for missing keys instead of silently displaying another language.

The dashboard otherwise retains its loading/error/empty states and stateless check-in
contract. The app still supplies illustrative data and never saves attendance.

## Brand artwork

The shared `CenterOneBrand` no longer draws a numbered yellow circle. It accepts a
`Painter` (directly or through `LocalCenterOneLogo`) and renders it as an `Image` in
an 88 dp box using `ContentScale.Fit`, with one localized brand name.

The logo shown inline in the chat was not exposed as a downloadable or mounted file
in this environment. Its original pixels have therefore not been recreated or
substituted. The exact artwork still needs to be supplied as a PNG/JPG file and saved
as `core/ui/src/commonMain/composeResources/drawable/center_one_logo.png` (or `.jpg`).
Once supplied, load the generated drawable with Compose `painterResource` inside the
core branding integration. Until then the brand is text-only; no fake image is bundled.

## Validation

```sh
./gradlew ktlintCheck allTests :desktopApp:desktopJar
```

Tests cover catalog completeness, locale selection, safe string interpolation,
dynamic toggle/direction changes, input preservation, single-language activity names,
and supplied-logo painter rendering. Headless snapshots cover Arabic and English.
