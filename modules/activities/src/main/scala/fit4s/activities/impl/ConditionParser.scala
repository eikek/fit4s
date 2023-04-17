package fit4s.activities.impl

import java.time._

import cats.data.NonEmptyList
import cats.parse.Parser

import fit4s.activities.ActivityQuery.Condition
import fit4s.activities.ActivityQuery.Condition._

final class ConditionParser(val zoneId: ZoneId, currentTime: Instant)
    extends BasicParser(zoneId, currentTime) {

  def parseCondition(c: String): Either[Parser.Error, Condition] = {
    val str =
      if (c.startsWith("(")) c.trim
      else s"(& ${c.trim})"

    condition.parseAll(str).map(Condition.normalizeCondition)
  }

  val tagCondition: Parser[Condition] =
    (Parser.stringIn(List("tag~", "tag=")) ~ listOf(tagName)(Right(_), Left(_)))
      .map { case (word, c) =>
        if (word.endsWith("~")) c.fold(TagAnyStarts, TagAllStarts)
        else c.fold(TagAnyMatch, TagAllMatch)
      }

  val locationCondition: Parser[Condition] =
    (Parser.stringIn(List("loc~", "location~", "loc=", "location=")) ~ listOf(
      path
    )(Right(_), Left(_)))
      .map { case (word, c) =>
        if (word.endsWith("~")) c.fold(LocationAnyStarts, LocationAllStarts)
        else c.fold(LocationAnyMatch, LocationAllMatch)
      }

  val fileIdCondition: Parser[Condition] =
    (Parser.string("file_id=") *> fileId).map(FileIdMatch)

  val idCondition: Parser[Condition] =
    (Parser.string("id=") *> activityId.repSep(BasicParser.commaSep)).map(ActivityIdMatch)

  val sportCondition: Parser[Condition] =
    (Parser.string("sport=") *> sport).map(SportMatch)

  val subSportCondition: Parser[Condition] =
    (Parser.stringIn(List("sub-sport=", "sub=")).void *> subSport).map(SubSportMatch)

  val startedCondition: Parser[Condition] =
    (Parser.stringIn(List("start>", "started>", "start<", "started<")) ~ dateTime).map {
      case (word, time) =>
        if (word.endsWith(">")) StartedAfter(time)
        else StartedBefore(time)
    }

  val distanceCondition: Parser[Condition] =
    (Parser.stringIn(List("dist>", "distance>", "dist<", "distance<")) ~ distance)
      .map { case (word, dst) =>
        if (word.endsWith(">")) DistanceGE(dst)
        else DistanceLE(dst)
      }

  val durationCondition: Parser[Condition] = {
    val words =
      Parser.stringIn(List("elapsed>", "elapsed<", "moved>", "moved<", "time>", "time<"))

    (words ~ duration).map {
      case ("elapsed>", d) => ElapsedGE(d)
      case ("elapsed<", d) => ElapsedLE(d)
      case ("moved>", d)   => MovedGE(d)
      case ("moved<", d)   => MovedLE(d)
      case ("time>", d)    => Or(NonEmptyList.of(MovedGE(d), ElapsedGE(d)))
      case ("time<", d)    => Or(NonEmptyList.of(MovedLE(d), ElapsedLE(d)))
    }
  }

  val notesCondition: Parser[Condition] =
    (Parser.stringIn(List("notes=", "notes:")).void *> BasicParser.qstring)
      .map(NotesContains)

  val nameCondition: Parser[Condition] =
    (Parser.stringIn(List("name=", "name:")).void *> BasicParser.qstring)
      .map(NameContains)

  val deviceMatchCondition: Parser[Condition] =
    (Parser.stringIn(List("device=", "dev=")) *> device).map(DeviceMatch)

  val stravaLinkCondition: Parser[Condition] =
    (Parser.string("strava-link=") *>
      Parser.ignoreCase("yes").as(true).orElse(Parser.ignoreCase("no").as(false)))
      .map(StravaLink.apply)

  def andCondition(inner: Parser[Condition]): Parser[Condition] =
    inner
      .repSep(BasicParser.ws)
      .between(BasicParser.parenAnd, BasicParser.parenClose)
      .map(And)

  def orCondition(inner: Parser[Condition]): Parser[Condition] =
    inner
      .repSep(BasicParser.ws)
      .between(BasicParser.parenOr, BasicParser.parenClose)
      .map(Or)

  def notCondition(inner: Parser[Condition]): Parser[Condition] =
    (Parser.char('!') *> inner).map(Not)

  val basicCondition: Parser[Condition] =
    Parser.oneOf(
      tagCondition ::
        locationCondition ::
        fileIdCondition ::
        sportCondition ::
        subSportCondition ::
        startedCondition ::
        distanceCondition ::
        durationCondition ::
        notesCondition ::
        nameCondition ::
        deviceMatchCondition ::
        stravaLinkCondition ::
        idCondition :: Nil
    )

  val condition: Parser[Condition] =
    Parser.recursive[Condition] { recurse =>
      val andP = andCondition(recurse)
      val orP = orCondition(recurse)
      val notP = notCondition(recurse)
      Parser.oneOf(basicCondition :: andP :: orP :: notP :: Nil)
    }

}
