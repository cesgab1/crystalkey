# Crystal Key — project conventions

## Canonical location

```
C:\Users\cesga\Claude\Projects\crystalkey
```

This is **the only copy**. It is the project root — the folder containing
`settings.gradle.kts`, `app/` and `core/`. Open exactly this path in Android
Studio.

There is no nesting, no `crystalkey-android` wrapper folder, and no archive
sitting beside it. Earlier there were three of those and fixes kept landing in
the copy that was not being built; that is what this file exists to prevent.

## Delivery rules

- **Write files directly into this directory.** Do not ship `.zip` archives of
  an active project. The nesting problem came from an archive named
  `crystalkey-android.zip` whose root folder was `crystalkey`: Windows'
  "Extract All" creates a folder named after the archive, so extracting it
  produced `crystalkey-android\crystalkey\`.
- **Never create a second copy.** No `-v2`, no `-new`, no "backup" folder. If a
  change is risky, use git, not a duplicate directory.
- The device bridge **cannot delete files**. Anything to be removed gets moved
  to `_to_delete\` and flagged in the reply so it can be binned by hand.
- Do not touch `.idea/`, `.gradle/` or `local.properties` — Android Studio owns
  those and regenerates them on sync.

## The build loop

The container that writes this code cannot build Android (see below), so builds
happen on **GitHub Actions** and Claude reads the results from the API.

- Repo is **public** on purpose: GitHub serves workflow runs and job logs for
  public repos anonymously, so Claude reads full build output with no token and
  no credential in the chat transcript.
- Claude writes files into this folder; the human runs `push` (that is
  `push.bat` in the project root) to stage, commit and push.
- Claude then polls `api.github.com/repos/<user>/crystalkey/actions/runs`,
  reads the failing job's log, fixes, and the cycle repeats.
- Setup instructions live in `SETUP_CI.md`. One-time only.

Do not go back to pasting compiler errors into chat one at a time. That made the
human the compiler for four rounds and each round surfaced exactly one error.

## Build gates

Carried over from the famconnect project and still in force here:

1. **Visual first.** Always produce a render, diagram or sketch before or
   alongside anything else.
2. **Feature list.** Always state what a build actually contains.
3. **Approval before coding.** Ask before writing implementation code.
   Exception: none.

## Verified vs unverified — keep this honest

| Module | State |
|---|---|
| `core/` | Compiled and executed. 31 checks, 0 failures. See `VERIFICATION.txt`. |
| `app/` | Source only. Never compiled by anyone. |

The cloud container that writes this code **cannot build Android**: the egress
allowlist returns 403 for `maven.google.com`, `dl.google.com`,
`repo1.maven.org`, `plugins.gradle.org` and `services.gradle.org`, and there is
no `/dev/kvm` for an emulator. `github.com` is reachable, which is how the
standalone Kotlin compiler gets in and why `core/` can be verified by running
it.

Never describe `app/` as working. Say what was executed and what was not.

## Build and test

```bash
gradlew.bat :core:test --info          # the real rule suite, no device needed
gradlew.bat :app:assembleDebug         # debug APK
gradlew.bat :app:connectedDebugAndroidTest   # needs a running device
```

Compose previews live in `app/src/main/kotlin/com/crystalkey/app/ui/Previews.kt`
and render with no device at all — the fastest way to look at a screen.

## Toolchain pins

- AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1, Compose BOM 2024.12.01
- `compileSdk` 35, `minSdk` 26, JVM target **17 in both modules**

Both modules pin `jvmTarget = 17` *without* `jvmToolchain(17)`. A toolchain has
to be found or downloaded, and Android Studio commonly runs Gradle on JDK 21,
which fails as "No matching toolchains found". The two targets must stay equal
or `:app` consuming `:core` throws "class file has wrong version 65.0, should
be 61.0".

`settings.gradle.kts` filters Google's Maven to `com.android.*`, **`com.google.*`**
and `androidx.*`. Dropping `com.google.*` breaks the plugin classpath on
`com.google.testing.platform:core-proto`, which AGP pulls for its Unified Test
Platform.

## Known build failures and their fixes

Each of these cost a build cycle. They are written down so they are not
rediscovered.

**`Could not find com.google.testing.platform:core-proto`** — the `google()`
block in `settings.gradle.kts` must filter in `com.google.*` alongside
`com.android.*` and `androidx.*`. AGP pulls its Unified Test Platform artifacts
from that group; without the filter entry Gradle never looks in Google's repo
and falls through to Maven Central, where they do not exist.

**`Could not connect to Kotlin compile daemon`** — Kotlin compiles in a separate
JVM reached over a loopback socket. Causes, in the order worth trying:

1. No explicit heap for it, so it dies during handshake. `gradle.properties`
   now sets `kotlin.daemon.jvmargs=-Xmx1536m`.
2. Stale daemon state. `gradlew.bat --stop`, then delete
   `%USERPROFILE%\.kotlin\daemon`.
3. Antivirus or firewall blocking the loopback socket — common on Windows.
   Uncomment `kotlin.compiler.execution.strategy=in-process` in
   `gradle.properties` and raise `org.gradle.jvmargs` to `-Xmx4g`.

**`Cannot access 'val RowColumnParentData?.weight': it is internal`** — something
imported `androidx.compose.foundation.layout.weight`. `weight`, `align`,
`matchParentSize` and `alignByBaseline` are **members of `RowScope`/`ColumnScope`**,
not top-level functions. Importing the name resolves to an unrelated internal
property. Delete the import; the modifier works from the receiver scope.

**`No matching toolchains found for JavaLanguageVersion 17`** — do not use
`jvmToolchain(17)`. Both modules pin `jvmTarget = 17` instead, which uses
whatever JDK Gradle already runs on. The two modules must stay equal or `:app`
consuming `:core` throws "class file has wrong version 65.0, should be 61.0".

## Architecture rule

Game rules live in `core/` and nowhere else. `app/` owns state and rendering and
delegates every decision — dealing, rotation, thresholds, timers — to the
verified module. A rule implemented in a composable is a rule nobody tested.
