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
