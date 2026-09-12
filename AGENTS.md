# DevInfo Agent Instructions

This file is an execution contract for AI agents working in this repository. It is not a contributor tutorial. Apply these rules automatically on every task in this repository.

## 1. Execution contract

- Treat the user's current request as the objective for this turn. When the user explicitly requests implementation, implement it; do not return only a plan.
- Do not ask the user to reconfirm facts that are already explicit. Ask only when scope, authority, safety, compatibility, or a behavior trade-off is materially ambiguous.
- Before editing, inspect git status, the relevant source files, their callers, and the relevant tests. Never infer implementation from filenames or stale documentation.
- Preserve existing user changes. Never use git reset --hard, git checkout --, unapproved recursive deletion, or any other operation that can discard work.
- Use apply_patch for file edits. Keep changes minimal, local, and reviewable. Do not mix unrelated cleanup, dependency upgrades, or broad refactors into the task.
- Do not commit, push, create branches or releases, change CI secrets, or send external messages unless the user explicitly requests it.
- At completion, report changed files, behavior changes, verification commands, results, and blockers. Never claim that an unrun check passed.

## 2. Source of truth and documentation boundaries

Use this fact precedence:

1. Kotlin/Java source, Gradle configuration, Android manifests, resources, and tests.
2. GitHub Actions configuration under .github/workflows/.
3. Current README files and current documents under docs/.
4. Historical files under docs/archive/.

- When code, configuration, or tests disagree with documentation, trust the implementation and update the documentation.
- docs/ is local-only documentation and is excluded by /docs in .gitignore. Do not add it to a commit.
- docs/archive/ is historical material only. Do not treat its plans, proposals, old paths, or pending decisions as implemented behavior.
- When changing user-visible behavior, permissions, build/release flow, module boundaries, or security behavior, check whether all three public README files and the local docs need updates.
- Do not document permissions, retries, caching, backend services, navigation frameworks, or compatibility that the implementation does not provide.

## 3. Standard agent workflow

1. Parse the request into a goal, scope, acceptance criteria, and risks.
2. Perform read-only discovery with git status --short, rg/rg --files, entry points, types, callers, resources, and tests.
3. Form the smallest viable implementation internally. Ask a focused question only if a material ambiguity remains.
4. Apply a small patch without overwriting unrelated work.
5. Add or update tests for behavior changes. For UI, permissions, networking, Root, export, or lifecycle changes, record the required manual checks.
6. Run relevant checks, inspect git diff and git diff --check, and review the final diff line by line.
7. Report the result and remaining risks. If the environment blocks verification, state exactly where it was blocked.

## 4. Project structure and dependency boundaries

~~~text
app
├── feature-main ── data ── core
│                 └─────── ui ── core
├── data
├── ui
└── core
~~~

| Module | Put here | Do not put here |
| --- | --- | --- |
| app | MainActivity, manifest, app resources, splash, dependency assembly, APK build | Page-specific business details or platform collection |
| core | UI-independent models, enums, update state, CPU parsing | Android Context, Compose, or page logic |
| data | DeviceInfoCollector, CPU/hardware/battery monitoring, preferences, update checks, module export | Compose layout or page navigation |
| feature-main | MainScreen, MainViewModel, overview, details, settings, theme, and about pages | Scattered system reads or network implementations |
| ui | Material 3/Miuix themes, shared Compose components, blur, floating navigation, Markdown rendering | Device collection, business persistence, or release logic |

Key entry points:

- app/src/main/java/com/fioiu8/devinfo/MainActivity.kt: dependency assembly and the Compose root.
- data/src/main/java/com/fioiu8/devinfo/data/DeviceInfoCollector.kt: device information collection.
- data/src/main/java/com/fioiu8/devinfo/data/UpdateChecker.kt and GitHubClient.kt: update checks.
- data/src/main/java/com/fioiu8/devinfo/data/ModuleExportHelper.kt: module ZIP generation and validation.
- feature-main/src/main/java/com/fioiu8/devinfo/feature/main/MainViewModel.kt: loading, refresh, overview snapshots, and foreground monitoring.
- feature-main/src/main/java/com/fioiu8/devinfo/feature/main/MainScreen.kt: page state, back behavior, dialogs, and export interaction.

Current constraints:

- Page state is managed by MainScreen; Navigation Compose NavHost is not used.
- Theme and language preferences primarily use SharedPreferences.
- The update cache uses DataStore and retains a legacy SharedPreferences compatibility path.
- Root monitoring is optional. It is not a manifest permission and is not required to start the app.

## 5. Coding and architecture rules

### Kotlin and formatting

