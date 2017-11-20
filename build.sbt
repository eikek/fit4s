import libs._

lazy val sharedSettings = Seq(
  scalaVersion := `scala-version`,
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains)),
  scalacOptions in (Test) := (scalacOptions in (Compile, console)).value
)

lazy val coreDeps = Seq(log4s, `scodec-core`)
lazy val testDeps = Seq(scalatest, scalacheck, `logback-classic`, `fs2-core`, `fs2-io`).map(_ % "test")

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
