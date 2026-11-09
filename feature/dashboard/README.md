# Dashboard

`DashboardScreen(state, onEvent)` renders caller-owned presentation state. The feature
has no domain/data, DI, navigation controller, or platform dependencies. State and typed
events are in separate files; screen widgets reuse `:core:ui` components.

The root provides RTL. The last child in its RTL row is the sidebar, which puts it at
the physical left as in the supplied design. Below a 1000 dp viewport, the desktop
canvas scrolls horizontally. Statistics use two or four columns based on available
content width. The main content scrolls vertically; the activity list is lazy with a
bounded viewport and stable activity IDs (callers must supply unique IDs).

`DashboardContent` supports loading, failure/retry, and ready/empty content. Check-in
supports idle, submitting, failure, and success. The caller owns code validation,
submission concurrency, persistence, formatting, and all navigation/effects. Barcode
scanners that type a code and Enter use the same submit event as the button.

`desktopApp/DashboardRoute.kt` is an explicitly labeled demo boundary with screenshot
sample data. It does not write attendance. Replace its local state and handler with
state collected from your lifecycle-owned domain-backed state holder when connecting
attendance and dashboard queries. Guard duplicate submissions in that state holder
as well as in the UI; map domain results into these presentation models there.

No logo asset was supplied separately, so the sidebar uses a typographic brand mark.
The English action reports unavailable until English resources and locale ownership
are implemented. Other unfinished destinations report unavailable rather than
pretending to navigate or log out.

Checks:

```sh
./gradlew :core:ui:ktlintCheck :feature:dashboard:ktlintCheck \
  :feature:dashboard:desktopTest :desktopApp:desktopTest :desktopApp:desktopJar
```
