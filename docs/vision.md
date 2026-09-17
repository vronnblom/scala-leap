# Impulse — Vision

> A 2D platformer where everything — including you — obeys the same hand-written physics.

## 1. Pitch

*Impulse* is a single-player 2D platformer. The player character is a real rigid body inside a
custom physics engine: it has mass, it is pushed by moving platforms, it shoves crates and is
shoved back, it launches off springs and inherits momentum from whatever it was standing on.
Levels are small, hand-built puzzle-courses made in Tiled. Getting to the goal means
understanding and exploiting the physics, not just pressing jump at the right time.

The project has two equally important purposes:

1. **Learn Scala 3** by building something non-trivial end to end.
2. **Learn game physics** by writing the engine from scratch.

When these conflict with "make the best possible game", learning wins.

## 2. Design pillars

1. **Physics is the toy.** Every mechanic is a consequence of the engine (mass, impulse, friction,
   restitution), not a scripted special case. If a crate can be pushed, it is because the solver
   pushed it.
2. **Tight controls on top of real physics.** The player is a dynamic body, but must still *feel*
   like a good platformer: responsive acceleration, coyote time, jump buffering, variable jump
   height. The controller shapes the forces; it never cheats by teleporting.
3. **Everything is visible.** A debug overlay (hitboxes, contact points and normals, velocities,
   broad-phase cells, sleeping state) is a first-class feature, built early and kept working.
4. **Deterministic and testable.** Same inputs → same simulation. The engine runs headlessly in
   unit tests with no browser.
5. **Small and finished beats big and abandoned.** Each milestone ends in something that runs.

## 3. Core gameplay loop

```
enter level → read the layout → traverse (run / jump / push / ride / bounce)
      ↑                                        │
      │                          hazard or fall → quick respawn at level start / checkpoint
      │                                        │
      └──────── next level ← reach the goal ←──┘
```

Moment to moment: move with momentum, jump precisely, reposition physics objects to create routes
(a crate as a step, a crate as a counterweight on a spring, a moving platform's velocity as a
launch boost).

## 4. Player abilities

### MVP

- **Run** with acceleration and deceleration; distinct ground and air control.
- **Jump** with variable height (hold = higher, release early = short hop).
- **Coyote time** (jump still allowed a few ticks after walking off a ledge).
- **Jump buffering** (jump pressed slightly before landing still fires).
- **Push** dynamic objects by running into them (emergent from collision resolution).
- **Ride** moving platforms and crates; **inherit their velocity** on jump.
- **Bounce** on springs.

### Later (not MVP)

- Wall slide / wall jump.
- Pick up, carry and throw crates.
- Grab and swing on ropes (requires the joints stretch milestone).
- Dash.

## 5. Why the physics is the fun

| Engine feature | What it becomes in the game |
|---|---|
| Mass & impulse resolution | Heavy crates are slow to push; light ones fly. Jumping off a light crate kicks it away. |
| Friction | Crates stay on moving platforms; icy surfaces (low μ) as a level theme. |
| Restitution | Bouncy surfaces and springs; bouncy crates as a puzzle element. |
| Kinematic bodies | Moving platforms that carry and launch the player; crushers. |
| Momentum inheritance | Jumping from a fast platform gives a longer jump — a core "aha" trick. |
| Stacking stability | Crate staircases the player builds to reach high ledges. |
| Triggers (sensor shapes) | Goal, hazards, checkpoints, pressure plates. |
| Joints (stretch) | Ropes, swinging platforms, bridges. |

## 6. Win / lose conditions

- **Win a level:** the player body overlaps the goal trigger.
- **Win the game:** finish the last level; show total time and deaths.
- **Lose (soft):** touching a hazard, falling below the level's kill line, or being crushed
  (deep unresolved penetration between a kinematic body and something immovable). Result: instant
  respawn at level start (checkpoints are "later"). No lives, no game over screen.

## 7. Scope

### MVP (end of milestone M14)

- Custom physics: Vec2 math, static/kinematic/dynamic bodies, AABB + circle + non-rotating convex
  polygon (SAT), uniform-grid broad phase, impulse solver with friction, restitution and positional
  correction, sleeping, sensors/triggers, fixed timestep.
- Physics-driven player with the MVP abilities above.
- Level elements: solid tiles, slopes, one-way platforms, moving platforms, crates, springs,
  hazards (spikes/kill zones), goal.
- Tiled `.tmj` level loading; 5–8 short levels that each teach one idea.
- Camera that follows the player, clamped to level bounds.
- Sprite rendering with a small animation state machine (idle/run/jump/fall) — placeholder or free
  art is fine.
- Debug overlay toggle, pause, single-step.
- Title screen → levels → end screen. Keyboard input.

### Later / stretch

- Rotation and angular velocity; joints, ropes; replay system; in-game level editor; audio;
  particles; gamepad input; checkpoints; wall jump, carry/throw; save progress to `localStorage`.

## 8. Non-goals

- **No off-the-shelf physics** (Box2D, Rapier, planck.js…) and **no game framework**. A JSON
  library and `scala-js-dom` are the only expected runtime dependencies.
- Not a general-purpose, reusable engine. It serves this game.
- No physical *accuracy* goal. Stable and fun beats correct.
- No 3D, no multiplayer/networking, no procedural generation.
- No mobile/touch support, no WebGL (canvas 2D is enough).
- No continuous collision detection in the MVP (mitigate tunneling with speed caps and tile
  thickness instead).
- No performance target beyond "a few hundred bodies at 60 Hz in a desktop browser".
- No original art/music requirement; free asset packs and coloured rectangles are acceptable.

## 9. Success criteria

- A friend can open the page, finish the levels, and at least once say "oh, *that's* how you do it".
- `sbt test` runs the physics test-suite headlessly in seconds.
- The owner can explain every line of the solver and every Scala feature used.
