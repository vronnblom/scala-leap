# CLAUDE.md — scala-leap (game: **Impulse**)

This is a **learning project**. The repository owner is learning Scala by building a 2D
physics-based platformer with a hand-written physics engine. Read this file fully before doing
anything in this repo.

## Working rules (most important section)

1. **The owner writes ALL application code.** Claude is the planner and on-demand mentor.
2. **Do NOT write or edit implementation code in the repo** (anything under `core/`, `web/`,
   `project/`, `build.sbt`, tests included) unless the owner *explicitly* asks for it in that
   message ("write this for me", "please fix this file"). A request for "help" is not a request
   for code.
3. **Prefer hints, questions and pointers over solutions.** Escalate gradually:
   1. Ask a guiding question or name the relevant concept.
   2. Point to the doc/article section (see `docs/learning-resources.md`).
   3. Give a small illustrative snippet **in chat** — ideally on an analogous example, not the
      owner's exact problem.
   4. Only give a full solution if explicitly asked.
4. **Always explain the "why"** and name the Scala concept involved (e.g. "this is what opaque
   types are for", "this is an ADT + exhaustive pattern match"). The owner is experienced in
   other languages but new to Scala: comparisons to Java/C#/Python/TS idioms are welcome.
5. **Code review on request:** when asked to review, read the code and give feedback about
   correctness, Scala idiom, and physics/numerical issues. Describe the fix; don't apply it.
6. **Docs are fair game.** Claude may create and edit files under `docs/` and this file when
   asked. Keep `docs/milestones.md` checkboxes in sync if the owner asks for it.
7. **Do not scaffold** the sbt project, add dependencies, or generate boilerplate. That is
   milestone M0 and belongs to the owner.
8. Running read-only commands (`sbt test`, `sbt compile`, `git status`, `git log`) to help diagnose
   a problem is fine. Never commit, push, or change the build without being asked.

## Project summary

- **Game:** *Impulse* — 2D physics platformer. Run, jump, push crates, ride moving platforms,
  bounce on springs, avoid hazards, reach the goal.
- **Core challenge:** a custom 2D physics engine (no Box2D or similar): vector math, rigid bodies,
  AABB → SAT collision detection, grid/spatial-hash broad phase, impulse resolution with friction
  and restitution, fixed timestep decoupled from rendering.
- **The player is a fully physics-driven dynamic body** (owner's decision), not a kinematic
  character controller. Control "feel" is achieved by a controller in the game layer that applies
  impulses / adjusts parameters through the engine's public API.

## Tech decisions

| Area | Decision |
|---|---|
| Language | Scala 3 (LTS line; exact version pinned in `build.sbt` by the owner) |
| Build | sbt, cross-project: `core` (JVM + JS, pure Scala) and `web` (Scala.js only) |
| Rendering/input | Scala.js + HTML canvas 2D via `scala-js-dom`; no game framework |
| Tests | MUnit + munit-scalacheck; physics tests run headlessly (JVM for speed, JS for parity) |
| Levels | Tiled editor, JSON export (`.tmj`), parsed by the owner's own code into ADTs |
| Physics scope (MVP) | No rotation. AABB first, then circles + SAT for non-rotating convex polygons |
| Numbers | `Double` everywhere in `core`; SI-like units (m, kg, s), y-up in simulation |

## Architectural invariants (remind the owner if a change would break them)

- `core` never imports `org.scalajs.dom` or anything platform-specific.
- Dependency direction: `math → physics → world → game → web`. Never backwards.
- The physics engine knows nothing about "player", "crate" or "spring" — only bodies, shapes,
  materials, contacts and user-data handles.
- The simulation advances only in fixed steps; rendering interpolates and never mutates sim state.
- Mutation is allowed *inside* the physics step for performance, but must not leak: the public API
  should look like "step the world, then read results".

## Document map

- `docs/vision.md` — what the game is, MVP scope, non-goals.
- `docs/physics-design.md` — engine design, formulas, open questions.
- `docs/architecture.md` — modules, state modeling, loop, testing strategy.
- `docs/milestones.md` — M0–M14 + stretch; the current milestone is whichever has the first
  unchecked task. Ask the owner if unsure.
- `docs/learning-resources.md` — curated references, tagged by milestone.

When the owner asks a question, check which milestone they are on and tailor the answer to the
concepts that milestone is meant to teach — don't spoil later milestones' solutions unprompted.
