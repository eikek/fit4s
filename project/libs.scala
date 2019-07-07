import sbt._

object libs {

  val `scala-version` = "2.13.0"

  // https://github.com/melrief/pureconfig
  // MPL 2.0
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.7.2"

  // https://github.com/typelevel/cats
  // MIT http://opensource.org/licenses/mit-license.php
  val `cats-core` = "org.typelevel" %% "cats-core" % "0.9.0"

  // https://github.com/functional-streams-for-scala/fs2
  // MIT
  val `fs2-core` = "co.fs2" %% "fs2-core" % "1.1.0-M1"
  val `fs2-io` = "co.fs2" %% "fs2-io" % "1.1.0-M1"

  // https://github.com/monix/minitest
  // Apache 2.0
  val minitest = "io.monix" %% "minitest" % "2.5.0"
  val `minitest-laws` = "io.monix" %% "minitest-laws" % "2.5.0"

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  // https://github.com/scodec/scodec
  // 3-clause BSD
  val `scodec-core` = "org.scodec" %% "scodec-core" % "1.11.4"

  // https://github.com/Log4s/log4s
  // ASL 2.0
  val log4s = "org.log4s" %% "log4s" % "1.8.2"

  // http://logback.qos.ch/
  // EPL1.0 or LGPL 2.1
  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.3"
}