- Follow the Kotlin official style, .editorconfig, and ktlint.
- Use four-space indentation, UTF-8, LF line endings, a final newline, and no trailing whitespace for Kotlin/KTS.
- Use PascalCase for classes, interfaces, objects, and Composables; camelCase for functions, properties, and parameters; follow the existing UPPER_SNAKE_CASE convention for constants.
- Prefer val, immutable data classes, and explicit null handling. Do not use !! to hide a possible system-read failure.
- Before changing a shared model or public function, search all callers and update tests, resources, and documentation as needed.

### Compose, state, and coroutines

- Pages render UI and forward events. ViewModels/data modules coordinate loading, refresh, update checks, and monitoring.
- Publish UI state through StateFlow and immutable snapshots. Pages must collect state with lifecycle awareness.
- Do not create uncancellable network, file, sensor, or long-running work directly in a Composable.
- File, network, /proc, /sys, and other potentially blocking calls must not run on the main thread.
- When catching coroutine failures, rethrow CancellationException. Never convert cancellation into an ordinary failure.
- Follow MainViewModel lifecycle ownership for foreground monitoring. Avoid leaking Handlers, Receivers, SensorListeners, or global coroutines.
- Validate both Material 3 and Miuix paths when a shared behavior or UI contract changes.

### Failures, permissions, and privacy

- Return an explainable unavailable state when an individual device field cannot be read. Do not represent unavailable data as 0 or an empty value that looks valid.
- When adding permissions, network requests, device fields, Root operations, or export fields, assess privacy impact and update tests and documentation.
- Root access, module export, signing, and update checks are security-sensitive paths. Preserve confirmation, validation, failure handling, and user warnings.

## 6. Comments and KDoc

- Explain why, constraints, lifecycle, threading/dispatcher choices, compatibility reasons, and failure policy. Do not translate obvious code line by line.
- Use KDoc for public classes, public functions, complex Composables, cross-module models, and side-effecting entry points.
- Add @param and @return only when they clarify parameter, return, exception, thread, or lifecycle semantics.
- Every TODO must include a reason, a follow-up action, or an Issue reference. Do not submit ownerless TODOs.
- After changing code, review nearby comments and remove claims that are no longer true.
- Write new source comments in the dominant language of the file. Keep API names, class names, protocol names, and error types in English.
- Preserve source, license, and change-scope attribution when adapting external code.

Good comment:

~~~kotlin
// Reading /proc may block; use the IO dispatcher to keep the Compose main thread responsive.
val metrics = withContext(Dispatchers.IO) {
    collector.getCpuCoreMetrics()
}
~~~

Bad comment:

~~~kotlin
// Call getCpuCoreMetrics to get metrics.
val metrics = collector.getCpuCoreMetrics()
~~~

## 7. Verification commands

Windows:

~~~powershell
gradlew.bat testDebugUnitTest
gradlew.bat ktlintCheck
gradlew.bat lintDebug
gradlew.bat assembleDebug
~~~

Linux/macOS: replace gradlew.bat with ./gradlew.

Test locations:

- core/src/test: UI-independent models and CPU parsing.
- data/src/test: preference validation and export filename/path/ZIP boundaries.
- feature-main/src/test: ViewModel state and monitoring-mode transitions.
- app/src/androidTest: preference migration, theme preferences, and Android-environment behavior.

- Run tests directly related to the change first, then run the full checks according to risk.
- Instrumented tests require a device or emulator. Do not claim they passed when no device was available.
- When tests, lint, builds, or dependency downloads fail, report the exact command and failure stage.
- Never invent device results, network responses, performance numbers, or test results.

## 8. Git, pull requests, and reporting

- Use English for commit messages, branch names, pull request titles/descriptions, and agent status/final reports.
- Use an English commit prefix and an English description, for example: fix: repair foreground monitoring lifecycle, docs: update contributor instructions, test: cover module export validation.
- Keep one reviewable purpose per commit. Do not mix unrelated formatting, dependency upgrades, or refactors.
- Pull requests must state the goal, affected modules, behavior changes, risks, verification commands/results, and unverified items.
- Before submission, inspect git diff, git diff --check, and the file list. Confirm that no secrets, device data, APKs, ZIPs, logs, or temporary files are included.

## 9. Security prohibitions

- Never commit keystores, passwords, access tokens, APK/AAB files, module ZIPs, real device exports, or logs/screenshots containing device data.
- Never weaken release-signing gates, ZIP path/escaping validation, privacy warnings, Root confirmation, or error handling to make a build pass.
- Never hard-code secrets or real personal/device data in source, documentation, tests, or CI output.
- Without explicit authorization, do not change release credentials, CI secrets, remote repository settings, or external service state.
