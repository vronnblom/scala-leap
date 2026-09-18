# Impulse — Milestones

Every milestone ends in something **runnable or testable**. Estimates are focused hours for
someone experienced in programming but new to Scala; there is no calendar. Expect ±50 %.
Total MVP (M0–M14): roughly **150–230 h**.

Conventions:

- "Tests" means MUnit; "properties" means ScalaCheck via munit-scalacheck. Run on JVM day to day
  (`coreJVM/test`), on JS before closing a milestone (`coreJS/test`).
- At the end of each milestone: tick the boxes, note surprises/decisions in the relevant design doc
  (resolve `OPEN-n` items as they get decided), tag the commit (`m03` …).
- References like *physics §7.3* point into `docs/physics-design.md`; *arch §5* into
  `docs/architecture.md`.

Overview:

| # | Milestone | Runnable/testable result | Est. |
|---|---|---|---|
| M0 | Toolchain & cross-project skeleton | `sbt test` green on JVM+JS; "hello" on a web page | 4–8 h |
| M1 | Headless vector math | `Vec2`/`AABB` library with property tests | 8–12 h |
| M2 | Canvas & fixed-timestep loop | Rectangle moving smoothly, keyboard-controlled | 8–12 h |
| M3 | Bodies, integration, gravity | Boxes falling; analytic free-fall test | 8–12 h |
| M4 | Debug visualization | Overlay, pause, single-step | 6–10 h |
| M5 | AABB collision + impulse resolution | Boxes bounce off floor and each other | 12–18 h |
| M6 | Friction, positional correction, stacking | Stable 5-crate stack | 12–18 h |
| M7 | Broad phase grid | Same results as brute force, faster; cells drawn | 8–12 h |
| M8 | Tiled levels & tilemap collision | Crates tumbling around a Tiled-made level | 14–20 h |
| M9 | Physics-driven player & game feel | A level you can run and jump around | 16–24 h |
| M10 | Camera | Scrolling level bigger than the screen | 5–8 h |
| M11 | Level mechanics | Platforms, springs, hazards, goal, respawn | 14–20 h |
| M12 | Circles, SAT, slopes | Slopes and balls in a level | 14–20 h |
| M13 | Sprites & animation FSM | Animated character and tiles | 10–14 h |
| M14 | Game flow, sleeping, polish → **MVP** | Title → levels → end; published build | 14–20 h |
| S1–S5 | Stretch | rotation · joints/ropes · replays · editor · juice | open |

**Easiest scope cuts** if energy runs low: M12 (ship AABB-only; no slopes), M13 (coloured
rectangles are fine), sleeping in M14, and M7 (brute force is OK under ~100 bodies).

---

## M0 — Toolchain & cross-project skeleton

**Goal:** a working build you understand. **Deliverable:** repo with `core` (JVM+JS) and `web`
(JS) projects; one trivial test passing on both platforms; a web page that prints a string computed
in `core`.

**Tasks**

- [x] Install a JDK (17 or 21), sbt, Node.js (for Scala.js tests); pick an editor setup
      (Metals + VS Code, or IntelliJ) and confirm go-to-definition works
- [x] `git init`, `.gitignore` (target/, .bsp/, .metals/, .idea/, node_modules/)
- [x] Pin sbt version and the Scala 3 version (current LTS line); add `sbt-scalajs` and
      `sbt-crossproject` plugins
- [ ] Define `core` as a pure cross-project and `web` depending on `coreJS` + `scala-js-dom`
- [ ] Add MUnit to `core`; write one trivial test; run `coreJVM/test` and `coreJS/test`
- [ ] `web`: main method that writes a value from `core` into the page; `fastLinkJS`; `index.html`
      that loads the output; serve it with any static server
- [ ] Try `~fastLinkJS` and `~coreJVM/test` (watch mode)
- [ ] Write a short README: how to build, test, run

**Scala concepts:** sbt basics (settings, projects, `dependsOn`, `%%%`), Scala 3 syntax
(indentation vs braces — pick one and stay consistent), `@main`, packages, `val`/`def`, the REPL /
`sbt console`, how Scala.js links.

**Definition of done**

- `sbt test` green on both platforms from a clean clone; page shows the message.
- You can explain what `crossProject`, `%%%` and `fastLinkJS` do.
- Tests: one sanity test in `core`.

