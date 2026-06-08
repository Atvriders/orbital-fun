# Orbital Fun

A top-down Android game about gravity and orbital mechanics, built with
[libGDX](https://libgdx.com/) in Kotlin. Fly a spacecraft through a star system,
use planets for **gravity assists** (and reverse assists) to gain or shed speed,
plan burns with **KSP-style maneuver nodes**, and explore for profit.

## Modes

- **Sandbox** — unlimited fuel and no fail state, but a *fuel-used* counter is
  always shown so you can judge how efficient a maneuver was.
- **Survival** — fuel is finite. Earn money by reaching farther from the system
  origin and by selling scanned maps, then spend it at the dock on fuel and ship
  upgrades (thrust, fuel tank, efficiency, scanner range). Run dry away from a
  dock and you're stranded.

## Controls

- **Thrust D-pad** (bottom-left): PRO+ / RETRO / RAD IN / RAD OUT set a continuous
  burn direction; CUT stops thrusting. THR -/+ set throttle.
- **Camera**: drag to pan, scroll / pinch to zoom. Camera follows the ship until
  you pan.
- **Maneuver node** (bottom-right): NODE toggles a planned burn; PG/RD +/- dial in
  prograde and radial delta-v; T -/+ slide the node earlier/later; the magenta
  curve previews the resulting orbit. BURN commits the planned delta-v; CLR clears
  the node.
- **Survival**: SCAN discovers nearby bodies (sellable maps); SHOP opens the dock
  store when you're docked.

## How it works

- **Physics** — real Newtonian gravity (`a = G·m/r²`, softened near bodies),
  integrated with RK4. Planets orbit the star on rails, so a flyby of a *moving*
  body transfers energy — gravity assists fall straight out of the simulation.
- **Trajectory** — the cyan dotted line forward-integrates a copy of the ship
  state (planets included) to show where you'll coast; the maneuver planner does
  the same after applying the planned burn.

## Project layout

- `core/` — all platform-agnostic game code (physics, simulation, render, UI).
- `lwjgl3/` — desktop launcher, used to play/test without a device.
- `android/` — Android launcher (included in the build only when an SDK is
  present).

## Building & running

Requires JDK 17+.

```bash
# Run the desktop version
./gradlew lwjgl3:run

# Run the headless physics/economy tests
./gradlew core:test

# Build an Android debug APK (requires the Android SDK; create local.properties
# with sdk.dir=/path/to/Android/Sdk, then:)
./gradlew android:assembleDebug
```

The Android module is only part of the build when an Android SDK is detected
(`ANDROID_HOME`/`ANDROID_SDK_ROOT` set, or a `local.properties` present), so the
core and desktop targets build fine without it.
