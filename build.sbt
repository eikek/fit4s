import libs._

val scalacOpts: Seq[String] = Seq(
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:higherKinds",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import"
)

lazy val sharedSettings = Seq(
  scalaVersion := `scala-version`,
  scalacOptions := {
    if (scalaBinaryVersion.value.startsWith("2.13")) {
      scalacOpts.filter(o => o != "-Yno-adapted-args" && o != "-Ywarn-unused-import")
    } else {
      scalacOpts
    }
  },
  scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains)),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  crossScalaVersions := Seq("2.12.8", `scala-version`),
  testFrameworks += new TestFramework("minitest.runner.Framework")
)

lazy val coreDeps = Seq(log4s, `scodec-core`)
lazy val testDeps = Seq(minitest, scalacheck, `logback-classic`, `fs2-core`, `fs2-io`).map(_ % "test")

lazy val core = project.in(file("modules/core")).
  settings(sharedSettings).
  settings(
    name := "fit4s-core",
    description := "Library for reading and writing FIT format",
    libraryDependencies ++= coreDeps ++ testDeps
  )


lazy val root = project.in(file(".")).
  settings(sharedSettings).
  settings(
    name := "root"
  ).
  aggregate(core)
