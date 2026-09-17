# Impulse — Architecture

Status: **draft**. This document describes structure and rules, deliberately without
implementation code. Names are suggestions.

## 1. Build layout

One sbt build, two sub-projects:

```
scala-leap/
├── build.sbt                 cross-project definition (sbt-crossproject + sbt-scalajs)
├── project/                  plugins, sbt version
├── core/                     crossProject(JVM, JS), CrossType.Pure — pure Scala, zero platform deps
│   └── src/{main,test}/scala/impulse/
│       ├── math/             Vec2, AABB, numeric helpers
│       ├── physics/          bodies, shapes, broad/narrow phase, solver, PhysicsWorld, events
│       ├── world/            entities, level model, Tiled (.tmj) decoding, level → world building
│       └── game/             player controller, game rules, game-state machine, camera model, animation FSM
├── web/                      Scala.js only, depends on coreJS
│   └── src/main/scala/impulse/web/
│       ├── loop/             requestAnimationFrame driver + fixed-timestep accumulator
│       ├── render/           canvas renderer, debug overlay, view transform (metres→pixels, y flip)
│       ├── input/            keyboard listeners → InputSnapshot
│       └── assets/           fetch levels (JSON text) and images
├── web/index.html            canvas + script tag
├── assets/                   levels/*.tmj, tilesets, sprites
└── docs/
```

Why a cross-project: `coreJVM/test` gives a fast edit-test cycle with no browser or Node
involved, `coreJS/test` checks parity, and the compiler *enforces* that physics cannot see the
DOM, because `scala-js-dom` is simply not on `core`'s classpath.

Starting simpler is fine: in M0–M1 `core` may contain only `math`; the packages above appear as
milestones need them. Splitting `core` into several sbt modules (to have the compiler also enforce
`math → physics → world → game`) is optional; package discipline plus review is enough initially.

## 2. Modules and dependency rules

```
math  ←  physics  ←  world  ←  game  ←  web (loop, render, input, assets)
```

An arrow means "is depended on by". **Dependencies only point left.** Consequences:

| Module | Knows about | Must NOT know about |
|---|---|---|
| `math` | numbers | everything else |
| `physics` | `math` | entities, player, tiles, Tiled, input, canvas, time sources |
| `world` | `math`, `physics` | input, canvas, game rules (win/lose) |
| `game` | all of core | canvas, DOM, `requestAnimationFrame`, wall-clock time |
| `web` | everything | — (but contains *no game rules*) |

Responsibilities:

- **math** — `Vec2`, `AABB`, epsilon comparison, clamp/lerp. Immutable values, total functions.
- **physics** — the engine from `physics-design.md`. Public surface: create/remove bodies, apply
  force/impulse, set velocity (kinematic), set gravity scale, `step(dt)`, read body state, read the
  step's events and manifolds (for game logic *and* the debug overlay), spatial queries.
- **world** — what exists in a level. Entity definitions (an ADT: Player, Crate, MovingPlatform,
  Spring, Hazard, Goal…), the mapping entity ↔ `BodyId`, the level model (tile layers, object
  layer, bounds, spawn), the Tiled decoder (`String` JSON → `Either[LevelError, Level]`), tile
  merging into static colliders, and "instantiate level into a fresh physics world".
- **game** — behaviour and rules: `PlayerController`, moving-platform path following, spring and
  hazard reactions to physics events, win/lose detection, the top-level game-state machine
  (Title / Playing / LevelComplete / Finished, Paused), camera model (a position + rules, in
  metres), animation state machine (which logical animation/frame — not pixels).
  Exposes essentially one function shape: *advance the game by one fixed step given an input
  snapshot*, and one read-only *view* for the renderer.
- **web** — the only impure shell: owns the loop, reads the keyboard, loads files, draws.

## 3. How modules communicate

Data flows one way per frame; all messages are **plain immutable data (case classes / enums)**.

```
 keyboard events ──► input ──► InputSnapshot ─────────────┐
                                                           ▼
   ┌────────────────────── game.step(snapshot) — once per FIXED step ──────────────────────┐
   │ 1. controllers/AI write intentions into physics (impulses, kinematic velocities,       │
   │    gravity scale)                                                                      │
   │ 2. physics.step(dt)                                                                    │
   │ 3. read PhysicsEvents (contact begin/end, sensor overlaps, crush) → game rules →       │
   │    GameEvents (PlayerDied, LevelCompleted, SpringFired, …)                             │
   │ 4. apply GameEvents: respawn, switch game state, queue level load, animation changes   │
   └────────────────────────────────────────────────────────────────────────────────────────┘
                                                           │
                         RenderView (read-only) ◄──────────┘
                                │   + interpolation alpha
                                ▼
                     render (sprites, tiles, HUD, debug overlay)
```

