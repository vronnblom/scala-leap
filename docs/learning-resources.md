# Impulse — Learning Resources

Curated, not exhaustive. Tags like **[M1]** say which milestone a resource serves; read it *when
you get there*, not up front. ⭐ = start here for that topic.

> Links were written from memory and not re-verified when this file was created. Some articles
> (notably the Tuts+ ones) have moved hosts over the years — if a link is dead, search for the
> title + author. Fix links here as you find them.

## 1. Scala 3 language

- ⭐ **The Scala 3 Book** — https://docs.scala-lang.org/scala3/book/introduction.html
  The official guided tour. Chapters to prioritise: *A Taste of Scala*, *Domain Modeling*
  (case classes, enums, ADTs), *Methods*, *Functions*, *Collections*, *Contextual Abstractions*.
  **[M0–M3]**
- ⭐ **Scala for Java developers** (also JS and Python versions in the same section) —
  https://docs.scala-lang.org/scala3/book/scala-for-java-devs.html — fastest on-ramp if you come
  from another language. **[M0]**
- **Scala 3 Reference** — https://docs.scala-lang.org/scala3/reference/ — precise descriptions of
  individual features. Look up as needed:
  - Enums & ADTs **[M3, M8, M11]** · Extension methods **[M1]** · Opaque type aliases **[M3]**
  - `given`/`using`, type classes **[M1 (ScalaCheck), M7 (Ordering), M8 (codecs)]**
  - `inline` **[M1, M6]** · Union types, `Matchable`, export clauses (nice to know)
- **Scala standard library API** — https://www.scala-lang.org/api/3.x/ — especially the
  collections: `Vector`, `Map`, `Set`, `Option`, `Either`.
- **Collections overview & performance characteristics** —
  https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html **[M5–M7]**
- **Scala Toolkit tutorials** (MUnit, uPickle, etc. in small recipes) —
  https://docs.scala-lang.org/toolkit/introduction.html **[M0, M8]**
- Books (optional): *Programming in Scala, 5th ed.* (Odersky et al. — thorough, Scala 3);
  *Functional Programming in Scala, 2nd ed.* (Chiusano, Bjarnason, Pilquist — deep FP; its
  chapters on property-based testing and purely functional state are directly relevant);
  *Hands-on Scala Programming* (Li Haoyi — pragmatic; Scala 2 but transfers well).
- **Scala Exercises** — https://www.scala-exercises.org/ — koans-style practice (Scala 2 syntax
  mostly, concepts carry over).
- Community help: Scala Discord and https://users.scala-lang.org/.

## 2. Build, tooling, Scala.js

- ⭐ **sbt — Getting Started guide** — https://www.scala-sbt.org/1.x/docs/Getting-Started.html
  Read at least *Build definition*, *Multi-project builds*, *Library dependencies*. **[M0]**
- ⭐ **Scala.js documentation** — https://www.scala-js.org/doc/ — *Tutorial*, *Project setup*,
  *Interoperability* (facade types, `js.Function`, `js.Promise` ↔ `Future`), *Cross-building*,
  and *Semantics* (how numbers differ from the JVM — relevant to determinism). **[M0, M2, M8]**
- **sbt-crossproject** — https://github.com/portable-scala/sbt-crossproject — `CrossType.Pure` is
  what `core` wants. **[M0]**
- **scala-js-dom** — https://scala-js.github.io/scala-js-dom/ — typed DOM API. **[M2]**
- **Scala.js + Vite tutorial** (optional nicer dev loop, see architecture A-5) —
  https://www.scala-js.org/doc/tutorial/scalajs-vite.html
- **Metals** (editor support) — https://scalameta.org/metals/
- **MDN: Canvas tutorial** — https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API/Tutorial
  — `scala-js-dom` mirrors this API one-to-one, so MDN *is* your rendering documentation.
  Also *Optimizing canvas*. **[M2, M4, M13]**
- **MDN: `requestAnimationFrame`** —
  https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame **[M2]**
- **MDN: KeyboardEvent.code**, **Gamepad API**, **Web Audio API** **[M2, S5]**

## 3. Testing

- ⭐ **MUnit** — https://scalameta.org/munit/ — incl. the *ScalaCheck integration* page. **[M0, M1]**
- ⭐ **ScalaCheck User Guide** —
  https://github.com/typelevel/scalacheck/blob/main/doc/UserGuide.md — generators, `forAll`,
  shrinking, conditional properties, `Gen` combinators. **[M1, M5, M7, M8, M12]**
- *Functional Programming in Scala*, chapter on property-based testing — builds a mini ScalaCheck;
  explains *why* it works.
