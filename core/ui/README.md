# Center One design system

This desktop KMP library owns shared colors, Arabic typography, surfaces, inputs,
buttons, navigation items, avatars, metric cards, bilingual headings, and status
badges. Features depend on `:core:ui`; this library has no feature or domain dependency.

## Arabic font resources

Almarai Regular (400) and Bold (700) are bundled at:

- `src/commonMain/composeResources/font/almarai_regular.ttf`
- `src/commonMain/composeResources/font/almarai_bold.ttf`

These are official Google Fonts files, distributed under the SIL Open Font License;
see `licenses/Almarai-OFL.txt`. No installed system font or runtime download is required.
Source: https://github.com/google/fonts/tree/main/ofl/almarai

The module applies the Compose Multiplatform and Compose compiler plugins and depends
on `compose.components.resources`. Its `compose.resources.packageOfResClass` is
`center.ui.resources`. Gradle generates `Res.font.almarai_regular` and
`Res.font.almarai_bold`; never handwrite those accessors. `ui/theme/Type.kt` loads
both with `org.jetbrains.compose.resources.Font`, supplies their correct weights,
and applies the family to every Material 3 typography role.

Wrap your app or previews in `CenterOneTheme`. If substituting Cairo, place licensed
static regular/bold TTF files in the same resource directory, update the two generated
font references in `Type.kt`, and retain the font's license. Do not load fonts using
Android-only resource APIs or absolute desktop filesystem paths.

Use start/end padding and alignment. RTL belongs at the app/screen boundary rather
than inside individual primitives. The dashboard supplies RTL at its root and limits
LTR overrides to numeric IDs and times.

Financials also uses the shared controlled dropdowns, selection rows, segmented
choices, tabs, generic lazy table, info panels, sidebar/branding, outlined buttons,
and bounded modal surfaces. Dropdown expansion and selected IDs belong to callers;
close a dropdown after handling its selection event. `CenterOneTextField` retains
its single-line defaults and additionally supports labels, read-only fields, and
multiline notes.

## Localization and branding

`CenterOneLocalization(language)` supplies `LocalAppLanguage` and the corresponding
layout direction without changing the process/system locale. `StringResources<Key>`
selects `LocalizedText` entries and interpolates indexed string arguments. Missing
keys/arguments fail explicitly; inserted arguments are never interpreted again.
The dashboard is the first migrated feature; other resource catalogs can adopt the
same provider separately.

`CenterOneLanguageToggle` is caller-controlled. `BilingualHeading` and navigation
items retain their existing API but display one selected-language label.
`CenterOneBrand` uses a supplied `Painter` via its `logo` parameter or
`LocalCenterOneLogo`, with `ContentScale.Fit` and localized accessibility text.
No number-circle placeholder is drawn. The original logo file is still required
before a drawable can be packaged; see the dashboard README.
