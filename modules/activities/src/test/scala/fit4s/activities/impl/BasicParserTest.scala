package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.syntax.all._
import fit4s.activities.data.TagName
import fit4s.data.{DeviceProduct, Distance}
import fit4s.profile.types.{Sport, SubSport}
import munit.FunSuite
import org.scalacheck.Prop.forAll
import org.scalacheck.Test
import org.scalacheck.Test.Parameters

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, LocalDateTime}

class BasicParserTest extends FunSuite {
  val current = Instant.parse("2023-04-02T10:05:00Z")
  val parser = new ConditionParser(ConditionParser.defaultZone, current)

  test("parse device") {
    DeviceProduct.all.foreach { p =>
      val out = parser.device.parseAll(p.name.toLowerCase)
      assertEquals(out, p.asRight)
    }
  }

  List(
    "15min" -> Duration.ofMinutes(15),
    "6h" -> Duration.ofHours(6),
    "2hours" -> Duration.ofHours(2),
    "12 h" -> Duration.ofHours(12)
  )
    .foreach { case (in, out) =>
      test(s"parse duration: $in -> $out") {
        val result = parser.duration.parseAll(in)
        assertEquals(result, out.asRight)
      }
    }

  List(
    "3km" -> Distance.km(3),
    "151km" -> Distance.km(151),
    "300m" -> Distance.meter(300),
    "15k" -> Distance.km(15),
    "6 km" -> Distance.km(6)
  )
    .foreach { case (in, out) =>
      test(s"parse distance: $in -> $out") {
        val result = parser.distance.parseAll(in)
        assertEquals(result, out.asRight)
      }
    }

  test("parse sport") {
    Sport.all.foreach { sp =>
      val out = parser.sport.parseAll(sp.typeName)
      assertEquals(out, sp.asRight)
    }
    SubSport.all.foreach { sp =>
      val out = parser.subSport.parseAll(sp.typeName)
      assertEquals(out, sp.asRight)
    }
  }

  test("parse FileId strings") {
    val prop =
      forAll(ActivityQueryGenerator.fileIdGen) { fileId =>
        val out = parser.fileId.parseAll(fileId.asString)
        out == fileId.asString.asRight
      }

    val result = Test.check(Parameters.default, prop)
    if (result.passed) ()
    else sys.error(result.status.toString)
  }

  test("quoted string") {
    assertEquals(
      BasicParser.qstring.parseAll("\"hello world\""),
      "hello world".asRight
    )
    assertEquals(
      BasicParser.qstring.parseAll("\"hello \\\" world\""),
      "hello \" world".asRight
    )
    assertEquals(
      BasicParser.qstring.parseAll("hello-world"),
      "hello-world".asRight
    )
  }

  test("parse listOf") {
    val p = parser.listOf(parser.tagName)(identity, identity)
    assertEquals(p.parseAll("bike/mine"), NonEmptyList.of(tag("bike/mine")).asRight)
    assertEquals(
      p.parseAll("bike/mine"),
      NonEmptyList.of(tag("bike/mine")).asRight
    )
    assertEquals(
      p.parseAll("bike/mine,commute"),
      NonEmptyList.of(tag("bike/mine"), tag("commute")).asRight
    )
    assertEquals(
      p.parseAll("bike/mine, commute"),
      NonEmptyList.of(tag("bike/mine"), tag("commute")).asRight
    )
  }

  List(
    "2023" -> dt(2023),
    "2023-01" -> dt(2023),
    "202302" -> dt(2023, 2),
    "2022-03" -> dt(2022, 3),
    "2020-04-15" -> dt(2020, 4, 15),
    "2021-6-30T12" -> dt(2021, 6, 30, 12),
    "2021-6-30T1215" -> dt(2021, 6, 30, 12, 15),
    "202211051310" -> dt(2022, 11, 5, 13, 10)
  ).foreach { case (in, out) =>
    test(s"parsing date: $in -> $out") {
      val dateTime = parser.instant
        .parseAll(in)
        .fold(err => sys.error(err.toString()), identity)

      assertEquals(dateTime, out)
    }
  }

  List(
    "1week" -> current.minus(7, ChronoUnit.DAYS),
    "2 weeks" -> current.minus(14, ChronoUnit.DAYS),
    "week" -> current.minus(7, ChronoUnit.DAYS),
    "3w" -> current.minus(21, ChronoUnit.DAYS),
    "14 weeks" -> current.minus(14 * 7, ChronoUnit.DAYS)
  ).foreach { case (in, out) =>
    test(s"parse n weeks ago: $in -> $out") {
      val dateTime = parser.lastWeeks
        .parseAll(in)
        .fold(err => sys.error(err.toString()), identity)
      assertEquals(dateTime, out)
    }
  }

  List(
    "1day" -> current.minus(1, ChronoUnit.DAYS),
    "2 days" -> current.minus(2, ChronoUnit.DAYS),
    "day" -> current.minus(1, ChronoUnit.DAYS),
    "3d" -> current.minus(3, ChronoUnit.DAYS),
    "14 days" -> current.minus(14, ChronoUnit.DAYS)
  ).foreach { case (in, out) =>
    test(s"parse n days ago: $in -> $out") {
      val dateTime = parser.lastDays
        .parseAll(in)
        .fold(err => sys.error(err.toString()), identity)
      assertEquals(dateTime, out)
    }
  }

  def dt(year: Int, month: Int = 1, day: Int = 1, hour: Int = 0, min: Int = 0): Instant =
    LocalDateTime.of(year, month, day, hour, min).atZone(parser.zoneId).toInstant

  def tag(name: String): TagName = TagName.unsafeFromString(name)
}