- **"Choosing properties for property-based testing"** (Scott Wlaschin, F# but universal) —
  https://fsharpforfunandprofit.com/posts/property-based-testing-2/ — patterns such as
  "different paths, same destination", "test oracle" (your brute-force broad phase!), invariants,
  round-trips (level encode/decode). ⭐ for learning to *think* in properties.
- **"What Every Computer Scientist Should Know About Floating-Point Arithmetic"** (Goldberg) — the
  long classic; or the friendlier https://floating-point-gui.de/ (see *Comparison*). **[M1]**
- **"Comparing Floating Point Numbers, 2012 Edition"** (Bruce Dawson, Random ASCII blog). **[M1]**

## 4. Game loop and architecture

- ⭐ **"Fix Your Timestep!"** (Glenn Fiedler) — https://gafferongames.com/post/fix_your_timestep/
  — the accumulator + interpolation loop this project uses. **[M2]**
- ⭐ ***Game Programming Patterns*** (Robert Nystrom), free online —
  https://gameprogrammingpatterns.com/ — read *Game Loop* and *Update Method* **[M2]**, *State*
  **[M9, M13, M14]**, *Command* **[M9, S3, S4]**, *Observer* / *Event Queue* **[M11]**,
  *Spatial Partition* **[M7]**, *Component* (to understand why we are *not* doing ECS),
  *Data Locality* **[M6]**.
- **"Integration Basics"** (Glenn Fiedler) — https://gafferongames.com/post/integration_basics/
  — explicit vs semi-implicit Euler vs RK4. **[M3]**
- **"Functional core, imperative shell"** (Gary Bernhardt, talk *Boundaries*) — the idea behind the
  `core`/`web` split.

## 5. Game physics

### Core series (follow these closely)

- ⭐ **Randy Gaul — "How to Create a Custom 2D Physics Engine"** (Tuts+ Game Development, 4 parts):
  1. *The Basics and Impulse Resolution* **[M5]**
  2. *The Core Engine* (timestep, broad phase, layers) **[M2, M7]**
  3. *Friction, Scene and Jump Table* **[M6, M12]**
  4. *Oriented Rigid Bodies* **[S1]**
  Originally at gamedevelopment.tutsplus.com (now under code.tutsplus.com / Envato Tuts+); search by
  title. Companion code (C++): https://github.com/RandyGaul/ImpulseEngine — read it for structure,
  resist transliterating it.
- ⭐ **Erin Catto (Box2D) — GDC presentations** — https://box2d.org/publications/
  *Fast and Simple Physics using Sequential Impulses* (2006) **[M6]** — accumulated impulses and
  clamping, the single most useful talk for stable stacking; *Understanding Constraints* (2014)
  **[S2]**; *Continuous Collision* (2013) (background on tunneling).
  Also **Box2D-Lite** — https://github.com/erincatto/box2d-lite — a tiny, readable sequential
  impulse engine.
- **Box2D blog** — https://box2d.org/posts/ — see *Ghost Collisions* **[M8, M9]** (the seam
  snagging problem, explained by the person who named it) and *Solver2D* (comparison of solver
  types; advanced).
- **Allen Chou — Game Physics series** — https://allenchou.net/game-physics-series/ — broad phase,
  contact constraints, stability & warm starting, written for programmers. **[M6, M7, S1, S2]**

### Collision detection

- ⭐ **dyn4j — "SAT (Separating Axis Theorem)"** — https://dyn4j.org/2010/01/sat/ — the clearest
  SAT walkthrough, including MTV and circles. **[M12]**
  Also on the same blog: *Contact Points Using Clipping* **[S1]**, *GJK* / *EPA* (alternative to
  SAT; curiosity only).
- **Metanet Software — N tutorials A & B** (collision detection/response and grid broad phase, from
  the makers of *N*) — https://www.metanetsoftware.com/technique/tutorialA.html **[M5, M7, M12]**
- **MDN — "2D collision detection"** —
  https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection **[M5]** (gentle)
- ***Real-Time Collision Detection*** (Christer Ericson) — the reference book; ch. 4 (bounding
  volumes), ch. 5 (closest points — circle vs box/polygon), ch. 7 (spatial partitioning: grids,
  hashing). **[M5, M7, M12]**
- **"Optimized Spatial Hashing for Collision Detection of Deformable Objects"** (Teschner et al.) —
  the classic spatial-hash paper; short and readable. **[M7]**

### Dynamics and solvers (background / deeper)

- **Chris Hecker — Rigid Body Dynamics** (Game Developer Magazine series) —
  https://chrishecker.com/Rigid_Body_Dynamics — derivation of the impulse formula, with and without
  rotation. **[M5, S1]**
- ***Game Physics Engine Development*** (Ian Millington) — builds an impulse engine step by step
  (3D, but mass-aggregate chapters map well to 2D). **[M3–M6]**
- ***Physics for Game Developers*** / any first-year mechanics text for momentum, impulse,
  Coulomb friction, coefficient of restitution.
- **Matthias Müller — "Ten Minute Physics"** (YouTube/site) — https://matthias-research.github.io/pages/tenMinutePhysics/
  — position-based dynamics; a different approach worth knowing, great for ropes. **[S2]**
