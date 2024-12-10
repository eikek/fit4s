import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Keys.scalaVersion

object Dependencies {

  object V {
    val scala2 = "2.13.10"
    val scala3 = "3.5.2"

    val borer = "1.15.0"
    val catsEffect = "3.5.5"
    val catsParse = "1.0.0"
    val calico = "0.2.3"
    val ciris = "3.6.0"
    val decline = "2.4.1"
    val doobie = "1.0.0-RC6"
    val http4s = "0.23.29"
    val http4sDom = "0.2.11"
    val flyway = "10.21.0"
    val h2 = "2.3.232"
    val munit = "1.0.0"
    val munitCatsEffect = "2.0.0"
    val scodec1 = "1.11.10"
    val scodec2 = "2.3.2"
    val catsCore = "2.9.0"
    val fs2 = "3.11.0"
    val scalaCheck = "1.17.0"
    val scalaCsv = "1.4.1"
    val postgres = "42.7.4"
    val scalaJsDom = "2.8.0"
    val scalaJsTime = "2.6.0"
    val scalaXML = "2.3.0"
    val scribeSlf4j = "3.15.1"
    val scribe = "3.15.1"
  }

  val scalaXML = Def.setting(
    Seq(
      "org.scala-lang.modules" %%% "scala-xml" % V.scalaXML
    )
  )

  val postgres = Seq(
    "org.postgresql" % "postgresql" % V.postgres
  )

  val scalaJsJavaTime = Def.setting(
    Seq(
      "io.github.cquiroz" %%% "scala-java-time" % V.scalaJsTime
    )
  )

  val http4sJsClient = Def.setting(
    Seq(
      "org.http4s" %%% "http4s-dom" % V.http4sDom,
      "org.http4s" %%% "http4s-client" % V.http4s
    )
  )

  val calico = Def.setting(
    Seq(
      "com.armanbilge" %%% "calico" % V.calico,
      "com.armanbilge" %%% "calico-router" % V.calico
    )
  )

  val scalaJsDom = Def.setting(
    Seq(
      "org.scala-js" %%% "scalajs-dom" % V.scalaJsDom
    )
  )

  val betterMonadicFor =
    "com.olegpy" %% "better-monadic-for" % "0.3.1"

  val scribe = Seq(
    "com.outr" %% "scribe" % V.scribe,
    "com.outr" %% "scribe-slf4j" % V.scribeSlf4j,
    "com.outr" %% "scribe-cats" % V.scribe
  )

  val scribeJs = Def.setting(
    Seq(
      "com.outr" %%% "scribe" % V.scribe,
      "com.outr" %%% "scribe-cats" % V.scribe
    )
  )

  val http4sCore = Def.setting(Seq("org.http4s" %%% "http4s-core" % V.http4s))

  val http4sEmberClient = Def.setting(
    Seq(
      "org.http4s" %%% "http4s-ember-client" % V.http4s
    )
  )
  val http4sServer = Seq(
    "org.http4s" %% "http4s-ember-server" % V.http4s,
    "org.http4s" %% "http4s-dsl" % V.http4s
  )

  val scalaCsv = Seq(
    "com.github.tototoshi" %% "scala-csv" % V.scalaCsv
  )

  val ciris = Seq(
    "is.cir" %% "ciris" % V.ciris
  )

  val catsParse = Def.setting(
    Seq(
      "org.typelevel" %%% "cats-parse" % V.catsParse
    )
  )

  val flyway = Seq(
    "org.flywaydb" % "flyway-core" % V.flyway,
    "org.flywaydb" % "flyway-database-postgresql" % V.flyway
//    "org.flywaydb" % "flyway-mysql" % FlywayVersion
  )

  val h2 = Seq(
    "com.h2database" % "h2" % V.h2
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core" % V.doobie
    // "org.tpolecat" %% "doobie-hikari" % DoobieVersion
  )

  val decline = Seq(
    "com.monovore" %% "decline" % V.decline,
    "com.monovore" %% "decline-effect" % V.decline
  )

  // val circeCore = Seq(
  //   "io.circe" %% "circe-core" % V.circe,
  //   "io.circe" %% "circe-generic" % V.circe
  // )
  // val circeGenericExtra = Def.setting(
  //   Seq(
  //     "io.circe" %%% "circe-generic-extras" % V.circeGenericExtra
  //   )
  // )
  // val circeParser = Seq(
  //   "io.circe" %% "circe-parser" % V.circe
  // )
  // val circeAll = circeCore ++ circeParser

  // val circeAllJs = Def.setting(
  //   Seq(
  //     "io.circe" %%% "circe-core" % V.circe,
  //     "io.circe" %%% "circe-generic" % V.circe,
  //     "io.circe" %%% "circe-parser" % V.circe
  //   )
  // )

  val borer = Seq(
    "io.bullet" %% "borer-core" % V.borer,
    "io.bullet" %% "borer-derivation" % V.borer,
    "io.bullet" %% "borer-compat-cats" % V.borer,
    "io.bullet" %% "borer-compat-scodec" % V.borer
  )

  val borerJs = Def.setting(
    Seq(
      "io.bullet" %%% "borer-core" % V.borer,
      "io.bullet" %%% "borer-derivation" % V.borer,
      "io.bullet" %%% "borer-compat-cats" % V.borer,
      "io.bullet" %%% "borer-compat-scodec" % V.borer
    )
  )

  // https://github.com/typelevel/cats
  // MIT http://opensource.org/licenses/mit-license.php
  val catsCore = Seq("org.typelevel" %% "cats-core" % V.catsCore)

  val catsEffect = Def.setting(
    Seq("org.typelevel" %%% "cats-effect" % V.catsEffect)
  )

  // https://github.com/functional-streams-for-scala/fs2
  // MIT
  val fs2Core = Def.setting(Seq("co.fs2" %%% "fs2-core" % V.fs2))
  val fs2Io = Def.setting(Seq("co.fs2" %%% "fs2-io" % V.fs2))
  val fs2 = Def.setting(fs2Core.value ++ fs2Io.value)
  val fs2Jvm = Seq("co.fs2" %% "fs2-core" % V.fs2, "co.fs2" %% "fs2-io" % V.fs2)

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  val scalacheck = Seq("org.scalacheck" %% "scalacheck" % V.scalaCheck)

  // https://github.com/scodec/scodec
  // 3-clause BSD
  val scodecCore = Def.setting {
    val v = if (scalaVersion.value.startsWith("2.")) V.scodec1 else V.scodec2
    Seq("org.scodec" %%% "scodec-core" % v)
  }

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munit,
    "org.scalameta" %% "munit-scalacheck" % V.munit,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect
  )
}
