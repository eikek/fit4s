import Dependencies.V
import com.github.sbt.git.SbtGit.GitKeys._
import org.scalajs.linker.interface.ModuleSplitStyle

addCommandAlias("ci", "; lint; +test; +publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")
addCommandAlias("make-package", "; cli/Universal/packageBin")
addCommandAlias("make-stage", "; cli/Universal/stage")

// remove dirt suffix, bc the build apparently touches the xslx file
// this causes a timestamp suffix and then it's just harder to find
// the resulting file from the github action yaml
def versionFmt(out: sbtdynver.GitDescribeOutput): String =
  if (out.ref.isTag && out.commitSuffix.isEmpty) out.ref.dropPrefix
  else out.ref.dropPrefix + out.commitSuffix.mkString("-", "-", "")

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer.timestamp(d)}"

inThisBuild(
  List(
    dynverSeparator := "-",
    dynverSonatypeSnapshots := true,
    version := dynverGitDescribeOutput.value
      .mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value)),
    dynver := {
      val d = new java.util.Date
      sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
    }
  )
)

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := V.scala3,
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
           "-indent",
           "-print-lines",
           "-Ykind-projector",
           "-Xmigration",
           "-Xfatal-warnings"
         )
       else
         Nil),
  libraryDependencies ++=
    (if (scalaBinaryVersion.value.startsWith("3")) Seq.empty
     else Seq(compilerPlugin(Dependencies.betterMonadicFor))),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq(),
  Compile / packageDoc / publishArtifact := false, // deactivate until typelevel/fs2#3293 is resolved
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
    Dependencies.fs2Jvm ++
    Dependencies.borer).map(_ % Test),
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

val writeVersion = taskKey[Unit]("Write version into a file for CI to pick up")

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/core"))
  .enablePlugins(BuildInfoPlugin, ProfileGeneratorPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(buildInfoSettings)
  .settings(
    name := "fit4s-core",
    description := "Library for reading and writing FIT format",
    libraryDependencies ++= Dependencies.scodecCore.value,
    profileFile := (LocalRootProject / baseDirectory).value / "project" / "profile" / "profile_21.105.00.xlsx",
    Compile / sourceGenerators += generateProfile.taskValue
  )

lazy val coreJs = core.js
lazy val coreJvm = core.jvm

lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/common"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-common",
    description := "Shared code for data types and utilities",
    libraryDependencies ++=
      Dependencies.catsEffect.value ++
        Dependencies.borerJs.value
  )
  .dependsOn(
    core % "compile->compile;test->test"
  )

lazy val commonJs = common.js
lazy val commonJvm = common.jvm

lazy val tcx = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/tcx"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-tcx",
    description := "Read tcx format",
    libraryDependencies ++= Dependencies.scalaXML.value
  )
  .dependsOn(
    core % "compile->compile;test->test"
  )

lazy val tcxJs = tcx.js
lazy val tcxJvm = tcx.jvm

lazy val http4sBorer = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/http4s-borer"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-http4s-borer",
    description := "Use borer codecs with http4s",
    libraryDependencies ++=
      Dependencies.borerJs.value ++
        Dependencies.http4sCore.value
  )
lazy val http4sBorerJs = http4sBorer.js
lazy val http4sBorerJvm = http4sBorer.jvm

lazy val geocode = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/geocode"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-geocode",
    description := "Uses webservices to reverse lookup coordinates",
    libraryDependencies ++=
      Dependencies.http4sEmberClient.value ++
        Dependencies.fs2Core.value ++
        Dependencies.borerJs.value ++
        Dependencies.scribeJs.value
  )
  .dependsOn(core, common, http4sBorer)

lazy val geocodeJvm = geocode.jvm
lazy val geocodeJs = geocode.js

lazy val strava = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/strava"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-strava",
    description := "Strava client supporting fit4s",
    libraryDependencies ++=
      Dependencies.http4sEmberClient.value ++
        Dependencies.borerJs.value ++
        Dependencies.fs2.value
  )
  .jvmSettings(
    libraryDependencies ++=
      Dependencies.http4sServer ++
        Dependencies.scalaCsv ++
        Dependencies.scribe
  )
  .dependsOn(core, common, http4sBorer)

lazy val stravaJvm = strava.jvm
lazy val stravaJs = strava.js

