package fit4s.activities.internal

import java.time._
import java.time.temporal.ChronoUnit

import cats.data.NonEmptyList
import cats.parse.{Numbers, Parser}
import fs2.io.file.Path

import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.internal.BasicParser.ws
import fit4s.data.{DeviceProduct, Distance}
import fit4s.profile.types.{Sport, SubSport}

abstract class BasicParser(zoneId: ZoneId, currentTime: Instant) {
  val instant: Parser[Instant] =
    BasicParser.epochSeconds.backtrack | BasicParser.timestamp(zoneId)

  val lastWeeks: Parser[Instant] =
    BasicParser.lastNDays(currentTime, 7, NonEmptyList.of("week", "weeks", "w"))

  val lastDays: Parser[Instant] =
    BasicParser.lastNDays(currentTime, 1, NonEmptyList.of("day", "days", "d"))

  val dateTime: Parser[Instant] =
    lastDays.backtrack.orElse(lastWeeks.backtrack.orElse(instant))

  val tagName: Parser[TagName] =
    BasicParser.qstring.map(TagName.unsafeFromString)

  def listOf[A, B](elementParser: Parser[B])(
      plus: NonEmptyList[B] => A,
      comma: NonEmptyList[B] => A
  ): Parser[A] = {
    val seps =
      BasicParser.ws0.soft.with1 *> Parser.oneOf(
        List(BasicParser.commaSep.as(','), BasicParser.plusSep.as('+'))
      ) <* BasicParser.ws0

    (elementParser ~ (seps.soft ~ elementParser).rep0).flatMap { case (head, tail) =>
      tail.map(_._1).distinct match {
        case List('+') =>
          Parser.pure(plus(NonEmptyList(head, tail.map(_._2))))
        case List(',') =>
          Parser.pure(comma(NonEmptyList(head, tail.map(_._2))))
        case Nil =>
          Parser.pure(comma(NonEmptyList.of(head)))
        case other =>
          Parser.failWith[A](
            s"Mixed separators: $other. Either use a comma or plus separated list"
          )
      }
    }
  }
  val path: Parser[Path] =
    BasicParser.qstring.map(Path.apply)

  val fileId: Parser[String] = {
    val valid = (('1' to '9') ++ ('A' to 'Z') ++ ('a' to 'z'))
      .filterNot(c => Set('O', 'I', 'l').contains(c))
      .toSet

    Parser.charsWhile(valid.contains)
  }

  val sport: Parser[Sport] =
    Parser
      .stringIn(Sport.all.map(_.typeName))
      .flatMap(tn =>
        Sport.byTypeName(tn) match {
          case Some(s) => Parser.pure(s)
          case None    => Parser.failWith(s"Unknown sport: $tn")
        }
      )

  val subSport: Parser[SubSport] =
    Parser
      .stringIn(SubSport.all.map(_.typeName))
      .flatMap(tn =>
        SubSport.byTypeName(tn) match {
          case Some(s) => Parser.pure(s)
          case None    => Parser.failWith(s"Unknown sub-sport: $tn")
        }
      )

  val distance: Parser[Distance] = {
    val units = Map(
      "k" -> 1000,
      "km" -> 1000,
      "m" -> 1
    )
    (BasicParser.digits ~ (ws.? *> Parser.stringIn(units.keys))).map { case (n, u) =>
      val factor = units(u)
      val meter = n * factor
      Distance.meter(meter)
    }
  }

  val duration: Parser[Duration] = {
    val units = Map(
      "min" -> 1,
      "m" -> 1,
      "h" -> 60,
      "hour" -> 60,
      "hours" -> 60
    )
    (BasicParser.digits ~ (ws.? *> Parser.stringIn(units.keys))).map { case (n, u) =>
      val factor = units(u)
      val minutes = n * factor
      Duration.ofMinutes(minutes)
    }
  }

  val device: Parser[DeviceProduct] =
    Parser
      .stringIn(DeviceProduct.all.map(_.name.toLowerCase))
      .map(DeviceProduct.unsafeFromString)

  val activityId: Parser[ActivityId] =
    Numbers.nonNegativeIntString.map(_.toLong).map(ActivityId.apply)
}

object BasicParser {
  val ws = Parser.charsWhile(_.isWhitespace).void
  val ws0 = ws.rep0.void

  val digit2 = Numbers.digit.repAs[String](1, 2).map(_.toInt)
  val digit4 = Numbers.digit.repAs[String](4, 4).map(_.toInt)
  val digits = Numbers.digits.map(_.toInt)

  val month: Parser[Int] =
    digit2
      .filter(n => 1 <= n && n <= 12)
      .withContext("Invalid month digits")

  val day: Parser[Int] =
    digit2
      .filter(n => 1 <= n && n <= 31)
      .withContext("Invalid day digits")

  val hour: Parser[Int] =
    digit2
      .filter(n => 0 <= n && n <= 23)
      .withContext("Invalid hour digits")

  val minsec: Parser[Int] =
    digit2
      .filter(n => 0 <= n && n <= 59)
      .withContext("Invalid minute or second digits")

  val commaSep: Parser[Unit] = ws.?.with1 *> Parser.char(',') <* ws.?

  val plusSep: Parser[Unit] = ws.?.with1 *> Parser.char('+') <* ws.?

  val parenAnd: Parser[Unit] =
    Parser.stringIn(List("(&", "(and")).void <* ws0

  val parenClose: Parser[Unit] =
    ws0.soft.with1 *> Parser.char(')')

  val parenOr: Parser[Unit] =
    Parser.stringIn(List("(|", "(or")).void <* ws0

  val basicString =
    Parser.charsWhile(c =>
      c > ' ' && !c.isWhitespace && c != '"' && c != '\\' && c != ',' && c != '(' && c != ')' && c != '+'
    )

  val qstring =
    basicString.backtrack.orElse(cats.parse.strings.Json.delimited.parser)

  def lastNDays(
      ref: Instant,
      factor: Int,
      words: NonEmptyList[String]
  ): Parser[Instant] = {
    val word =
      (Parser.charsWhile(_.isWhitespace).?.with1 *> Parser.stringIn(words.toList)).void
    (digits.?.with1 <* word).map { n =>
      val days = n.getOrElse(1) * factor
      ref.minus(days, ChronoUnit.DAYS)
    }

  }

  val localDate: Parser[LocalDate] = {
    val year = digit4
    val minus = Parser.char('-').void

    val date = year ~ (minus.? *> month).? ~ (minus.? *> day).?
    date.map { case ((year, mm), md) =>
      LocalDate.of(year, mm.getOrElse(1), md.getOrElse(1))
    }
  }

  val utc = (Parser.char('Z') | Parser.char('z')).as(ZoneOffset.UTC)

  val localTime: Parser[LocalTime] = {
    val col = Parser.char(':').void

    val time = hour ~ (col.? *> minsec).? ~ (col.? *> minsec).?
    time.map { case ((hour, min), sec) =>
      LocalTime.of(hour, min.getOrElse(0), sec.getOrElse(0))
    }
  }

  def timestamp(zone: ZoneId): Parser[Instant] = {
    val T = Parser.char('T').void
    val dt = localDate ~ (T.? *> localTime).? ~ utc.?
    dt.map { case ((ld, lt), z) =>
      val tz = z.getOrElse(zone)
      LocalDateTime
        .of(ld, lt.getOrElse(LocalTime.MIDNIGHT))
        .atZone(tz)
        .toInstant
    }
  }

  val epochSeconds: Parser[Instant] =
    (Numbers.signedIntString.map(_.toLong) <* Parser.char('s')).map(Instant.ofEpochSecond)
}
