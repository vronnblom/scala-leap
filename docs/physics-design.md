# Impulse — Physics Engine Design (first draft)

Status: **draft**. Items marked **OPEN-n** are undecided; they are collected in §13. Formulas are
given conceptually — deriving and implementing them is the owner's job.

## 1. Goals and non-goals

**Goals**

- Stable, predictable and *fun* — plausible rather than accurate.
- Deterministic: same initial state + same input sequence ⇒ same result (on the same platform).
- Headless: pure Scala, no rendering or DOM dependency; unit- and property-testable.
- Small and understandable: every part fits in the owner's head.
- Good enough for: a few hundred bodies, stacks of ~5 crates, a player body that feels tight.

**Non-goals (MVP)**

- Rotation / angular dynamics (stretch S1). All MVP bodies have a fixed orientation.
- Continuous collision detection, joints, fluids, soft bodies, concave shapes.
- Generic, reusable engine API.

## 2. Units and coordinate system

- **Units:** metres, kilograms, seconds. A tile is 1 m × 1 m; the player is roughly 0.8 × 1.6 m.
  Rationale: solvers behave best with values near 1; tolerances (slop, sleep thresholds) get
  meaningful names; tuning gravity and jump height becomes intuitive.
- **Axes:** x right, **y up** in simulation space. Gravity is `(0, −g)`.
- **Rendering converts** to pixels and flips y (canvas is y-down) in exactly one place: the
  camera/view transform in the `web` module. Tiled is also y-down in pixels; the level loader
  converts once on import.
- **Numeric type:** `Double`. (`Float` on Scala.js has subtle semantics and no real benefit here.)
- Gravity will probably *not* be 9.81. Platformers usually use much stronger gravity (20–60 m/s²)
  for snappy jumps. Tune `g`, jump speed and run speed from desired *jump height* and *time to
  apex*: `g = 2h / t²`, `v_jump = 2h / t`.

## 3. Vector math

One small immutable 2-component vector type (`Vec2`) plus helpers. Required operations:

| Operation | Notes |
|---|---|
| `+`, `−`, unary `−`, scalar `*` and `/` | The basics; all return new values |
| `dot(a, b)` | Projection, angle tests, SAT, impulse along normal |
| `cross(a, b)` → scalar | 2D "cross product" `a.x·b.y − a.y·b.x`; signed area / winding; needed for SAT polygon winding, essential for rotation later |
| `cross(v, s)` / `cross(s, v)` → vector | Only needed for rotation (S1) |
| `length`, `lengthSquared` | Prefer squared for comparisons (no sqrt) |
| `normalized` | **Pitfall:** zero-length vector. Decide the policy (**OPEN-1**): return zero, return `Option`, or require a fallback direction |
| `perp` | `(−y, x)`: edge normals, tangent from normal |
| `lerp`, `clamp`, `min`/`max` component-wise | Interpolation for rendering, AABB unions |
| `distance`, `distanceSquared` | Circles |
| approximate equality | `|a − b| ≤ ε`; tests need it because floating-point laws hold only approximately |

Also: an `AABB` value (min, max) with `overlaps`, `contains`, `union`, `expanded(margin)`,
`center`, `halfExtents`.

**Scala angle:** this is where you meet case classes, operator methods, extension methods
(`2.0 * v`), `inline`, companion objects, and possibly opaque types for units/IDs. It is also the
perfect first target for property-based tests (commutativity, `|a·b| ≤ |a||b|`,
`perp(v)·v = 0`, triangle inequality — all "approximately").

## 4. Bodies

| Type | Inverse mass | Moved by | Affected by collisions | Used for |
|---|---|---|---|---|
| **Static** | 0 | nothing | no (others bounce off it) | tiles, walls |
| **Kinematic** | 0 | its own velocity, set by game code | no | moving platforms, crushers |
| **Dynamic** | 1/m > 0 | forces, impulses, gravity | yes | player, crates, balls |

Key idea: store **inverse mass**. Infinite mass becomes `invMass = 0`, and every formula
"just works" without special cases — a static body receives `impulse × 0` velocity change.

