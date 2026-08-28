# Quest for the Crystal Key — Android

Asymmetric local co-op for 2–6 players in one room, on one Wi-Fi, with a phone
each. No account, no server, no ads.

**Project root:** `C:\Users\cesga\Claude\Projects\crystalkey` — the only copy.
Open exactly this folder in Android Studio. Conventions are in `CLAUDE.md`.

---

## What is verified and what is not

Be blunt about this before reading any further, because it decides what you can
trust without building:

| Module | State | How it was checked |
|---|---|---|
| `core/` | **Verified** | Compiled with Kotlin 2.0.21 and executed. 31 checks, 0 failures — see `VERIFICATION.txt`. |
| `app/` | **Unverified source** | Written against the design system but never compiled. The container that produced it has no access to Google's Maven repository, so no Android build was possible. |

The core is the part worth trusting: it holds the dealing algorithm, the turn
rotation, the meters and the session state machine. The Compose layer is a
scaffold that expresses the design — expect to fix a few compile errors on the
first build.

## Build it

```bash
./gradlew :core:test          # runs the verification suite (fast, no Android SDK needed)
./gradlew :app:assembleDebug  # produces app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run. Requires JDK 17+ and
Android SDK 35.

## Why the container could not build it

The session's egress allowlist returns `403` at the proxy for
`maven.google.com`, `dl.google.com`, `repo1.maven.org`, `plugins.gradle.org` and
`services.gradle.org`. Without those there is no Android Gradle Plugin, no
AndroidX and no SDK. There is also no `/dev/kvm`, so no emulator.

`github.com` **is** reachable, which is how the standalone Kotlin compiler got
in — and it is why the core could be verified by execution rather than by
assertion.

Three ways to close that gap, cheapest first:

1. **Build on your own machine.** You already have `~/.android`, `~/.gradle`,
   `~/.expo` and `~/.maestro`, so the toolchain is there. Nothing to set up.
2. **Allowlist five hosts** — the five named above. Then the cloud container
   builds and unit-tests directly, though still without an emulator.
3. **Let CI be the build farm.** `.github/workflows/build.yml` is ready: it runs
   the core suite, assembles a debug APK as a downloadable artifact, and runs
   instrumented tests on a real emulator (GitHub runners have KVM). This is the
   only option of the three that can verify the UI end to end.

## Layout

```
core/                        pure Kotlin, no Android dependencies
  Model.kt                   atoms, seats, hands, deals
  Deterministic.kt           SplitMix64 — the deal is derived, never transmitted
  PartyRules.kt              everything that scales with party size
  Dealer.kt                  splits one puzzle across 2–6 seats
  TurnPlanner.kt             actor rotation and "nobody quiet twice running"
  Meters.kt                  Wrath, Lantern Light, the beat window
  Session.kt                 the state machine, including freeze-on-drop
app/                         Compose UI, unverified
  theme/Theme.kt             the palette and type scale from the design system
  ui/Components.kt           bevel buttons, glass, parchment, meters
  ui/Screens.kt              title and lobby
  SessionViewModel.kt        owns state, delegates every rule to core
```

## The properties the core actually guarantees

These are not aspirations — each one is an executed check in
`core/src/test/kotlin/com/crystalkey/core/CoreTests.kt`:

- Every seat is dealt at least one atom, at every party size from 2 to 6.
- No seat is ever dealt the whole puzzle, so no one can act alone.
- Hand sizes differ by at most one, and the extras go to the seats with the most
  carry capacity — a child is never handed two atoms while an adult holds one.
- The double hand **moves between rounds** instead of parking on one adult.
  (This one was a real bug the suite caught.)
- The same seed and round produce byte-identical deals on every device, so the
  deal is derived state rather than network traffic.
- Across a full rotation every seat is the actor exactly once, and no seat is
  quiet on two consecutive turns.
- No turn can be resolved by one phone alone.
- Wrath fills on room silence, and on any individual player's silence — but only
  above four seats.
- A dropped phone freezes the quest and rejoining restores the exact prior
  state; a seventh seat cannot squeeze into the lobby.

## Not built yet

Local transport (NSD discovery + sockets), the puzzle and boss screens, the
painted art pipeline, and audio. The transport is the next thing worth doing and
the next thing worth testing properly — it is the only remaining piece that can
silently break the "no server, no internet" promise.

---

## Testing it in Android Studio

**Open** `File ▸ Open` and pick this folder (the one with `settings.gradle.kts`).
Let the first Gradle sync finish — it downloads roughly a gigabyte of AGP,
AndroidX and Compose. If it offers to install **Android SDK Platform 35**, say
yes; that is what `compileSdk = 35` needs.

There are four ways to test, cheapest first.

**1 · The core suite — no device, no emulator, ~2 seconds.**
Right-click `core/src/test/kotlin/.../CoreVerificationTest.kt` ▸ **Run**, or from
a terminal:

```bash
gradlew.bat :core:test --info
```

This is the suite that actually proves the game rules. `--info` prints each
check as it passes. Green here means the dealer, the rotation, the meters and
the state machine behave as designed.

**2 · Compose previews — no device, instant.**
Open `app/src/main/kotlin/com/crystalkey/app/ui/Previews.kt` and click **Split**
or **Design** in the top-right of the editor. Four previews render: the title
screen and the lobby at two, four and six seats. The lobby previews are the
useful ones — they show the dealer's split ("1 atom" six times at six seats,
"2 atoms" on the adults at four) without anyone playing.

**3 · Run it on a device or emulator.**
Pick a device in the toolbar and press **Run ▸ app** (Shift+F10). An emulator
needs API 26+. What you get: the title screen, then **Start a new quest** opens
the lobby. Tap **Add a seat** a few times, then **Everyone ready**, then
**Begin the quest** — the party panel updates live with the lantern segments and
cast threshold for that party size, straight out of `PartyRules`.

**4 · Instrumented UI tests — needs a running device.**
Right-click `app/src/androidTest/kotlin/.../LobbyUiTest.kt` ▸ **Run**, or:

```bash
gradlew.bat :app:connectedDebugAndroidTest
```

Two tests: the title screen's primary button fires its callback, and the lobby
renders one "1 atom" row per seat at six seats.

### If the build fails

- **`Unsupported class file major version` / toolchain errors** — check
  `File ▸ Settings ▸ Build, Execution, Deployment ▸ Build Tools ▸ Gradle` and set
  Gradle JDK to the bundled JetBrains Runtime 17 or newer.
- **`SDK location not found`** — Android Studio writes `local.properties` on
  first sync. If it did not, add `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
- **Anything in `app/`** — that module has never been compiled by anyone. The
  core module has. If a Compose call does not resolve, fix it in place; nothing
  in `core/` depends on `app/`, so the verified half stays green either way.
