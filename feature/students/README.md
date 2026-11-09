# Students Management presentation

The existing `feature/students` placeholder is now a desktop KMP presentation module.
It depends only on `core:ui`, Compose resources, and icons; it has no domain/data,
DI, repository, platform, or persistence dependency.

## Public entry points

- `StudentsScreen(state, onEvent, modifier)` renders the RTL directory and, when
  `state.addStudent` is non-null, the dark scrim and native `AddStudentModal`.
- `StudentsDataGrid(students, grades, onEvent, modifier)` reuses the shared generic
  lazy table, avatars, tags, and accessible icon buttons. Edit/delete emit stable
  row IDs; they never mutate the supplied list.
- `AddStudentModal(state, onEvent, modifier)` renders a shared centered dialog.
  At desktop widths name/ID occupy the right column and grade/system/phone occupy
  the left. Narrow modal bodies stack the fields. Phone and code text use LTR
  paragraph direction within the RTL layout to preserve international numbers.

Wrap entry points in the existing `CenterOneTheme`; the app does this once at its
root. Arabic/English labels live in Compose resources and use the bundled Almarai
font through the shared typography.

## State and events

`StudentsState` owns the summary, sample rows, grade catalog, stage/search/grade
filters, dropdown expansion, and optional add form. Its derived display filter
matches Arabic/English names and student codes. Grade options follow the selected
stage; unknown grade IDs are displayed explicitly as unavailable, never mapped to
an invented grade or stage.

`AddStudentState` owns all form fields, errors, dropdown expansion, system selection,
submission availability, and progress. All interactions emit `StudentsEvent` values.
Only visual scroll/focus/animation state is retained by Compose. The caller must
validate/authorize registration, supply/generate the correct code, guard duplicate
submissions and stale lookups, and handle navigation and row actions. The UI disables
form actions and ignores dialog dismissal during submission; the owner must guard
those operations too.

`StudentsPreviewData` provides four explicit example rows and screenshot summary
counts (248/95/81/72). These aggregates illustrate the design and do not describe the
complete four-row fixture. Result count reflects the visible fixture rows. Pagination
and remote lookup are not implemented; future paginated data should be filtered by
the owning query instead of filtering an incomplete page locally.

## App wiring and shared components

Dashboard → Students and Financials → Students open `desktopApp/StudentsRoute.kt`.
That boundary owns an interactive fixture: tabs, search, filters, and form edits work;
confirmation reports that no student was saved. Edit/delete report an unavailable
demo action and do not change data.

Pill tabs, tags, icon buttons, and a reusable page header live in `core:ui`. Financials
also uses the page header. The shared modal supports configurable maximum width
(defaults unchanged); shared text fields support caller-provided text style for bidi
text while retaining existing defaults.

## Validation

```sh
./gradlew ktlintCheck allTests :desktopApp:desktopJar
```

Tests cover combined filters, unknown grades, immutable snapshots, stateless form
edits and actions, submission-disabled controls, row-action IDs, and rendering at
wide/narrow widths with two-column/stacked modal bodies. Screenshots are written to
`build/screenshots` (ignored by Git). Native dialog focus/Escape and Windows packaging
are not exercised by the headless tests.
