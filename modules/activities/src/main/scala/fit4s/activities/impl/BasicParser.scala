package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.parse.{Numbers, Parser}
import fit4s.activities.data.TagName
import fit4s.activities.impl.BasicParser.ws
import fit4s.data.{DeviceProduct, Distance}
import fit4s.profile.types.{Sport, SubSport}
import fs2.io.file.Path

import java.time.temporal.ChronoUnit
import java.time._

abstract class BasicParser(zoneId: ZoneId, currentTime: Instant) {
  val instant: Parser[Instant] =
    BasicParser.localDateTime.map(_.atZone(zoneId).toInstant)

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
}

object BasicParser {
  val ws = Parser.charsWhile(_.isWhitespace).void
  val ws0 = ws.rep0.void

  val digit2 = Numbers.digit.repAs[String](1, 2).map(_.toInt)
  val digit4 = Numbers.digit.repAs[String](4, 4).map(_.toInt)
  val digits = Numbers.digits.map(_.toInt)

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
    val d2 = digit2
    val minus = Parser.char('-').void

    val date = year ~ (minus.? *> d2).? ~ (minus.? *> d2).?
    date.map { case ((year, mm), md) =>
      LocalDate.of(year, mm.getOrElse(1), md.getOrElse(1))
    }
  }

  val localTime: Parser[LocalTime] = {
    val d2 = digit2
    val col = Parser.char(':').void

    val time = d2 ~ (col.? *> d2).?
    time.map { case (hour, min) =>
      LocalTime.of(hour, min.getOrElse(0))
    }
  }

  val localDateTime: Parser[LocalDateTime] = {
    val T = Parser.char('T').void
    val dt = localDate ~ (T.? *> localTime).?
    dt.map { case (ld, lt) =>
      LocalDateTime.of(ld, lt.getOrElse(LocalTime.MIDNIGHT))
    }
  }
}