---

## M1 — Headless vector math

**Goal:** the math foundation, developed test-first. **Deliverable:** `impulse.math` with `Vec2`,
`AABB`, approx-equality helpers, all under property tests. Nothing on screen.

**Tasks**

- [ ] `Vec2` with the operations in *physics §3*; decide **OPEN-1** (normalising zero)
- [ ] Scalar-on-the-left multiplication (`2.0 * v`) via an extension method
- [ ] Approximate equality for `Double` and `Vec2` (absolute + relative tolerance — read why)
- [ ] `AABB`: construct from min/max and from centre/half-extents; reject/normalise inverted boxes;
      `overlaps`, `contains`, `union`, `expanded`, `center`
- [ ] ScalaCheck generators: finite, reasonably-sized doubles; `Vec2`; non-zero `Vec2`; valid `AABB`
- [ ] Properties (see DoD)
- [ ] Skim the generated JS or use `javap`/a benchmark later — just be aware `Vec2` allocates

**Scala concepts:** case classes, companion objects & `apply`, operator methods, extension methods,
`inline` (light touch), `Option`, immutability, `given` instances (ScalaCheck `Arbitrary`/`Gen`),
for-comprehensions over `Gen`, MUnit basics, why `==` on doubles is a trap.

**Definition of done**

- Properties (all "approximately"): `a + b = b + a`; `(a + b) + c ≈ a + (b + c)`; `a − a = 0`;
  `k(a + b) ≈ ka + kb`; `a·b = b·a`; `|a·b| ≤ |a||b|`; `perp(a)·a ≈ 0`; `|normalized(a)| ≈ 1`
  for non-zero `a`; `cross(a, b) = −cross(b, a)`; triangle inequality; `lerp` endpoints.
- AABB properties: `overlaps` is symmetric and reflexive; `union(a, b)` contains both;
  `a.expanded(m)` contains `a` for `m ≥ 0`; boxes separated on either axis never overlap.
- You hit at least one failing property caused by floating point and understood the shrunk
  counter-example.
- Green on JVM and JS.

---

## M2 — Canvas & the fixed-timestep loop

**Goal:** pixels on screen driven by a proper loop. **Deliverable:** a rectangle that moves at a
constant *simulated* speed regardless of monitor refresh rate, steered with the keyboard, drawn with
interpolation.

**Tasks**

- [ ] Get the canvas and 2D context via `scala-js-dom`; clear and draw a rectangle
- [ ] `requestAnimationFrame` loop; compute frame time
- [ ] Accumulator logic as a **pure function in `core`** (*arch §5*): given accumulator + frame
      time → number of steps + new accumulator + alpha; include the frame-time clamp
- [ ] Minimal "game": state = previous & current position + velocity; `step(input)` moves it
- [ ] Keyboard listeners → `InputSnapshot` with held state and pressed-this-step edges
- [ ] Render with interpolation between previous and current position
- [ ] View transform: metres → pixels and y-flip, in one place
- [ ] Experiment: set DT to 1/10 s and toggle interpolation to *see* what it does
- [ ] Handle canvas resize / devicePixelRatio (keep it simple)

**Scala concepts:** Scala.js interop (`js.Function`, facades, `dom.*` types), closures, `var` at
the edge vs immutable state in the core, case-class `copy`, tuples / small result case classes,
enums for logical actions, `Set`/`Map` for key state.

**Definition of done**

- Rectangle covers the same distance per second at 60 Hz and with the loop artificially throttled.
- Returning to a backgrounded tab does not freeze the page (spiral-of-death clamp works).
- Tests: accumulator function — 0 steps for tiny frames, N steps for N·DT, remainder/alpha
  correct, clamp applied; property: `steps·DT + newAccumulator ≈ oldAccumulator + clampedFrame`.
  Input edge detection: a press yields "pressed" in exactly one snapshot.

---

## M3 — Bodies, integration, gravity

**Goal:** the first real engine code. **Deliverable:** `PhysicsWorld` with static/kinematic/dynamic
bodies (AABB shape only) that fall under gravity, shown on the canvas; no collisions yet.

**Tasks**

