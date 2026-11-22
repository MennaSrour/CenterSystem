# CenterSystem

Desktop-first Kotlin Multiplatform starter for Windows, using Compose Multiplatform,
Koin, SQLDelight (SQLite), Decompose, and ktlint. Requires JDK 21. The Gradle 8.14.3
wrapper is included; no global Gradle installation or Android SDK is required.

## Layout

```text
.github/workflows/ci.yml         Windows build, tests, style, installer artifacts
gradle/libs.versions.toml        Pinned plugins and library versions
gradle/wrapper/                  Gradle wrapper
settings.gradle.kts              Module graph and repositories
build.gradle.kts                 Root plugins and style checks
gradle.properties               Build resource limits
desktopApp/                     Composition root, Koin wiring, typed navigation
  src/commonMain/               App UI and Decompose root
  src/desktopMain/              JVM entry point, database path and lifecycle
  src/commonTest/               Navigation behavior tests
domain/                         Student model, repository port, registration use case
  src/commonMain/
  src/commonTest/                Validation and normalization tests
core/
  database/                     SQLDelight schema, repository adapter, JDBC factory
    src/commonMain/sqldelight/  SQLite schema and generated query definitions
    src/desktopMain/kotlin/     JDBC repository implementation
    src/desktopMain/            JVM SQLite driver factory
    src/desktopTest/            Real SQLite persistence/update/reopen test
  network/                      Ktor client foundation; no backend required
  utils/                        Shared name normalization utility
feature/
  reception/                    Reception screen
  billing/                      Billing screen
  students/                     Students screen
  reports/                      Reports screen
```

Every module has its own `build.gradle.kts`, a KMP `desktop` JVM target and JDK 21
toolchain. Portable logic/UI uses `commonMain`; Java filesystem, JDBC, windowing
and HTTP engine integration stay in `desktopMain`. Desktop is the only configured
target; Android/iOS can be added with their platform adapters later.

## Run and validate

From the repository directory on Windows PowerShell:

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat ktlintCheck allTests :desktopApp:desktopJar
.\gradlew.bat :desktopApp:createDistributable :desktopApp:packageMsi :desktopApp:packageExe
```

On Linux/macOS use `./gradlew` for build/tests or development launch. Launch needs
a graphical desktop. Windows installers must be built on Windows with JDK 21 and
WiX Toolset 3.x. Set `WIX_PATH` to the directory containing `candle.exe` and
`light.exe` and add it to `PATH` before running packaging:

```powershell
$env:WIX_PATH = "C:\Program Files (x86)\WiX Toolset v3.14\bin"
$env:Path = "$env:WIX_PATH;$env:Path"
```

CI discovers the runner's preinstalled WiX 3 and adds it to the packaging environment. Installers are
written under `desktopApp/build/compose/binaries/main/`. The package includes its
Java runtime; end users do not need to install Java.

Use `./gradlew ktlintFormat` to format Kotlin and Gradle scripts. CI triggers for
pushes and pull requests to `main`, runs all configured unit tests, checks style,
builds the desktop JAR and publishes MSI/EXE workflow artifacts.

## Student import and local backup

- Admin and Reception can download the template from Students. Keep the headers
  `name,grade,studentPhone,parentPhone,groupName` and export as CSV UTF-8. Format
  phone columns as text in Excel to preserve leading zeroes. Name, grade and parent
  phone are required. Use a grade label from the app in Arabic or English, or its
  ID (for example, `primary-1`). Group name is optional and must match one active
  group when provided. An optional `code` column preserves supplied student codes.
- The import defaults new students to the Arabic education system. It skips rows
  with matching name, grade, student phone, parent phone and group. A validation
  or code conflict rolls back the file so it can be corrected and retried.
- Admin can export a SQLite backup to a folder from Settings. Restore accepts a
  compatible `.db` file, closes the app after confirmation, and applies the backup
  on the next launch. Restored accounts come from the selected backup. The prior
  database is retained as `center.before-restore.db` next to `center.db`.

## Architecture and extension points

Dependencies flow from the desktop composition root to feature UI and infrastructure;
features depend only on domain. Domain owns business models, repository contracts
and validation and has no UI, DI, network or SQL dependencies. Infrastructure
implements domain ports; Koin connects them in `Main.kt`. Feature modules are
independent and must not import sibling implementations.

Decompose owns the selected typed destination with a single-entry stack; selecting
a workspace replaces the active destination. State is process-local; restoration
is intentionally disabled (`serializer = null`). The desktop owns the component
lifecycle and closes Koin and SQLite when the application exits.

The initial UI contains four navigable workspace placeholders. Student registration
and SQLite persistence are a tested foundation, not a complete student management
UI. Billing, reports, authentication and backend contracts are not implemented.
No secrets or external service are required. Close future Ktor clients in their
owning application scope. The JDBC repository dispatches blocking operations to `Dispatchers.IO`.

Database location: `%LOCALAPPDATA%/CenterSystem/center.db` on Windows, or
`~/CenterSystem/center.db` when LOCALAPPDATA is absent. SQLDelight initializes and
tracks schema versions. Add versioned `.sqm` migrations for future schema changes;
do not replace an existing database to apply changes. Generated SQL code stays in
build output. The SQLite integration test uses and cleans a temporary directory.

Each cloud task is isolated: use its existing checkout, without creating a worktree.
No cloud configuration or environment publication is performed by this project.

See [local database documentation](core/database/README.md) for the offline schema, DAOs, migration and storage rules.

See [domain layer documentation](domain/README.md) for repository contracts, business use cases and transactional message queuing.
