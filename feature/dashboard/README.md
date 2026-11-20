# Reception dashboard

`DashboardScreen(state, onEvent)` consumes an immutable snapshot and emits typed
events. It displays today's attendance and treasury, a student search, the selected
student's actions, and shortcuts for adding a student, collecting a subscription,
and recording an expense.

`DashboardRoute` observes SQLDelight-backed attendance, treasury, enrollments and
monthly balances. Today's treasury shows inflow minus outflow. The scanner submits
with Enter and uses the live student code; name searches allow selection when
several results match. Attendance uses the selected active group and rejects
duplicates. The three shortcuts open the live registration, payment and expense
forms.

The dashboard follows `CenterOneLocalization` in Arabic and English. The desktop
app owns the selected language, and changing it preserves the current search.
The sidebar stays physically left in both languages. Its shared main navigation
contains Reception, Students, Teachers, Courses, and Treasury, with Settings in
the footer. The shared brand displays the localized Center One name and supports
optional artwork through `LocalCenterOneLogo`.

Run `./gradlew :feature:dashboard:desktopTest :desktopApp:desktopTest` for the
search, localization, action dispatch, navigation, and rendering tests.