- [ ] Opaque `BodyId`; `BodyType`; `Material`; body data (*physics §4*); decide **OPEN-2**
- [ ] `PhysicsWorld`: add/remove body, lookup, `step(dt)`; start with the simple immutable model
      (*arch §4*, **OPEN-14**)
- [ ] Semi-implicit Euler; forces accumulate then clear; gravity scale; speed cap
- [ ] Kinematic bodies move by velocity; static bodies don't move
- [ ] `applyForce`, `applyImpulse` (impulse = instant `Δv = j · invMass`)
- [ ] Hook into the M2 loop: spawn a box on key press, watch it fall; a kinematic box patrols
- [ ] Try explicit Euler on a spring force (`F = −kx`) just to watch it blow up; then revert

**Scala concepts:** opaque types, enums, `require`/smart constructors, immutable collections
(`Vector`, `Map`), `map`/`foldLeft`, pattern matching, default & named arguments, thinking in
"old world → new world".

**Definition of done**

- Tests: free fall from rest matches `y = y0 − ½gt²` within a documented tolerance after N steps
  (and you can explain why the error is what it is); static and kinematic bodies ignore forces
  and gravity; impulse on mass m changes velocity by `j/m`; `invMass = 0` bodies never change
  velocity; speed cap holds; determinism: two identical runs give identical state.
- Property: stepping with zero gravity and zero force preserves velocity exactly.

---

## M4 — Debug visualization

**Goal:** see what the engine thinks *before* collisions make it complicated. **Deliverable:**
toggleable overlay + pause + single-step.

**Tasks**

- [ ] Define `DebugView` data in `core` (*arch §3*): AABBs, body type, velocity vectors, IDs;
      extensible for contacts and grid cells later
- [ ] Renderer draws it: colour by body type, velocity arrows, optional ID labels
- [ ] Toggle keys: overlay on/off, pause, single-step, slow motion (e.g. 1 step every N frames)
- [ ] On-screen stats: step time (ms), body count, steps per frame
- [ ] World-space grid lines and an origin marker (catches y-flip/unit mistakes early)

**Scala concepts:** designing data for a consumer (view models), enums with parameters,
`Option`al fields, string interpolation/formatting, separating pure data from effectful drawing.

**Definition of done**

- You can pause, step one tick at a time and read each body's velocity from the screen.
- Tests: `DebugView` built from a known world contains the expected items; pause/single-step logic
  (pure part) — paused + no step key ⇒ 0 steps, step key ⇒ exactly 1.

---

## M5 — AABB collision detection & impulse resolution

**Goal:** things hit things. **Deliverable:** boxes fall, land on a static floor, bounce according
to restitution, and collide with each other. Brute-force pairs; no friction yet.

**Tasks**

- [ ] `Manifold` (normal A→B, depth, contact point); fix the normal convention in writing
- [ ] AABB-vs-AABB narrow phase returning `Option[Manifold]` (*physics §7.2*)
- [ ] Brute-force pair generation with filtering (skip static–static, both `invMass = 0`)
- [ ] Normal impulse resolution with combined restitution (*physics §7.3*)
- [ ] Naive positional correction so bodies don't sink (*physics §7.5*)
- [ ] Decide step order (**OPEN-5**)
- [ ] Overlay: contact points and normals; colour colliding bodies
- [ ] Playground scene: box rain on a floor with walls; key to spawn; e = 0 / 0.5 / 1 samples

**Scala concepts:** `Option` and `match`, `for`-comprehensions over collections to generate pairs,
`collect`/`flatMap`, tuples vs case classes, `foldLeft` to thread a world through contacts,
`@tailrec` or loops — and noticing where immutable updates start to feel awkward.

**Definition of done**

- Properties: narrow phase symmetry (`collide(a,b)` ⇔ `collide(b,a)`, normals opposite, depth
  equal); moving B by `n·(depth + ε)` removes the overlap; disjoint boxes never produce a manifold;
  resolution conserves total momentum for dynamic pairs; post-resolution relative normal velocity
  is ≥ 0; with e = 1 kinetic energy is conserved, with e < 1 it never increases.
- Scenarios: box dropped on floor with e = 0 comes to rest *on* the floor (penetration ≤ slop
  after 300 steps); with e = 0.5 each bounce apex is lower than the last; heavy box hitting a
  light box sends it away faster than the reverse.

---

## M6 — Friction, solver iterations, stable stacking

