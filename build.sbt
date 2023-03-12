import Dependencies.V
import com.github.sbt.git.SbtGit.GitKeys._

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := V.scala2,
  scalacOptions ++=
    Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:higherKinds"
    ) ++
      (if (scalaBinaryVersion.value.startsWith("2.13"))
        List("-Werror", "-Wdead-code", "-Wunused", "-Wvalue-discard")
      else if (scalaBinaryVersion.value.startsWith("3"))
        List(
          "-explain",
          "-explain-types",
          "-indent",
          "-print-lines",
          "-Ykind-projector",
          "-Xmigration",
          "-Xfatal-warnings"
        )
      else
        Nil),
  crossScalaVersions := Seq(V.scala2, V.scala3),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq(),
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek/emil")),
  versionScheme := Some("early-semver")
) ++ publishSettings

lazy val publishSettings = Seq(
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  Test / publishArtifact := false
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val testSettings = Seq(
  libraryDependencies ++= (Dependencies.munit ++ Dependencies.logback).map(_ % Test),
  testFrameworks += TestFrameworks.MUnit
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    gitHeadCommit,
    gitHeadCommitDate,
    gitUncommittedChanges,
    gitDescribedVersion
  ),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime,
  buildInfoPackage := "emil"
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
)


lazy val core = project.in(file("modules/core"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-core",
    description := "Library for reading and writing FIT format",
    libraryDependencies ++= Dependencies.scodecCore(
      if (scalaVersion.value.startsWith("2.")) V.scodec1 else V.scodec2
    )
  )

lazy val root = project.in(file(".")).
  settings(sharedSettings).
  settings(
    name := "root"
  ).
  aggregate(core)
