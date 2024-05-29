package fit4s.activities.internal

import java.time.*

import cats.data.NonEmptyList
import cats.parse.Parser

import fit4s.activities.data.QueryCondition
import fit4s.activities.data.QueryCondition.*

final private[activities] class ConditionParser(val zoneId: ZoneId, currentTime: Instant)
    extends BasicParser(zoneId, currentTime):

  def parseCondition(c: String): Either[Parser.Error, QueryCondition] =
    val str =
      if (c.startsWith("(")) c.trim
      else s"(& ${c.trim})"

    condition.parseAll(str).map(QueryCondition.normalize)

  val tagCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("tag~", "tag=")) ~ listOf(tagName)(Right(_), Left(_)))
      .map { case (word, c) =>
        if (word.endsWith("~")) c.fold(TagAnyStarts.apply, TagAllStarts.apply)
        else c.fold(TagAnyMatch.apply, TagAllMatch.apply)
      }

  val locationCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("loc~", "location~", "loc=", "location=")) ~ listOf(
      path
    )(Right(_), Left(_)))
      .map { case (word, c) =>
        if (word.endsWith("~")) c.fold(LocationAnyStarts.apply, LocationAllStarts.apply)
        else c.fold(LocationAnyMatch.apply, LocationAllMatch.apply)
      }

  val fileIdCondition: Parser[QueryCondition] =
    (Parser.string("file_id=") *> fileId).map(FileIdMatch.apply)

  val idCondition: Parser[QueryCondition] =
    (Parser.string("id=") *> activityId.repSep(BasicParser.commaSep))
      .map(ActivityIdMatch.apply)

  val sportCondition: Parser[QueryCondition] =
    (Parser.string("sport=") *> sport).map(SportMatch.apply)

  val subSportCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("sub-sport=", "sub=")).void *> subSport)
      .map(SubSportMatch.apply)

  val startedCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("start>", "started>", "start<", "started<")) ~ dateTime).map:
      case (word, time) =>
        if (word.endsWith(">")) StartedAfter(time)
        else StartedBefore(time)

  val distanceCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("dist>", "distance>", "dist<", "distance<")) ~ distance)
      .map { case (word, dst) =>
        if (word.endsWith(">")) DistanceGE(dst)
        else DistanceLE(dst)
      }

  val durationCondition: Parser[QueryCondition] =
    val words =
      Parser.stringIn(List("elapsed>", "elapsed<", "moved>", "moved<", "time>", "time<"))

    (words ~ duration).map:
      case ("elapsed>", d) => ElapsedGE(d)
      case ("elapsed<", d) => ElapsedLE(d)
      case ("moved>", d)   => MovedGE(d)
      case ("moved<", d)   => MovedLE(d)
      case ("time>", d)    => Or(NonEmptyList.of(MovedGE(d), ElapsedGE(d)))
      case ("time<", d)    => Or(NonEmptyList.of(MovedLE(d), ElapsedLE(d)))

  val notesCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("notes=", "notes:")).void *> BasicParser.qstring)
      .map(NotesContains.apply)

  val nameCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("name=", "name:")).void *> BasicParser.qstring)
      .map(NameContains.apply)

  val deviceMatchCondition: Parser[QueryCondition] =
    (Parser.stringIn(List("device=", "dev=")) *> device).map(DeviceMatch.apply)

  val stravaLinkCondition: Parser[QueryCondition] =
    (Parser.string("strava-link=") *>
      Parser.ignoreCase("yes").as(true).orElse(Parser.ignoreCase("no").as(false)))
      .map(StravaLink.apply)

  def andCondition(inner: Parser[QueryCondition]): Parser[QueryCondition] =
    inner
      .repSep(BasicParser.ws)
      .between(BasicParser.parenAnd, BasicParser.parenClose)
      .map(And.apply)

  def orCondition(inner: Parser[QueryCondition]): Parser[QueryCondition] =
    inner
      .repSep(BasicParser.ws)
      .between(BasicParser.parenOr, BasicParser.parenClose)
      .map(Or.apply)

  def notCondition(inner: Parser[QueryCondition]): Parser[QueryCondition] =
    (Parser.char('!') *> inner).map(Not.apply)

  val basicCondition: Parser[QueryCondition] =
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

  val condition: Parser[QueryCondition] =
    Parser.recursive[QueryCondition] { recurse =>
      val andP = andCondition(recurse)
      val orP = orCondition(recurse)
      val notP = notCondition(recurse)
      Parser.oneOf(basicCondition :: andP :: orP :: notP :: Nil)
    }
