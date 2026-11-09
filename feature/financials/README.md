# Financials & Treasury presentation

`FinancialsScreen(state, onEvent)` renders an RTL financials screen using `core:ui`
and the app's `CenterOneTheme`. This feature has no domain/data, DI, navigation,
platform, persistence, or file-picker dependency. The desktop target matches the
existing project; its presentation sources live in `commonMain`.

## State ownership

`FinancialsState` contains formatted summary values, income/expense snapshots, the
selected log and query, and one `FinancialsModal` (closed, receive payment, or record
expense). The state computes a local case-insensitive display filter for the active
log. For future server pagination/search, supply the filtered page from the owner
instead of filtering a partial dataset locally.

Every form value, selected ID, dropdown expansion, validation message, confirmation
availability, and submitting flag comes from the caller. `FinancialsEvent` routes
edits, selections, dismissal, confirmation, attachment requests, and navigation back
to that owner. Compose retains only visual scroll/focus/animation state.

The caller must:

- Supply unique record/option IDs and preformatted amounts, dates, dues, and shares.
- Validate and authorize transactions; never trust `canConfirm` as business validation.
- Recompute or clear a financial split when inputs change. The UI does no arithmetic.
- Guard duplicate submissions and stale student/class/teacher lookups.
- Set `isSubmitting` while work is active. Inputs, confirmation, cancellation, and
  dismissal are disabled during submission; handle equivalent guards in the owner.
- Handle `AttachReceipt` at the platform boundary, then supply `attachmentName`.
  The feature never reads a filesystem path or invokes a picker.

## Shared components and dialogs

The screen reuses metric cards, bilingual headings, inputs, buttons, badges, avatars,
and navigation items. New dropdown, selection row, segmented choice, tabs, generic
lazy table, info panel, sidebar/branding, and modal components live in `core:ui`.
The existing dashboard now reuses the sidebar and branding components too.
`CenterOneTextField` supports read-only and multiline modes without changing its
existing defaults. Theme container roles use the brand palette for selected controls.

`ReceivePaymentModal` and `RecordExpenseModal` use a native Compose `Dialog` for modal
focus and Escape handling. `FinancialsScreen` draws the dark scrim over its full
background. Each modal has a separately renderable body for headless tests; its
surface is bounded to 600 x 840 dp and scrolls when constrained. Operational expenses
hide teacher selection and dues. Submitting dialogs ignore dismissal requests.

At wide desktop sizes statistics occupy four columns. Smaller content widths use
two columns; below a 1080 dp viewport the canvas scrolls horizontally. The sidebar
remains at the physical left in RTL. Table rows render lazily with a bounded viewport.

## Interactive fixture

`FinancialsPreviewData` explicitly supplies screenshot-inspired dummy data.
`desktopApp/FinancialsRoute.kt` owns an interactive in-memory fixture. Open it using
the dashboard's Payments action. Confirmation displays that nothing was saved;
attachment requests explain that platform integration is pending. Switching the
expense type shows/hides teacher fields without calling domain code.

The existing Billing workspace remains available. No financial transaction,
receipt upload, WhatsApp message, or logout occurs in this UI-first implementation.

## Validation

```sh
./gradlew ktlintCheck allTests :desktopApp:desktopJar
```

Feature tests cover log filtering and snapshot immutability, form edit/confirmation
events, caller-controlled expense type, disabled submitting actions, and headless
rendering of both logs, empty/narrow layouts, and all three modal-body variants.
Rendering artifacts are written to `build/screenshots` (ignored by Git). Native
window focus/Escape and file picking are not exercised by these headless tests.
