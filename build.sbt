ThisBuild / scalaVersion := "3.9.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "impulse"

// Pure Scala, compiled for both the JVM (fast headless tests) and JS (the game).
// `core.jvm` and `core.js` are the two real sbt projects this defines: coreJVM / coreJS.
lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "impulse-core",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit"            % "1.3.6" % Test,
      "org.scalameta" %%% "munit-scalacheck" % "1.3.1" % Test
    )
  )

// Browser shell: loop, rendering, input, assets. Scala.js only.
lazy val web = project
  .in(file("web"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(core.js)
  .settings(
    name := "impulse-web",
    scalaJSUseMainModuleInitializer := true,
    // Fixed output paths so index.html does not have to know the Scala version.
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory := target.value / "js" / "dev",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory := target.value / "js" / "prod",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1"
  )

lazy val root = project
  .in(file("."))
  .aggregate(core.jvm, core.js, web)
  .settings(
    name           := "impulse",
    publish / skip := true
  )