- **Thomas Jakobsen — "Advanced Character Physics"** (Hitman's Verlet ropes/cloth) — classic paper,
  alternative rope approach. **[S2]**

## 6. Platformer controls and game feel

- ⭐ **Maddy Thorson — "Celeste and TowerFall Physics"** —
  https://maddythorson.medium.com/celeste-and-towerfall-physics-d24bd2ae0fc5
  Describes the *opposite* approach from ours (integer, kinematic actors/solids, no physics
  engine). Read it to understand exactly what a physics-driven player gives up and must
  recover through the controller. **[M9]**
- ⭐ **Maddy Thorson — Celeste "forgiveness" thread** (coyote time, jump buffering, corner
  correction, etc.; originally a Twitter thread, widely mirrored — search "Celeste forgiveness
  mechanics Maddy Thorson"). **[M9]**
- ⭐ **Game Maker's Toolkit — "Why Does Celeste Feel So Good to Play?"** (YouTube) and the
  interactive essay **"Platformer Toolkit"** (free on itch.io) — lets you play with accel, decel,
  gravity scale, coyote time, buffers. Steal target numbers from here. **[M9]**
- **"Math for Game Programmers: Building a Better Jump"** (Kyle Pittman, GDC 2016, YouTube) —
  deriving gravity and jump velocity from desired height and duration — used in physics-design §2.
  **[M9]**
- **"The Guide to Implementing 2D Platformers"** (Rodrigo Monteiro, Higher-Order Fun) —
  http://higherorderfun.com/blog/2012/05/20/the-guide-to-implementing-2d-platformers/ — taxonomy of
  approaches; slopes, one-way platforms, moving platforms. **[M8, M9, M11, M12]**
- **Box2D-based character controllers** — search "Box2D platformer character iforce2d" — iforce2d's
  tutorials (https://www.iforce2d.net/b2dtut/) on *constant speed / velocity targeting*, *jumping*,
  *one-way walls*, and *ghost vertices* describe precisely the techniques a dynamic-body player
  needs, just with Box2D underneath. ⭐ for our chosen controller style. **[M9, M11]**
- **Steve Swink — *Game Feel*** (book) — vocabulary and measurement of feel. Optional.
- **Itay Keren — "Scroll Back: The Theory and Practice of Cameras in Side-Scrollers"** (GDC 2015
  talk + article) — every 2D camera technique catalogued. ⭐ **[M10]**
- **"Juice it or lose it"** (Jonasson & Purho, talk) and **Jan Willem Nijman — "The art of
  screenshake"** (talk). **[M14, S5]**

## 7. Levels and assets

- ⭐ **Tiled documentation** — https://doc.mapeditor.org/en/stable/ — *Working with Layers*,
  *Working with Objects*, *Custom Properties*. **[M8]**
- ⭐ **Tiled JSON Map Format** — https://doc.mapeditor.org/en/stable/reference/json-map-format/
  — the spec your decoder implements (note *global tile IDs* and their flip flags). **[M8]**
- **uPickle** — https://com-lihaoyi.github.io/upickle/ · **circe** — https://circe.github.io/circe/
  — the two JSON candidates (architecture A-3); both cross-build for Scala.js. **[M8]**
- **Free art:** Kenney — https://kenney.nl/assets (CC0; several platformer packs);
  https://opengameart.org/; itch.io free asset packs. **[M13]**
- **Free SFX:** sfxr/jsfxr — https://sfxr.me/ ; https://freesound.org/ **[S5]**

## 8. Scala game-dev context (for inspiration, not dependencies)

- **Indigo** — https://indigoengine.io/ — a purely functional Scala 3 engine on Scala.js. Reading
  its docs on its frame/update model is a useful contrast with our hand-rolled loop.
- **Tyrian** (Elm-style Scala.js UI) — possible basis for an editor UI **[S4]**.
- Li Haoyi's **"Hands-on Scala.js"** (older, still a good explanation of the platform) —
  https://www.lihaoyi.com/hands-on-scala-js/ — includes small canvas games.

## 9. Suggested reading order

| Before… | Read |
|---|---|
| M0 | Scala for Java/other devs · sbt Getting Started · Scala.js tutorial |
| M1 | Scala 3 Book: Domain Modeling, Methods · MUnit + ScalaCheck guides · Wlaschin on properties · floating-point-gui.de |
| M2 | Fix Your Timestep! · GPP: Game Loop · MDN canvas basics · Scala.js interop |
| M3 | Integration Basics · Gaul part 1 (first half) · opaque types & enums reference |
| M5 | Gaul part 1 · Hecker (impulse derivation) · MDN 2D collision |
| M6 | Gaul part 3 (friction) · Catto 2006 · Allen Chou on stability |
| M7 | Gaul part 2 · GPP: Spatial Partition · Ericson ch. 7 |
| M8 | Tiled docs + JSON format · JSON library docs · Box2D "Ghost Collisions" |
| M9 | Thorson (both) · GMTK Celeste + Platformer Toolkit · Building a Better Jump · iforce2d character tutorials · GPP: State |
| M10 | Itay Keren: Scroll Back |
| M11 | GPP: Observer, Event Queue · Higher-Order Fun guide (moving & one-way platforms) |
| M12 | dyn4j SAT · Metanet tutorial A · Ericson ch. 5 |
| M13–M14 | GPP: State · Juice it or lose it |
| S1 | Gaul part 4 · dyn4j clipping · Hecker parts 3–4 · Box2D-Lite source |
| S2 | Catto 2014 (constraints) · Müller · Jakobsen |