Per-body state: position, velocity, accumulated force (cleared each step), inverse mass,
gravity scale (needed by the player's variable jump), linear damping (optional), shape, material,
body type, flags (sensor, sleeping, one-way), sleep timer, and an opaque **user-data handle /
body ID** so the game layer can map a body back to an entity without the engine knowing what an
entity is. Body modelling as an ADT (enum) vs. flags is **OPEN-2**.

**Material:** friction coefficient(s) μ and restitution e. Two bodies' materials must be
*combined* per contact. Common choices: friction = √(μa·μb) (geometric mean), restitution =
max(ea, eb). (**OPEN-3**: single μ or separate static/dynamic μ? Start with a single μ.)

**Collision filtering:** category/mask bits or a simple layer enum so that e.g. sensors and
decorative debris can ignore each other. Keep it minimal.

## 5. Shapes

Introduced in this order:

1. **AABB** (M5): half-extents around the body position. All tiles, crates and the first player.
2. **Circle** (M12): radius. Balls; also handy as the player's "feet" to avoid seam snagging.
3. **Convex polygon** (M12): vertices in counter-clockwise order relative to the body origin;
   *not rotating* in the MVP. Slopes (triangles), trapezoids.

Shape is a natural **sealed ADT / Scala 3 `enum`**; narrow phase is a pattern match on the pair of
shapes (a "double dispatch" table). Each shape can compute its world AABB for the broad phase.
One shape per body in the MVP (compound shapes: **OPEN-4**, probably unnecessary).

## 6. Integration

**Semi-implicit (symplectic) Euler**, per dynamic, awake body, per fixed step `dt`:

```
v ← v + (gravity · gravityScale + force · invMass) · dt      (velocity first…)
v ← v · damping factor                                       (optional)
x ← x + v · dt                                               (…then position with the NEW velocity)
```

Why not explicit Euler (position first with the old velocity)? It gains energy over time and
explodes with springs/stiff contacts. Semi-implicit Euler is just as cheap and much more stable.
RK4 is unnecessary: collisions dominate the error, not integration.

Kinematic bodies integrate position from velocity only. Static bodies are skipped.

**Step order (**OPEN-5** — two common orders, pick one and document why):**

```
A) integrate velocities → detect collisions → solve velocity constraints → integrate positions → correct positions
B) detect collisions → solve impulses → integrate velocities and positions → correct positions
```

(A) is the Box2D-style order and gives better resting contact because gravity's velocity is
cancelled by the solver *before* it moves the body. Recommended; but (B) is what Randy Gaul's
tutorial does and is easier to start with.

**Speed cap:** clamp `|v|` to a maximum (e.g. such that a body never moves more than ~half of the
thinnest collider per step). This is the MVP's anti-tunneling measure.

## 7. Collision pipeline overview

```
            all bodies
                │  world AABBs
        ┌───────▼────────┐
        │  BROAD PHASE   │  uniform grid / spatial hash → candidate pairs (cheap, conservative)
        └───────┬────────┘
                │  filter: static-static, sleeping-sleeping, layer masks, same body
        ┌───────▼────────┐
        │  NARROW PHASE  │  exact test per shape pair → Option[Manifold]
        └───────┬────────┘
                │  manifolds (normal, penetration, contact point(s), body pair)
        ┌───────▼────────┐
        │  RESOLUTION    │  sensors → events only; others → iterative impulses + positional correction
        └───────┬────────┘
                ▼
       contact/trigger events for the game layer (begin / persist / end)
```

### 7.1 Broad phase

Start with **brute force O(n²)** (M5) — it is the *oracle* the real broad phase will be tested
against. Then (M7) a **uniform grid**: cell size ≈ 1–2× the typical body size (the tile size is a
natural choice). Each body's AABB is inserted into every cell it overlaps; bodies sharing a cell
form candidate pairs; pairs must be **de-duplicated** (a pair can share several cells) and ordered
canonically (lower ID first) for determinism.

- Fixed dense grid vs. **spatial hash** (hash of cell coords → bucket, unbounded world): **OPEN-6**.
  Levels are bounded, so a dense grid is simpler; spatial hash is the more instructive exercise.
- Static tiles: don't insert thousands of tile bodies. Either query the tile grid directly
  (tilemap *is* a grid) or merge tiles into larger static colliders at load time (see §10).
- Rebuild-every-step is fine at this scale; incremental update is an optimisation for later.

**Correctness property (great ScalaCheck target):** for any random set of AABBs,
`gridPairs ⊇ bruteForceOverlappingPairs` (no false negatives), and after the narrow phase both
give exactly the same set.

### 7.2 Narrow phase

Output for a colliding pair is a **manifold**: collision **normal** `n` (unit, pointing from A to
B — pick a convention and never deviate), **penetration depth**, and one or two **contact points**
(points only matter for debug drawing until rotation exists).

- **AABB vs AABB:** compute overlap on x and on y. If either ≤ 0 → no collision. Otherwise the
  axis with the *smaller* overlap is the separation axis; normal sign comes from the relative
  position of the centres. (This "minimum overlap axis" idea is exactly SAT specialised to boxes.)
- **Circle vs circle:** compare squared centre distance with squared radius sum. Normal = centre
  difference normalised (handle coincident centres!).
- **Circle vs AABB / polygon:** closest point on the box/polygon to the circle centre; special
  case when the centre is inside.
- **SAT (polygon vs polygon):** two convex shapes are separated iff there exists an axis on which
  their projections don't overlap; candidate axes are the edge normals of both polygons. Project
  all vertices onto each axis (dot products), compute interval overlap; any gap → early out.
  Otherwise the axis with **minimum overlap** gives the normal and depth (the *minimum translation
  vector*). Ensure the normal points from A to B. Contact points via reference/incident edge
  clipping can wait for rotation (S1); until then a single approximate point is enough.

Properties to test: symmetry (`collide(a,b)` and `collide(b,a)` agree with flipped normal);
translating B by `n · depth` (plus ε) separates the shapes; AABB-as-polygon through SAT agrees
with the dedicated AABB test; shapes far apart never collide.

### 7.3 Resolution — normal impulse

For a contact with normal `n` (A → B):

```
v_rel  = vB − vA
v_n    = v_rel · n                      (negative ⇒ approaching)
if v_n > 0: bodies are separating → do nothing
j      = −(1 + e) · v_n / (invMassA + invMassB)
vA    −= j · invMassA · n
vB    += j · invMassB · n
```

Intuition: we want the post-collision normal velocity to be `−e · v_n`. An impulse `j·n` changes
each body's velocity in proportion to its inverse mass; solve for the `j` that yields the target.
`e = 0` → perfectly inelastic (no bounce), `e = 1` → perfectly elastic. If both inverse masses are
0 (static vs kinematic), skip.

### 7.4 Friction (Coulomb model)

After the normal impulse, recompute `v_rel`, then:

```
t    = normalize(v_rel − (v_rel · n) n)          (tangent: the sliding direction; skip if ~0)
j_t  = −(v_rel · t) / (invMassA + invMassB)      (impulse that would stop sliding completely)
clamp: |j_t| ≤ μ · j                             (Coulomb: friction can't exceed μ × normal impulse)
apply ∓ j_t · t like the normal impulse
```

Inside the cone (`|j_t| ≤ μ j`) the contact "sticks" (static friction); outside it slides with a
capped impulse (dynamic friction). Friction is what makes crates ride platforms and lets the
player stand on a moving platform without sliding off — so it is central to this game.

### 7.5 Positional correction

Impulses fix velocities, but floating-point error and gravity leave bodies slightly overlapping;
resting objects slowly **sink**. Fix by nudging positions apart directly:

```
correction = max(depth − slop, 0) · percent / (invMassA + invMassB) · n
xA −= correction · invMassA ;  xB += correction · invMassB
```

`slop` (≈ 0.005–0.01 m) is tolerated penetration that prevents jitter; `percent` (≈ 0.2–0.8) how
aggressively to correct per step. Alternative: **Baumgarte stabilisation** (feed the penetration
into the velocity target as a bias) — adds energy/bounce; linear projection as above is simpler.
(**OPEN-7**)

### 7.6 Iterations

Resolving contacts one at a time breaks contacts solved earlier (crate stack: fixing the top pair
pushes the bottom pair back into overlap). Standard cure: **sequential impulses** — loop over all
contacts several times (4–10 iterations); the solution converges. A more correct version
**accumulates** the total impulse per contact and clamps the accumulated value (≥ 0 for normal,
within ±μ·jₙ for friction) rather than each increment; this matters for stable stacks and is the
natural M6 upgrade. **Warm starting** (reusing last step's accumulated impulses) is an optional
refinement that requires persistent contacts (**OPEN-8**).

## 8. Stability and sleeping

- **Resting contact & restitution threshold:** with `e > 0`, a resting body keeps micro-bouncing
  on gravity's per-step velocity. Use `e = 0` when `|v_n|` is below a threshold (~1 m/s, or
  compare with `g·dt`).
- **Deterministic ordering:** iterate bodies and contacts in a stable order (by ID). Avoid
  iteration over hash maps with unspecified order in the solver.
- **Sleeping:** a dynamic body whose speed stays below a threshold for ~0.5 s is put to sleep: not
  integrated, not tested against other sleeping/static bodies. **Wake** on: contact with an awake
  dynamic or moving kinematic body, an applied impulse/force, or a neighbour being removed. The
  classic bug is the *floating crate* (support removed, crate never wakes). Proper engines sleep
  whole **islands**; the MVP sleeps individual bodies and wakes everything touching a waking body
  (**OPEN-9**). The player body never sleeps.
- **Tunneling:** fast or thin things pass through each other in one step. MVP mitigations: speed
  cap (§6), colliders no thinner than ~0.25 m, merged tile colliders, and a smaller `dt` (1/120)
  if needed (**OPEN-10**: 1/60 vs 1/120 step). True CCD is out of scope.
- **Crushing:** a dynamic body between a kinematic body and a static one cannot be resolved.
  Detect "penetration stays above X for N steps" and report it as an event (the game kills the
  player / breaks the crate).

## 9. Sensors, events and queries

- **Sensor** shapes detect overlap but generate no impulses: goal, hazards, pressure plates.
- The world reports **contact events** after each step: `Begin(a, b)`, `Persist(a, b, manifold)`,
  `End(a, b)`, distinguishing sensor overlaps from solid contacts. Requires remembering last step's
  pair set (a set difference). Events are plain immutable data → an ADT.
- **Queries** the game layer needs: AABB overlap query, point query, maybe a ray cast (for ground
  probing or line-of-sight; **OPEN-11**, add only when needed).

## 10. Tilemap collision specifics

- **Don't make one body per tile.** Either merge solid tiles into maximal rectangles/horizontal
  strips at load time, or let the narrow phase query the tile grid directly. Merging is easier to
  reason about and reduces the next problem.
- **Ghost edges / seam snagging:** a box sliding along a floor made of adjacent tiles can catch on
  the internal vertical edge between two tiles (the solver sees a tiny wall). Mitigations: merge
  tiles (fewer seams); ignore contacts whose normal points into a neighbouring solid tile (the
  edge is "internal"); give the player a rounded bottom (circle/capsule-like polygon) once M12
  lands. The player being physics-driven makes this problem *certain* to appear — plan to meet it
  in M8/M9 and treat it as a learning moment.
- **One-way platforms:** collide only if the body is moving downward relative to the platform
  *and* was above the platform surface at the start of the step; otherwise drop the contact.
  Implemented as a contact filter hook between narrow phase and resolution.
- **Slopes** arrive with SAT (M12) as static triangles. A dynamic body on a slope slides unless
  friction holds it — see §11.

## 11. The player: a fully physics-driven body

**Decision:** the player is an ordinary **dynamic body** resolved by the same solver as crates.
No kinematic move-and-slide. This is the harder road for game feel, chosen deliberately: it makes
every interaction emergent (pushing, being pushed, riding, weight on springs, recoil when jumping
off a light crate) and keeps the engine honest.

**Architecture rule:** the engine has no concept of a player. A `PlayerController` in the *game*
layer reads input + last step's contacts and, **once per fixed step before `world.step`**, acts on
the player body through the public API only: apply impulse/force, set gravity scale, read
velocity and contacts.

### 11.1 Techniques

| Concern | Approach |
|---|---|
| **Horizontal movement** | *Velocity-target with clamped acceleration*: compute the desired horizontal velocity from input; apply the impulse that moves the current velocity toward it, limited by `maxAccel · dt · mass`. Separate limits for ground accel, ground decel (stopping/turning), and air control. Because it is a limited impulse, external momentum (spring launch, explosion) is not erased instantly — especially in the air. |
| **Ground-relative motion** | The target velocity is relative to the **ground body's velocity** (the body under the player's feet). Standing still on a moving platform then means "target = platform velocity", and jumping inherits the platform's velocity for free. |
| **Player friction** | If the player's material has friction, holding "right" against a wall lets friction hold the player up, and braking depends on surface μ. Usual solution: player μ ≈ 0 and the controller does all ground braking itself (above). Then the *player* no longer rides platforms by friction — ground-relative targeting replaces it. Crates still use real friction. (**OPEN-12**: alternatively per-contact friction override: μ = 0 for wall-ish normals, normal μ for floor-ish normals.) |
| **Ground detection** | From contact manifolds: the player is grounded if any solid contact has a normal whose up-component (from the player's perspective) exceeds a threshold (e.g. cos 50°). No separate ray/foot sensor needed, though a small foot sensor is a valid alternative. One step of latency is fine — coyote time hides it. |
| **Jump** | Set/raise vertical velocity to `v_jump` via an impulse (`Δv · mass`). Apply the **equal and opposite impulse to the ground body** if it is dynamic — that is what makes light crates kick away (pillar 1). |
| **Variable jump height** | Gravity scale: normal while rising with jump held; ×2–3 when jump is released early or when falling. Pure parameter change, no teleporting. |
| **Coyote time / jump buffer** | Counters measured in **fixed steps** (e.g. 6 steps ≈ 0.1 s at 60 Hz) inside the controller's small state machine. Deterministic because they tick with the simulation, not wall-clock. |
| **Restitution** | Player e = 0 so landing never bounces. Consequence: springs can't rely on restitution with `max` combine… (**OPEN-13**: spring = sensor that applies a fixed launch impulse [predictable, recommended] vs. high-restitution surface [emergent, less controllable].) |
| **Slopes** | With μ ≈ 0 the player slides down slopes when idle. Options: controller cancels the tangential component of gravity while grounded with no input, or per-contact friction (OPEN-12). |
| **Mass ratios** | Player ~60–80 kg; crates 10–200 kg. Huge ratios (>10:1) stress the iterative solver; keep them modest and let level design do the rest. |
| **Max fall speed** | Clamp, for feel and tunneling. |

### 11.2 Known risks (and what they teach)

- Jitter when standing on stacked dynamic crates → needs accumulated impulses + slop (M6).
- Seam snagging on tile floors (§10).
- Squeezing through gaps / being crushed by kinematic platforms → crush event (§8).
- Feel tuning is coupled with solver parameters — changing iterations or `dt` changes feel.
  Mitigation: a tuning panel / constants file and the debug overlay; tune on the final `dt`.
- "Floaty" feel if acceleration limits are low; "ice-skating" if decel is low. Tune from target
  numbers (time to full speed ≈ 0.1 s, time to stop ≈ 0.05–0.08 s).

The fallback, if this becomes a motivation-killer: keep the player dynamic but let the controller
*set* horizontal velocity directly while grounded. It is still physics-driven vertically and in
interactions. Note it here as an escape hatch, not the plan.

## 12. Testing strategy (engine-level)

- **Property tests:** vector laws; AABB algebra; narrow-phase symmetry and MTV-separates;
  broad phase ⊇ brute force; impulse resolution conserves momentum (`Σ m·v` unchanged for
  dynamic–dynamic pairs) and never makes bodies approach faster; with `e = 1` and no friction,
  kinetic energy is conserved; with `e < 1` it never increases.
- **Scenario tests (headless, step N times, assert):** ball dropped from height h lands at the
  analytically expected time (± a step); box rests on floor without sinking more than slop after
  600 steps; 5-crate stack stays standing for 10 simulated seconds; crate on a moving platform
  stays on it; body at cap speed doesn't tunnel through a 1-tile wall; sleeping body wakes when its
  support is removed.
- **Determinism test:** run the same scenario twice → bit-identical state. Optionally compare JVM
  and JS runs (may differ in the last bits if `math` functions like `sin` are involved; +, −, ×, ÷
  and `sqrt` are IEEE-exact on both).
- **Golden/regression tests:** once feel is tuned, record "jump height = X, jump distance at full
  run = Y" as tests so later solver changes that alter feel are noticed.

## 13. Open design questions

| # | Question | Leaning |
|---|---|---|
| OPEN-1 | `normalized` on a zero vector: zero / `Option` / fallback param? | Safe variant with fallback; tests decide |
| OPEN-2 | Body type as enum with per-type data vs. one class with `invMass = 0` + flags? | One class + enum tag; ADT for shapes |
| OPEN-3 | Single μ vs static/dynamic μ; combine rules | Single μ, geometric mean; e = max |
| OPEN-4 | Compound shapes per body? | No (MVP) |
| OPEN-5 | Step order A (Box2D-style) or B (tutorial-style)? | Start B in M5, move to A in M6 if resting contact is poor |
| OPEN-6 | Dense grid vs spatial hash | Dense grid first; hash as an exercise |
| OPEN-7 | Linear projection vs Baumgarte | Linear projection |
| OPEN-8 | Persistent contacts + warm starting? | Only if stacks are unstable after accumulated impulses |
| OPEN-9 | Per-body sleeping vs islands | Per-body + wake neighbours |
| OPEN-10 | Fixed `dt` 1/60 vs 1/120 | 1/60; revisit at M9 when feel and tunneling are testable |
| OPEN-11 | Ray casts needed? | Defer until a feature demands it |
| OPEN-12 | Player friction: μ = 0 + controller braking vs per-contact override | μ = 0 + ground-relative targeting |
| OPEN-13 | Springs: sensor + impulse vs restitution | Sensor + impulse |
| OPEN-14 | World storage: immutable `Vector[Body]` copied per step vs mutable arrays inside the world | Start immutable/simple, measure, then decide (see architecture.md §4) |
| OPEN-15 | Body identity: index, opaque-type ID, or generational handle (safe removal)? | Opaque `BodyId`; think about removal during iteration |

Record each decision (and the reason) here when it is made — future-you will want the history.