**Goal:** from "bouncy boxes" to "a world you could stand in". **Deliverable:** crates slide to a
stop, ride a kinematic platform, and a 5-crate stack stands still.

**Tasks**

- [ ] Coulomb friction impulse with combined μ (*physics §7.4*)
- [ ] Iterate the solver N times; then upgrade to **accumulated impulses** with clamping
      (*physics §7.6*) — keep the old version around long enough to compare in the overlay
- [ ] Restitution velocity threshold (*physics §8*); slop + percent tuning (**OPEN-7**)
- [ ] Revisit step order (**OPEN-5**) if resting contact jitters
- [ ] Move all tunables into a `PhysicsConfig` case class
- [ ] Scenes: crate pushed along the floor stops; crate on moving platform; crate tower; pyramid
- [ ] **Measure:** step time with 200 bodies in the browser profiler. Decide **A-2 / OPEN-14**:
      stay immutable or encapsulate mutation. If refactoring, do it *now*, guarded by existing tests

**Scala concepts:** config case classes, refactoring under tests, `var`/mutable collections and
`Array` *inside* an encapsulated boundary (`private`), `while` loops vs combinators for hot paths,
reading a profiler, maybe `final`/`inline`.

**Definition of done**

- Scenarios: sliding crate with initial speed v stops within the distance predicted by
  `v²/(2μg)` ± tolerance; crate on a platform moving at constant velocity has relative velocity
  ≈ 0 after 1 s; 5-crate stack: after 10 simulated seconds no crate has drifted more than a few cm
  and max penetration ≤ 2·slop; crate at rest on the floor has |v| below the sleep threshold.
- Property: friction never reverses the tangential relative velocity and never increases kinetic
  energy.
- All M5 tests still green. Performance number recorded in the docs.

---

## M7 — Broad phase

**Goal:** stop testing every pair. **Deliverable:** uniform grid (or spatial hash, **OPEN-6**)
producing candidate pairs; overlay draws occupied cells; measurable speed-up with many bodies.

**Tasks**

- [ ] Define a `BroadPhase` abstraction with two implementations: brute force (the oracle) and grid
- [ ] Cell coordinate computation for negative positions (floor, not truncation!)
- [ ] Insert AABBs into all overlapped cells; generate pairs; de-duplicate; canonical pair order
- [ ] Deterministic iteration order (sorted pairs or insertion-ordered structures)
- [ ] AABB region query (used later by the game layer)
- [ ] Overlay: occupied cells, candidate-pair count vs actual-contact count
- [ ] Benchmark 50 / 200 / 1000 bodies, both implementations; record results

**Scala concepts:** traits and polymorphism, `Map`/`Set`/`groupBy`, mutable builders behind an
immutable interface, `Ordering` and `given` instances, hashing and `equals` of case classes/tuples,
integer division pitfalls.

**Definition of done**

- **Key property:** for random worlds, `grid.pairs ⊇ bruteForce.overlappingPairs`, and after the
  narrow phase the contact sets are identical. No duplicate pairs. Bodies spanning many cells and
  bodies at negative coordinates are covered by generators.
- Whole-engine determinism test and all scenario tests pass with either implementation plugged in.

---

## M8 — Tiled levels & tilemap collision

**Goal:** real levels. **Deliverable:** a level drawn in Tiled loads in the browser: solid tiles
rendered as rectangles, crates from the object layer tumbling around in it.

**Tasks**

- [ ] Install Tiled; make a tiny map; agree on layer/object conventions and **write them into
      *arch §7***
- [ ] Choose the JSON library (**A-3**); model `Level` and `LevelError` ADTs
- [ ] Decoder: `.tmj` text → `Either[LevelError, Level]`; pixel→metre and y-flip conversion here
- [ ] Tile merging: solid tiles → few large static AABBs (rows first; rectangles if ambitious)
- [ ] Level → `PhysicsWorld` + entity list builder (pure)
- [ ] `web.assets`: fetch the level file; `Loading` state; show decode errors on screen
- [ ] One-way platform contact filter (*physics §10*) — can slip to M9/M11 if needed
- [ ] Observe **seam snagging** with a sliding crate on unmerged vs merged tiles; note findings

