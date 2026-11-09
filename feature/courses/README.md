# Courses & Groups presentation

`CoursesScreen(state, onEvent)` is a stateless RTL feature. It reuses `CenterOneTheme`
from the app and shared `core:ui` components. It has no domain/data, DI, persistence,
platform, or cross-feature implementation dependency. Shared presentation sources
live in `commonMain`; the desktop target matches the existing project.

## UI and state

The screen uses one lazy adaptive grid for its full-width heading, three statistic
cards, filters/action bar, and group cards. Group items have stable IDs. Wide layouts
show three columns, medium layouts two, and narrow content one. Below 1000 dp the
canvas scrolls horizontally; the sidebar stays physically left in RTL.

`CourseCard` receives a group, display labels, menu expansion, and callback. The kebab
anchor is physically top-right in RTL. Edit/deactivate/delete actions emit group IDs;
deactivation is disabled for an already inactive group. Subject/grade, teacher/avatar,
schedule tags, and occupancy reuse shared primitives. Progress is clamped for display,
including zero capacity, while the numeric label retains caller-provided counts.

`CoursesState` owns snapshots, stage/search filters, expanded group menu ID, and a
single `CoursesModal` (closed, form, or deletion). Local filtering matches subject or
teacher name. Unknown teacher/grade IDs are displayed as unavailable; no substitute
valid-looking IDs are manufactured. Remote pagination/search should be handled by
the future owner rather than filtering an incomplete page locally.

## Forms and dialogs

`GroupFormModal` uses the shared native modal and a two-column body (920 dp maximum).
Subject/teacher/grade/system appear on the right; price, commission, capacity, and room
appear on the left. Narrow bodies stack. `GroupFormMode` distinguishes adding from
editing with the actual group ID. Commission defaults to the editable string `70`.

`GroupScheduleEditor` renders a bounded lazy list of caller-owned schedule rows.
Day/from/to, expansion, errors, additions, and removals are events. Stable slot IDs
survive edits/reordering. Rooms and days are explicit options; preview rooms cover 1–8.
Times/numeric fields use LTR paragraph direction inside the RTL layout.

`DeleteConfirmationModal` uses a shared small confirmation surface with a warning
icon and Accent Pink destructive action. The screen supplies the full dimmed scrim;
native dialogs own modal focus. Submitting/deleting states disable actions and ignore
dismissal. Only visual scroll/focus/animation state is local to Compose.

The caller must validate money, commission range, capacity, required references,
HH:mm values, time order, schedule conflicts, and authorization. It must guard duplicate
requests/stale lookups and provide unique group/teacher/grade/room/slot IDs. The UI
never computes payouts, generates business IDs, saves groups, or deletes records.
The owner should close card menus when opening a modal and keep only one form dropdown
or day menu expanded.

## Interactive fixture and shared components

`CoursesPreviewData` provides six example groups, teachers, grades, schedules, rooms,
and sample summary values (18 groups / 15 active / 12 teachers), which illustrate the
screenshot rather than aggregate the six-row fixture. `desktopApp/CoursesRoute.kt`
owns interactive demo state and creates unique draft slot IDs. Save, stop, and delete
report that nothing was persisted. This wrapper is the stateful app boundary; the
feature remains state/callback driven and ready for a lifecycle-owned domain adapter.

Dashboard, Students, and Financials Courses links open this screen. New reusable
capacity meter, destructive button, and confirmation surface live in `core:ui`.

## Checks

```sh
./gradlew ktlintCheck allTests :desktopApp:desktopJar
```

Tests cover filters, capacity display boundaries, fixture edit mapping, controlled
kebab menus and stable group IDs, schedule edit/add/remove events, busy controls,
deletion confirmation, and headless wide/narrow/empty screens and modal bodies.
Rendering artifacts live in `build/screenshots` (ignored by Git). Native dialog
focus/Escape and Windows packaging are not exercised by headless tests.
