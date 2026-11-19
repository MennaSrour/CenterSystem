# Reception dashboard

`DashboardScreen(state, onEvent)` consumes an immutable snapshot and emits typed
events. It displays today's attendance and treasury, a student search, the selected
student's actions, and shortcuts for adding a student, collecting a subscription,
and recording an expense.

`DashboardRoute` owns the demo snapshot and resolves searches against one sample
student. Try barcode `10234` or either form of Youssef Ahmed's name. Press Enter
after scanning with a keyboard-emulating barcode reader. Attendance shows demo
feedback, and the other actions open existing demo forms. No data is persisted.

The dashboard follows `CenterOneLocalization` in Arabic and English. The desktop
app owns the selected language, and changing it preserves the current search.
The sidebar stays physically left in both languages. Its shared main navigation
contains Reception, Students, Teachers, Courses, and Treasury, with Settings in
the footer. The shared brand displays the localized Center One name and supports
optional artwork through `LocalCenterOneLogo`.

Run `./gradlew :feature:dashboard:desktopTest :desktopApp:desktopTest` for the
search, localization, action dispatch, navigation, and rendering tests.