**Scala concepts:** `Either` and for-comprehensions for error handling, error ADTs, JSON decoding
(type classes / `given` codecs or manual cursor navigation), `Future`/`Promise` interop in Scala.js,
algorithms over 2D grids (`Vector[Vector[_]]` or flat `Array` + index math), `sealed` hierarchies.

**Definition of done**

- Tests: fixture `.tmj` files → expected `Level`; malformed fixtures (missing layer, unknown
  object type, wrong data length) → specific `LevelError`s; unit conversion round-trip; tile
  merging property: the merged rectangles cover **exactly** the set of solid cells (no overlap
  between rectangles, no gaps, nothing extra) for random grids.
- Scenario: a crate dropped into a loaded level rests on the tile floor.

---

## M9 — Physics-driven player & game feel

**Goal:** the heart of the game. **Deliverable:** a player body you can run and jump around a level
with; it pushes crates and rides platforms. *This milestone is mostly tuning — budget for it.*

**Tasks**

- [ ] Re-read *physics §11*; decide **OPEN-12** (player friction strategy)
- [ ] `PlayerController` as a pure function: (controller state, input, player body state, contacts)
      → (new controller state, commands for physics)
- [ ] Ground detection from contact normals; identify the ground body
- [ ] Horizontal: ground-relative velocity target with clamped acceleration; separate ground
      accel/decel and air control
- [ ] Jump impulse (+ opposite impulse to a dynamic ground body); max fall speed
- [ ] Variable jump height via gravity scale; derive `g` and jump speed from desired height and
      time-to-apex
- [ ] Coyote time and jump buffering as step counters
- [ ] `PlayerConfig` case class with all tunables; overlay shows grounded flag, counters, ground body
- [ ] Meet and fix seam snagging on tile floors (*physics §10*)
- [ ] Decide **OPEN-10** (DT 1/60 vs 1/120) now that feel and tunneling are observable
- [ ] Playtest checklist: stop on a dime? short hop vs full jump distinct? pushing a crate feels
      heavy but possible? standing on a moving platform is effortless? jumping from it carries
      momentum?

**Scala concepts:** state machines with ADTs, pure functions returning (state, outputs), command
ADTs interpreted elsewhere, case-class config and `copy`, exhaustive `match` with guards,
testing logic without its environment.

**Definition of done**

- Controller unit tests (no physics): jump fires when grounded + pressed; fires within the coyote
  window after leaving ground, not after; buffered press fires on landing within the window, not
  after; no double jump in air; releasing early switches to high gravity scale.
- Scenario tests (with physics): full jump apex height and short-hop height within tolerance of
  design values; time from rest to full speed and from full speed to rest within design values;
  player at rest on a moving platform stays on it; jumping from a platform moving at v yields
  horizontal air speed ≈ v; holding "into a wall" while falling does not slow the fall; player
  pushes a light crate, cannot move a very heavy one quickly.
- These become **golden feel tests** — future solver changes must not break them silently.

---

## M10 — Camera

**Goal:** levels larger than the screen. **Deliverable:** smooth follow camera clamped to level
bounds, interpolated like everything else.

**Tasks**

- [ ] Camera model in `game` (position in metres, view size) — pure update per fixed step
- [ ] Follow with smoothing (exponential/lerp) and a dead zone; optional look-ahead in the facing
      direction
- [ ] Clamp to level bounds; handle levels smaller than the view
- [ ] Renderer applies camera in the view transform; interpolate the camera too
- [ ] Cull off-screen tiles/bodies when drawing
- [ ] Debug: free camera mode, zoom

**Scala concepts:** small pure models, `clamp`/`lerp` reuse, extension methods for domain helpers,
frame-rate-independent smoothing (why `lerp(a, b, k)` per *fixed* step is fine).

**Definition of done**

- Tests: camera never shows outside the bounds (property over random player positions and level
  sizes); stationary player inside the dead zone ⇒ camera doesn't move; camera converges to the
  target.
- No visible jitter between the player and the background at high refresh rates.

---

## M11 — Level mechanics: platforms, crates, springs, hazards, goal

**Goal:** all MVP gameplay elements. **Deliverable:** a complete, winnable, losable level.

**Tasks**