- **InputSnapshot** — the state of *logical* actions for one step: left/right axis, jump held,
  jump pressed-this-step, pause, debug toggles. Key codes never leave `web.input`. Edge detection
  ("pressed this step") is resolved when the snapshot is taken, so a key press that falls between
  two fixed steps is not lost and not doubled.
- **PhysicsEvents → GameEvents** — physics speaks in body IDs; the `world` mapping translates to
  entities; `game` pattern-matches on (entity, entity) pairs. Exhaustive matching on sealed ADTs is
  the Scala feature that keeps this honest.
- **RenderView** — everything the renderer needs and nothing else: for each drawable its previous
  and current position (for interpolation), sprite/animation frame identifier, facing; the camera;
  the tile layers; HUD data; and, if debug is on, a **DebugView** (AABBs, shapes, manifolds with
  normals/points, velocities, grid cells, sleep flags, controller state such as grounded/coyote
  counters). The renderer **never** reaches into `PhysicsWorld` directly.
- **No callbacks from physics into game code during `step`.** Events are collected and returned
  after the step. This avoids re-entrancy (removing a body while the solver iterates) and keeps
  the step testable. Body creation/removal requested by game rules is applied between steps.

## 4. State modeling: immutable by default, mutable where it pays

Guiding rule: **immutable data and ADTs at the game level; mutation confined inside the physics
step; nothing mutable crosses a module boundary.**

| Thing | Model | Why |
|---|---|---|
| `Vec2`, `AABB`, `Material`, `Manifold`, events, `InputSnapshot`, `Level`, `RenderView` | immutable case classes / enums | values; cheap; safe to share; trivially testable |
| `Shape`, `BodyType`, `Entity`, `GameEvent`, `PhysicsEvent`, `LevelError` | sealed ADTs (Scala 3 `enum`) | exhaustive pattern matching; illegal states unrepresentable |
| Game-state machine (Title/Playing/Paused/…) | ADT + pure transition function `(state, input/events) → state` | classic FSM; each state carries only the data valid in it |
| Player controller state (grounded, coyote counter, buffer counter, facing) | small immutable case class, replaced each step | a pure function of (previous state, input, contacts) → (new state, commands) — unit-testable with no physics at all |
| Animation FSM | ADT + pure transition | same |
| IDs (`BodyId`, `EntityId`) | **opaque types** over `Int` | zero-cost, but can't mix up a body ID with an entity ID or a raw index |
| `PhysicsWorld` internals | **start immutable** (e.g. a `Vector` of immutable bodies, a step returning a new world); **move to encapsulated mutation** (mutable body fields or arrays, reused buffers) only when a profile or GC stutter in the browser says so | The solver touches every contact several times per step; allocating new bodies per impulse creates GC pressure, and Scala.js GC pauses show up as frame hitches. But measure first: a few hundred bodies may be fine. |

If/when the physics world becomes mutable, keep these guard rails:

- Mutation happens only inside `physics` and only during `step` or explicit commands.
- The public read API returns immutable snapshots or read-only views.
- Determinism and scenario tests (written against the immutable version) must still pass
  unchanged — they are the safety net for the refactor. This refactor is itself a good lesson:
  *same tests, different internals*.
- `Vec2` stays an immutable value type regardless. (Optimising `Vec2` allocation is a last resort.)

A full ECS is **not** planned: entity counts are tiny and an ADT of entities plus a map to body
IDs is easier to read. Revisit only if the entity ADT becomes painful.

**Error handling convention:** parsing/loading returns `Either[Error, A]` with an error ADT;
absence is `Option`; exceptions only for programmer errors (`require`, unreachable cases). No
`null`. The `web` shell decides how to display errors.

## 5. The fixed-timestep loop

Owned by `web.loop`; the *logic* of the accumulator can live in `core` as a pure function so it
can be unit-tested with fake frame times.

```
on each requestAnimationFrame(now):
    frameTime   = min(now − last, MAX_FRAME)        // clamp: tab was in background / debugger pause
    accumulator += frameTime
    while accumulator ≥ DT:                         // DT = 1/60 s (see physics OPEN-10)
        snapshot = input.takeSnapshot()             // edges consumed exactly once
        game     = game.step(snapshot)              // fixed DT — the sim never sees variable time
        accumulator −= DT
    alpha = accumulator / DT                        // 0 ≤ alpha < 1
    render(game.view, alpha)                        // draw lerp(previousPos, currentPos, alpha)
```

(Pseudocode to fix ideas — not a design for the actual code.)

Points to understand and decide:

