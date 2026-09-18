# Impulse

A 2D physics platformer with a hand-written physics engine, built in Scala 3 and Scala.js.
This is a learning project; see `docs/` for the design, milestones and reading list.

## Layout

| Project | What | Platform |
|---|---|---|
| `core` | math, physics, world, game logic — pure Scala | JVM (`coreJVM`) and JS (`coreJS`) |
| `web`  | browser shell: loop, rendering, input, assets | JS only, depends on `coreJS` |

## Prerequisites

- JDK 17 or 21
- sbt 1.x (the exact version is pinned in `project/build.properties`)
- Node.js (runs the JS test suite)
- Any static file server, e.g. Python's `http.server`

## Build and test

Start an sbt shell once and keep it open; every command below runs inside it.

```
sbt
```

| Command | Effect |
|---|---|
| `compile` | compile all projects |
| `coreJVM/test` | run the core tests on the JVM (fast, use this day to day) |
| `coreJS/test` | run the same tests under Node.js |
| `test` | both of the above |
| `~coreJVM/test` | re-run the JVM tests whenever a source file changes |

## Run in the browser

```
sbt web/fastLinkJS
```

writes `web/target/js/dev/main.js`. Serve the `web/` directory with any static server and open
it:

```
cd web
python -m http.server 8000
```

then browse to <http://localhost:8000/>. Use `~web/fastLinkJS` in the sbt shell to relink on every
change; reload the page to pick it up.

`web/fullLinkJS` produces the optimised production build in `web/target/js/prod/`.