- [ ] Sensors and contact **events** (begin/persist/end) from the physics step (*physics §9*)
- [ ] Entity ADT ↔ `BodyId` mapping; translate `PhysicsEvent`s into `GameEvent`s (*arch §3*)
- [ ] Moving platforms: kinematic bodies following a Tiled polyline path (ping-pong / loop);
      velocity set per step so friction and ground-relative control work
- [ ] Springs (**OPEN-13**); hazards; kill line below the level; goal trigger
- [ ] Crush detection event → death
- [ ] Death → respawn flow (rebuild the world from `Level`: the cheap, bug-free reset)
- [ ] One-way platforms if not done in M8
- [ ] Build 2–3 levels, each introducing one mechanic

**Scala concepts:** sealed ADTs and exhaustive matching on *pairs* of entities, extractor patterns,
`Map` updates, modelling events vs state, `enum` with fields, `collect` with partial functions,
immutability making "reset the level" trivial.

**Definition of done**

- Tests: begin/end events fire exactly once per overlap episode (scenario: body passes through a
  sensor); platform follows its path and reverses at the ends, its velocity matches its actual
  displacement per step; spring launches the player to the designed height ± tolerance; hazard
  contact and kill line produce `PlayerDied`; goal produces `LevelCompleted`; crush scenario
  produces a crush event; respawned world equals a freshly built one.
- You can play a level from spawn to goal, and die in three different ways.

---

## M12 — Circles, convex polygons (SAT), slopes

**Goal:** beyond boxes. **Deliverable:** slopes and rolling… well, *sliding* balls (no rotation
yet) in a level; the player copes with slopes.

**Tasks**

- [ ] `Shape` ADT gains `Circle` and `ConvexPolygon` (CCW, validated convex on construction)
- [ ] Narrow phase dispatch on shape pairs; circle–circle, circle–AABB
- [ ] SAT for polygon–polygon (AABB treated as a polygon) and circle–polygon (*physics §7.2*)
- [ ] World AABB per shape for the broad phase
- [ ] Tiled: slope tiles or polygon objects → static polygons
- [ ] Player on slopes: no sliding when idle; grounded threshold by normal angle (*physics §11*)
- [ ] Optionally give the player a rounded-bottom shape to reduce snagging
- [ ] Overlay: polygon outlines, SAT axis of minimum penetration

**Scala concepts:** growing an ADT and letting the compiler list every non-exhaustive match,
pattern matching on tuples, smart constructors returning `Either`/`Option`, collection
operations over vertices (`sliding`, `zip`, `minBy`), generators with invariants (random *convex*
polygons — e.g. convex hull of random points, which is an exercise in itself).

**Definition of done**

- Properties: symmetry and MTV-separates for every shape pair; SAT on box-shaped polygons agrees
  with the dedicated AABB test (normal and depth); a polygon never collides with a copy translated
  beyond the sum of bounding radii; circle–circle collision ⇔ distance < r₁ + r₂; polygon
  constructor rejects concave / clockwise / degenerate input.
- Scenarios: box on a 30° slope slides with μ = 0 and holds with high μ (compare with
  `tan θ ≤ μ`); idle player stays put on a slope; player runs up and down a slope without
  launching off the top excessively (note what you had to do about it).

---

## M13 — Sprites & animation state machine

**Goal:** make it look like a game. **Deliverable:** tileset-rendered levels, an animated player.

**Tasks**

- [ ] Image loading in `web.assets`; sprite-sheet description (frame rects, durations) as data
- [ ] Tile layer rendering from the Tiled tileset (with culling from M10)
- [ ] Animation FSM in `game`: Idle / Run / Jump / Fall (/ Push / Land) driven by controller state
      and velocity; frame advance in fixed steps
- [ ] Facing direction; sprite offset relative to the collider; draw order / simple layers
- [ ] Parallax background (optional)
- [ ] Overlay still works on top of sprites

**Scala concepts:** FSM with `enum`, pure transition functions and transition tables, `Map`-based
lookup of data-driven definitions, separating logical frames (core) from images (web),
`Future` sequencing for multiple assets.

**Definition of done**

- Tests: FSM transitions (grounded + |vx| > ε ⇒ Run; leaving ground with vy > 0 ⇒ Jump; vy < 0 ⇒
  Fall; landing ⇒ Land → Idle/Run); frame index advances and wraps per durations; no transition
  flicker when vx hovers around the threshold (hysteresis).
- The game is recognisably a platformer in a screenshot.

