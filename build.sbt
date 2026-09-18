ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "impulse"

// Pure Scala, compiled for both the JVM (fast headless tests) and JS (the game).
// `core.jvm` and `core.js` are the two real sbt projects this defines: coreJVM / coreJS.
lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "impulse-core"
  )

// Browser shell: loop, rendering, input, assets. Scala.js only.
lazy val web = project
  .in(file("web"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(core.js)
  .settings(
    name := "impulse-web",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1"
  )

lazy val root = project
  .in(file("."))
  .aggregate(core.jvm, core.js, web)
  .settings(
    name           := "impulse",
    publish / skip := true
  )
