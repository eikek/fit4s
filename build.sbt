import Dependencies.V
import com.github.sbt.git.SbtGit.GitKeys._

addCommandAlias("ci", "; lint; +test; readme/updateReadme; +publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")
addCommandAlias("make-package", "; cli/Universal/packageBin")

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
  addCompilerPlugin(Dependencies.betterMonadicFor),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq(),
  licenses := Seq(
    "GPL-3.0-or-later" -> url("https://spdx.org/licenses/GPL-3.0-or-later")
  ),
  homepage := Some(url("https://github.com/eikek/fit4s")),
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
  libraryDependencies ++= (Dependencies.munit ++
    Dependencies.fs2 ++
    Dependencies.fs2Io ++
    Dependencies.circe).map(_ % Test),
  testFrameworks += TestFrameworks.MUnit
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  ThisBuild / scalafixDependencies ++= Dependencies.organizeImports
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
  buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.BuildTime),
  buildInfoPackage := "fit4s"
)

lazy val core = project
  .enablePlugins(BuildInfoPlugin)
  .in(file("modules/core"))
  .enablePlugins(ProfileGeneratorPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(buildInfoSettings)
  .settings(
    name := "fit4s-core",
    description := "Library for reading and writing FIT format",
    libraryDependencies ++= Dependencies.scodecCore(
      if (scalaVersion.value.startsWith("2.")) V.scodec1 else V.scodec2
    ),
    profileFile := (LocalRootProject / baseDirectory).value / "project" / "profile" / "profile_21.105.00.xlsx",
    Compile / sourceGenerators += generateProfile.taskValue
  )

lazy val geocode = project
  .in(file("modules/geocode"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-geocode",
    description := "Uses webservices to reverse lookup coordinates",
    libraryDependencies ++=
      Dependencies.http4sClient ++
        Dependencies.fs2Core ++
        Dependencies.circe ++
        Dependencies.http4sCirce
  )
  .dependsOn(core)

lazy val strava = project
  .in(file("modules/strava"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-strava",
    description := "Strava client supporting fit4s",
    libraryDependencies ++=
      Dependencies.http4sClient ++
        Dependencies.http4sServer ++
        Dependencies.http4sCirce ++
        Dependencies.circe ++
        Dependencies.circeGenericExtra ++
        Dependencies.fs2 ++
        Dependencies.scalaCsv ++
        Dependencies.scribe
  )
  .dependsOn(core)

lazy val activities = project
  .in(file("modules/activities"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-activities",
    description := "A small database backed activity log",
    libraryDependencies ++= Dependencies.fs2 ++
      Dependencies.catsParse ++
      Dependencies.h2 ++
      Dependencies.doobie ++
      Dependencies.flyway ++
      Dependencies.scribe ++
      Dependencies.circe ++
      Dependencies.circeGenericExtra
  )
  .dependsOn(
    core % "compile->compile;test->test",
    geocode % "compile->compile;test->test",
    strava % "compile->compile;test->test"
  )

lazy val cli = project
  .in(file("modules/cli"))
  .enablePlugins(JavaAppPackaging, ClasspathJarPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-cli",
    description := "A command line interface to look at your fit files",
    libraryDependencies ++=
      Dependencies.fs2Core ++
        Dependencies.decline ++
        Dependencies.circeCore ++
        Dependencies.ciris ++
        Dependencies.scribe
  )
  .dependsOn(core % "compile->compile;test->test", activities)

lazy val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-root"
  )
  .aggregate(core, geocode, strava, activities, cli)