lazy val activities = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/activities"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-activities",
    description := "A small database backed activity log",
    libraryDependencies ++= Dependencies.catsParse.value ++
      Dependencies.borerJs.value
  )
  .dependsOn(
    core % "compile->compile;test->test",
    common % "compile->compile;test->test",
    geocode % "compile->compile;test->test",
    strava % "compile->compile;test->test",
    tcx % "compile->compile;test->test"
  )
  .jvmSettings(
    libraryDependencies ++= Dependencies.fs2Jvm ++
      Dependencies.h2 ++
      Dependencies.postgres ++
      Dependencies.doobie ++
      Dependencies.flyway ++
      Dependencies.scribe
  )
lazy val activitiesJvm = activities.jvm
lazy val activitiesJs = activities.js

val webclientTimezones = Set(
  "Europe/Berlin",
  "Europe/London"
)

lazy val webview =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .withoutSuffixFor(JVMPlatform)
    .in(file("modules/webview"))
    .enablePlugins(ScalaJSPlugin)
    .settings(sharedSettings)
    .settings(testSettings)
    .settings(scalafixSettings)
    .settings(
      name := "fit4s-webview",
      description := "View activities in a browser",
      scalaVersion := V.scala3,
      libraryDependencies ++=
        Dependencies.fs2Core.value ++
          Dependencies.borerJs.value
    )
    .jvmSettings(
      libraryDependencies ++=
        Dependencies.http4sServer ++
          Dependencies.fs2Jvm
    )
    .jsSettings(
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(
            ModuleSplitStyle.SmallModulesFor(List("fit4s.webview.client"))
          )
      },
      zonesFilter := { (z: String) =>
        webclientTimezones.contains(z)
      },
      Compile / sourceGenerators += Def.task {
        val log = streams.value.log
        val buildForProd =
          sys.env.get("FIT4S_BUILD_PROD").exists(_.equalsIgnoreCase("true"))
        val uri = if (buildForProd) "/api" else "http://localhost:8181/api"
        log.info(s"Using api uri: $uri")

        val srcDev =
          s"""package fit4s.webview.client
             |// This file is generated.
             |import org.http4s.implicits.*
             |
             |private class BaseUrlImpl extends BaseUrl {
             |  val get = uri"$uri"
             |}
            """.stripMargin

        val target = (Compile / sourceManaged).value / "scala" / "BaseUrlImpl.scala"
        IO.write(target, srcDev)
        Seq(target)
      },
      libraryDependencies ++=
        Dependencies.scalaJsDom.value ++
          Dependencies.calico.value ++
          Dependencies.fs2Io.value ++
          Dependencies.scalaJsJavaTime.value ++
          Dependencies.http4sJsClient.value ++
          Dependencies.scribeJs.value
    )
    .dependsOn(
      activities % "compile->compile;test->test",
      core % "compile->compile;test->test",
      common % "compile->compile;test->test"
    )

lazy val webviewJvm = webview.jvm
lazy val webviewJs = webview.js.enablePlugins(TzdbPlugin)

lazy val cli = project
  .in(file("modules/cli"))
  .enablePlugins(JavaAppPackaging, ClasspathJarPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "fit4s-cli",
    description := "A command line interface to look at your fit files",
    libraryDependencies ++=
      Dependencies.fs2Jvm ++
        Dependencies.decline ++
        Dependencies.borer ++
        Dependencies.ciris ++
        Dependencies.scribe,
    writeVersion := {
      val out = (LocalRootProject / target).value / "version.txt"
      val versionStr = version.value
      IO.write(out, versionStr)
    },
    Universal / mappings := {
      val allMappings = (Universal / mappings).value
      allMappings.filter {
        // scalajs artifacts are not needed at runtime
        case (file, name) => !name.contains("_sjs1_")
      }
    }
  )
  .dependsOn(
    coreJvm % "compile->compile;test->test",
    commonJvm % "compile->compile;test->test",
    activitiesJvm % "compile->compile;test->test",
    webviewJvm % "compile->compile;test->test"
  )

lazy val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "fit4s-root"
  )
  .aggregate(
    coreJvm,
    coreJs,
    commonJvm,
    commonJs,
    tcxJvm,
    tcxJs,
    http4sBorerJs,
    http4sBorerJvm,
    geocodeJvm,
    geocodeJs,
    stravaJvm,
    stravaJs,
    activitiesJvm,
    activitiesJs,
    webviewJs,
    webview.jvm,
    cli
  )
