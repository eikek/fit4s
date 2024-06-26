package fit4s.activities.internal

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, ZoneId}

import cats.data.NonEmptyList as Nel
import cats.parse.Parser
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.data.QueryCondition.*
import fit4s.activities.data.{ActivityId, QueryCondition, TagName}
import fit4s.data.Distance

import munit.FunSuite

class ConditionParserTest extends FunSuite:

  val current = Instant.parse("2023-04-02T10:05:00Z")
  val parser = new ConditionParser(ZoneId.of("Europe/Berlin"), current)

  def parse(p: Parser[QueryCondition], in: String): QueryCondition =
    p.parseAll(in).fold(e => sys.error(e.toString()), identity)

  def tag(name: String): TagName = TagName.unsafeFromString(name)

  test("parse single and node"):
    val in = "(& id=1)"
    val result = parser.parseCondition(in)
    val expected = ActivityIdMatch(Nel.of(ActivityId(1)))
    assertEquals(result, expected.asRight)

  test("unwrap and nodes"):
    val in = "(& (& id=1))"
    val result = parser.parseCondition(in)
    val expected = ActivityIdMatch(Nel.of(ActivityId(1)))
    assertEquals(result, expected.asRight)

  test("parse complex condition"):
    val in =
      "(& tag~bike/ tag=commute loc=/my/path started>7days (| distance>100k moved>2h))"
    val result = parser.parseCondition(in)
    val expected = And(
      Nel.of(
        TagAnyStarts(Nel.of(tag("bike/"))),
        TagAnyMatch(Nel.of(tag("commute"))),
        LocationAnyMatch(Nel.of(Path("/my/path"))),
        StartedAfter(current.minus(7, ChronoUnit.DAYS)),
        Or(
          Nel.of(
            DistanceGE(Distance.km(100)),
            MovedGE(Duration.ofHours(2))
          )
        )
      )
    )
    assertEquals(result, expected.asRight)

  test("normalize trees"):
    val in = "(& (| distance<80k) (| tag=bike))"
    val result = parser.parseCondition(in)
    val expected = And(
      Nel.of(
        DistanceLE(Distance.km(80)),
        TagAnyMatch(Nel.of(tag("bike")))
      )
    )
    assertEquals(result, expected.asRight)

  test("parse and"):
    assertEquals(
      parser.andCondition(parser.basicCondition).parseAll("(& distance>5k tag=commute)"),
      And(
        Nel.of(
          DistanceGE(Distance.km(5)),
          TagAnyMatch(Nel.of(tag("commute")))
        )
      ).asRight
    )
    assertEquals(
      parser.andCondition(parser.basicCondition).parseAll("(& tag~bike tag=commute)"),
      And(
        Nel.of(
          TagAnyStarts(Nel.of(tag("bike"))),
          TagAnyMatch(Nel.of(tag("commute")))
        )
      ).asRight
    )

  test("parse distance"):
    assertEquals(
      parser.distanceCondition.parseAll("distance>100k"),
      DistanceGE(Distance.km(100)).asRight
    )
    assertEquals(
      parser.distanceCondition.parseAll("distance<300m"),
      DistanceLE(Distance.meter(300)).asRight
    )

  test("parse tag starts condition"):
    assertEquals(
      parse(parser.tagCondition, "tag~bike/"),
      TagAnyStarts(Nel.of(tag("bike/")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag~bike/+commute"),
      TagAllStarts(Nel.of(tag("bike/"), tag("commute")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag~bike/,commute"),
      TagAnyStarts(Nel.of(tag("bike/"), tag("commute")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag~bike/,commute,\"long ride\""),
      TagAnyStarts(Nel.of(tag("bike/"), tag("commute"), tag("long ride")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag~bike/+commute+\"long ride\""),
      TagAllStarts(Nel.of(tag("bike/"), tag("commute"), tag("long ride")))
    )

  test("parse tag match condition"):
    assertEquals(
      parse(parser.tagCondition, "tag=bike/"),
      TagAnyMatch(Nel.of(tag("bike/")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag=bike/+commute"),
      TagAllMatch(Nel.of(tag("bike/"), tag("commute")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag=bike/,commute"),
      TagAnyMatch(Nel.of(tag("bike/"), tag("commute")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag=bike/,commute,\"long ride\""),
      TagAnyMatch(Nel.of(tag("bike/"), tag("commute"), tag("long ride")))
    )
    assertEquals(
      parse(parser.tagCondition, "tag=bike/+commute+\"long ride\""),
      TagAllMatch(Nel.of(tag("bike/"), tag("commute"), tag("long ride")))
    )