- **Why fixed:** stability and determinism of the solver; tuning (jump height, coyote steps) does
  not depend on frame rate; tests and replays are exact.
- **Spiral of death:** if a step takes longer than `DT`, the accumulator grows forever. The frame
  time clamp (e.g. 0.25 s) and/or a max-steps-per-frame cap trade correctness of wall-clock time
  for survival.
- **Interpolation:** rendering is up to one step behind, but smooth on 144 Hz monitors and when
  frames don't align with steps. Requires keeping previous positions per drawable. Camera must be
  interpolated too, or the world will jitter against it.
- **Pause / single-step** (debug): pause = stop feeding the accumulator; single-step = run
  exactly one `game.step` on a key press. Trivial with this structure — and very valuable.
- **Time source:** only `web` sees `now`. `core` sees a count of steps. Timers in game logic are
  step counters.

## 6. Keeping physics independent and headlessly testable

1. **Compile-time isolation:** `core` has no DOM dependency (enforced by the build).
2. **No hidden inputs:** no wall-clock, no randomness without an explicit seed, no global mutable
   singletons. `step(dt)` is the only way time advances.
3. **Physics speaks only data:** inputs are commands, outputs are state + events + manifolds.
4. **Debug drawing is data, not calls:** physics exposes manifolds/AABBs/cells; `web.render`
   decides how to draw them. (No `DebugDraw` interface injected into physics.)
5. **Test pyramid:**
   - property tests (`math`, narrow phase, broad phase vs brute force, impulse invariants);
   - scenario tests: build a tiny world in code, step N times, assert (see physics-design §12);
   - controller tests: pure state-machine tests with synthetic contacts and inputs;
   - level decoding tests: `.tmj` fixtures → expected `Level` or expected error;
   - loop tests: accumulator logic with scripted frame times;
   - a handful of end-to-end headless tests: load level 1, feed a recorded input script, assert
     the player reaches the goal (this becomes the replay system in stretch S3).
   - Rendering is verified by eye — aided by the debug overlay — not by automated tests.

## 7. Assets and level pipeline

```
Tiled editor ──export──► level.tmj (JSON)
   web.assets: fetch text ─► world: decode JSON ─► Either[LevelError, Level]      (pure, testable)
   Level ─► world: build ─► PhysicsWorld + entities + spawn + bounds               (pure, testable)
   web.assets: load tileset/sprite images (async) ─► renderer's image table        (impure, web only)
```

- **Level conventions in Tiled** (to be fixed in M8 and documented in this file): a `solid` tile
  layer; a `oneway` tile layer or tile property; an object layer with typed objects (`player`,
  `goal`, `crate` {mass}, `platform` {path polyline, speed}, `spring` {impulse}, `hazard`);
  custom properties for parameters. Unknown object types are *errors*, not silently ignored.
- **Units conversion** pixels → metres and y-down → y-up happens in the decoder, once.
- **JSON library:** one cross-platform (JVM + JS) library; uPickle (simple) or circe (more
  FP-idiomatic, more type-class learning). Decoding into hand-written ADTs rather than deriving
  everything automatically is encouraged — it is a good exercise in `Either`, `for`-comprehensions
  and error accumulation.
- **Async loading** (`Future`/`js.Promise`) exists only in `web.assets`. `core` receives fully
  loaded strings. A `Loading` state in the game-state machine covers the wait.

## 8. Cross-cutting conventions

- Package root `impulse`; one main concept per file; tests mirror the package structure.
- Tunable constants (gravity, jump speed, accel limits, solver iterations, slop…) grouped in
  immutable config case classes passed in explicitly — never scattered literals or globals. This
  makes tests able to use their own configs and makes a future tuning UI possible.
- Logging: none in `core` (return data instead); `web` may log to the console.
- Performance work only after measuring (browser profiler); record findings in `docs/`.

## 9. Open architecture questions

| # | Question | Leaning |
|---|---|---|
| A-1 | One `core` project with packages vs several sbt modules enforcing the layering | Packages first; split if violations creep in |
| A-2 | Immutable vs mutable `PhysicsWorld` | Immutable first, measure at M6/M7 (see §4) |
| A-3 | uPickle vs circe for `.tmj` | Decide at M8; try decoding one small file with each |
| A-4 | How does `game.step` return side-effect requests to `web` (load next level, play sound)? | A list of output commands/ADT in the step result; `web` interprets them |
| A-5 | Dev workflow: `fastLinkJS` + static server vs Vite + `vite-plugin-scalajs` | Simplest thing first (static `index.html`), upgrade when reload time hurts |
| A-6 | Where does the camera live — `game` (model) vs `web` (view)? | Model in `game` (testable, deterministic), pixel transform in `web` |
