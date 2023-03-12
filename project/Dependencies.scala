import sbt._

object Dependencies {

  object V {
    val scala2 = "2.13.10"
    val scala3 = "3.1.2"

    val munit = "0.7.29"
    val logback = "1.4.5"
    val scodec1 = "1.11.10"
    val scodec2 = "2.2.0"
    val catsCore = "2.9.0"
    val fs2 = "3.6.1"
    val scalaCheck = "1.17.0"
  }

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
    "org.scalameta" %% "munit-scalacheck" % V.munit
  )

  val logback = Seq(
    "ch.qos.logback" % "logback-classic" % V.logback
  )
}