---

## M14 — Game flow, sleeping, polish → MVP

**Goal:** a finished small game. **Deliverable:** title screen → 5–8 levels → end screen,
published as a static site.

**Tasks**

- [ ] Top-level game-state ADT: Title / Loading / Playing / Paused / LevelComplete / Finished;
      pure transition function; step outputs as commands to `web` (**A-4**)
- [ ] Level list and progression; level timer and death counter; HUD
- [ ] Sleeping (*physics §8*, **OPEN-9**) + overlay colour for sleeping bodies
- [ ] Build the remaining levels; playtest with another human; adjust difficulty curve
- [ ] Polish pass: screen transitions, respawn delay, small camera shake on landing/death
      (optional)
- [ ] `fullLinkJS` production build; publish (e.g. GitHub Pages); README with controls and credits
- [ ] Retrospective: update the design docs to match reality; close remaining `OPEN`s; write down
      what you would do differently

**Scala concepts:** larger-scale ADT design, nested state, total transition functions, `fullLinkJS`
optimisation, basic CI (GitHub Actions running `sbt test`) if desired.

**Definition of done**

- Tests: game-state transitions (can't pause from Title, LevelComplete → next level or Finished,
  death keeps the level); sleeping — resting crate sleeps after the timeout, wakes when hit, wakes
  when its support is removed (the floating-crate test), player never sleeps; sleeping on/off
  does not change the outcome of the golden scenarios beyond tolerance.
- End-to-end headless test: level 1 + scripted input ⇒ `LevelCompleted`.
- Someone else finishes the game from the public URL without your help.

---

# Stretch milestones (optional, any order unless noted)

## S1 — Rotation and angular velocity  *(big: 25–40 h)*

Add orientation, angular velocity, torque, moment of inertia per shape; rotation matrix / `Rot`
type; oriented boxes via SAT with rotated axes; **contact points via reference/incident edge
clipping** (two-point manifolds); the full impulse formula with `r × n` terms; friction with
rotation. The player keeps rotation locked (infinite inertia).
**Concepts:** extending ADTs and formulas under a large test-suite; more type-level safety
(opaque `Radians`). **Done when:** tumbling crates stack stably; properties: angular + linear
momentum conserved in free collisions; all MVP golden tests still pass.

## S2 — Joints and ropes  *(15–25 h; ropes look best after S1 but work without)*

Distance constraint solved with sequential impulses alongside contacts; rope = chain of small
bodies with distance (max-length) joints; spring joint; swinging platform; player grab/release.
**Concepts:** generalising "contact" into a `Constraint` abstraction (trait or ADT — a real design
decision). **Done when:** a 10-segment rope hangs stably for 60 s, tests assert segment lengths
stay within tolerance; a pendulum's period ≈ `2π√(L/g)` for small angles.

## S3 — Replay system  *(8–12 h)*

Record `InputSnapshot` per step + level ID + config; play back; ghost of best run; use recorded
runs as regression tests. **Concepts:** serialisation codecs, determinism as a feature,
`LazyList`/iterators. **Done when:** a recorded run replays to the identical final state 100/100
times; one replay per level runs in CI.

## S4 — In-game level editor  *(20–40 h)*

Place/remove tiles and objects with the mouse, play-test instantly, export to `.tmj`-compatible
JSON (or your own format). **Concepts:** encoder side of codecs, command pattern with undo/redo
as an immutable history list, mouse input and screen→world transforms. **Done when:** decode(encode(level)) == level (property test), and a level made only in your editor ships in the game.

## S5 — Juice: audio, particles, gamepad  *(10–20 h)*

Web Audio for SFX triggered by `GameEvent`s (impact volume from impulse magnitude!), a tiny
particle system (non-colliding, render-side or core-side — decide), Gamepad API.
**Concepts:** more JS interop/facades, keeping effects out of `core` via output commands.
**Done when:** landing, pushing, springs and death all have audio-visual feedback, and `core`
still has no DOM dependency.

## Other ideas (unsized)

Wall slide/jump · carry & throw · checkpoints · `localStorage` progress · continuous collision for
fast bodies · contact persistence + warm starting · island-based sleeping · spatial hash with
incremental updates · a JVM desktop front-end reusing `core` unchanged (proof the architecture
holds).
