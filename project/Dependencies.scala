import sbt._

object Dependencies {

  object V {
    val scala2 = "2.13.10"
    val scala3 = "3.1.2"

    val catsParse = "0.3.9"
    val circe = "0.14.5"
    val ciris = "3.1.0"
    val decline = "2.4.1"
    val doobie = "1.0.0-RC2"
    val http4sClient = "0.23.18"
    val flyway = "9.16.1"
    val h2 = "2.1.214"
    val munit = "0.7.29"
    val munitCatsEffect = "1.0.7"
    val logback = "1.4.5"
    val scodec1 = "1.11.10"
    val scodec2 = "2.2.0"
    val catsCore = "2.9.0"
    val fs2 = "3.6.1"
    val scalaCheck = "1.17.0"
    val scalaCsv = "1.3.10"
    val organizeImports = "0.6.0"
    val scribeSlf4j = "3.11.1"
    val scribe = "3.11.1"
  }

  val scribe = Seq(
    "com.outr" %% "scribe" % V.scribe,
    "com.outr" %% "scribe-slf4j" % V.scribeSlf4j,
    "com.outr" %% "scribe-cats" % V.scribe
  )

  val http4sClient = Seq(
    "org.http4s" %% "http4s-ember-client" % V.http4sClient
  )
  val http4sCirce = Seq(
    "org.http4s" %% "http4s-circe" % V.http4sClient
  )

  val scalaCsv = Seq(
    "com.github.tototoshi" %% "scala-csv" % V.scalaCsv
  )

  val ciris = Seq(
    "is.cir" %% "ciris" % V.ciris
  )

  val catsParse = Seq(
    "org.typelevel" %% "cats-parse" % V.catsParse
  )

  val flyway = Seq(
    "org.flywaydb" % "flyway-core" % V.flyway
//    "org.flywaydb" % "flyway-mysql" % FlywayVersion
  ).map(
    _.excludeAll(
      ExclusionRule("com.fasterxml.jackson.core"),
      ExclusionRule("com.fasterxml.jackson.dataformat")
    )
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

  val circeCore = Seq(
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-generic" % V.circe
  )
  val circeParser = Seq(
    "io.circe" %% "circe-parser" % V.circe
  )
  val circe = circeCore ++ circeParser

  val organizeImports = Seq(
    "com.github.liancheng" %% "organize-imports" % V.organizeImports
  )

  // https://github.com/typelevel/cats
  // MIT http://opensource.org/licenses/mit-license.php
  val catsCore = Seq("org.typelevel" %% "cats-core" % V.catsCore)

  // https://github.com/functional-streams-for-scala/fs2
  // MIT
  val fs2Core = Seq("co.fs2" %% "fs2-core" % V.fs2)
  val fs2Io = Seq("co.fs2" %% "fs2-io" % V.fs2)
  val fs2 = fs2Core ++ fs2Io

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  val scalacheck = Seq("org.scalacheck" %% "scalacheck" % V.scalaCheck)

  // https://github.com/scodec/scodec
  // 3-clause BSD
  def scodecCore(version: String) = Seq("org.scodec" %% "scodec-core" % version)

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munit,
    "org.scalameta" %% "munit-scalacheck" % V.munit,
    "org.typelevel" %% "munit-cats-effect-3" % V.munitCatsEffect
  )

  val logback = Seq(
    "ch.qos.logback" % "logback-classic" % V.logback
  )
}
