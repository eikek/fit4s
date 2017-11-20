import sbt._

object libs {

  val `scala-version` = "2.12.4"

  // https://github.com/melrief/pureconfig
  // MPL 2.0
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.7.2"

  // https://github.com/typelevel/cats
  // MIT http://opensource.org/licenses/mit-license.php
  val `cats-core` = "org.typelevel" %% "cats-core" % "0.9.0"

  // https://github.com/functional-streams-for-scala/fs2
  // MIT
  val `fs2-core` = "co.fs2" %% "fs2-core" % "0.9.6"
  val `fs2-io` = "co.fs2" %% "fs2-io" % "0.9.6"

  // https://github.com/scalatest/scalatest
  // ASL 2.0
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.4"

  // https://github.com/rickynils/scalacheck
  // unmodified 3-clause BSD
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5"

  // https://github.com/scodec/scodec
  // 3-clause BSD
  val `scodec-core` = "org.scodec" %% "scodec-core" % "1.10.3"

  // https://github.com/Log4s/log4s
  // ASL 2.0
  val log4s = "org.log4s" %% "log4s" % "1.4.0"

  // http://logback.qos.ch/
  // EPL1.0 or LGPL 2.1
  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.3"

  // https://java.net/projects/javamail/pages/Home
  // CDDL 1.0, GPL 2.0
  val `javax-mail-api` = "javax.mail" % "javax.mail-api" % "1.5.6"
  val `javax-mail` = "com.sun.mail" % "javax.mail" % "1.5.6"

  // http://dnsjava.org/
  // BSD
  val dnsjava = "dnsjava" % "dnsjava" % "2.1.8" intransitive()
}
