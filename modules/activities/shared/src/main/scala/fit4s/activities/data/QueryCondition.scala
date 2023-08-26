package fit4s.activities.data

import java.time.{Duration, Instant, ZoneId}

import cats.data.{NonEmptyList => Nel}
import cats.syntax.all._
import fs2.io.file.Path

import fit4s.activities.internal.ConditionParser
import fit4s.data.{DeviceProduct, Distance}
import fit4s.profile.types.{Sport, SubSport}

sealed trait QueryCondition extends Product

object QueryCondition {
  type QueryReader = String => Either[String, QueryCondition]

  def parser(zoneId: ZoneId, currentTime: Instant): QueryReader = {
    val cp = new ConditionParser(zoneId, currentTime)
    (cp.parseCondition _).andThen(_.left.map(_.show))
  }

  def apply(id: ActivityId, more: ActivityId*): QueryCondition =
    ActivityIdMatch(Nel(id, more.toList))

  case class TagAllStarts(name: Nel[TagName]) extends QueryCondition

  case class TagAnyStarts(name: Nel[TagName]) extends QueryCondition

  case class TagAllMatch(names: Nel[TagName]) extends QueryCondition

  case class TagAnyMatch(names: Nel[TagName]) extends QueryCondition

  case class LocationAllMatch(paths: Nel[Path]) extends QueryCondition

  case class LocationAnyMatch(paths: Nel[Path]) extends QueryCondition

  case class LocationAllStarts(path: Nel[Path]) extends QueryCondition

  case class LocationAnyStarts(path: Nel[Path]) extends QueryCondition

  case class FileIdMatch(fileId: String) extends QueryCondition

  case class ActivityIdMatch(activityId: Nel[ActivityId]) extends QueryCondition

  case class DeviceMatch(device: DeviceProduct) extends QueryCondition

  case class SportMatch(sports: Sport) extends QueryCondition

  case class SubSportMatch(sports: SubSport) extends QueryCondition

  case class StartedBefore(time: Instant) extends QueryCondition

  case class StartedAfter(time: Instant) extends QueryCondition

  case class DistanceGE(distance: Distance) extends QueryCondition

  case class DistanceLE(distance: Distance) extends QueryCondition

  case class ElapsedGE(time: Duration) extends QueryCondition

  case class ElapsedLE(time: Duration) extends QueryCondition

  case class MovedGE(time: Duration) extends QueryCondition

  case class MovedLE(time: Duration) extends QueryCondition

  case class NotesContains(text: String) extends QueryCondition

  case class NameContains(text: String) extends QueryCondition

  case class StravaLink(flag: Boolean) extends QueryCondition

  case class And(elements: Nel[QueryCondition]) extends QueryCondition

  case class Or(elements: Nel[QueryCondition]) extends QueryCondition

  case class Not(QueryCondition: QueryCondition) extends QueryCondition

  private[activities] def normalize(c: QueryCondition): QueryCondition =
    c match {
      case QueryCondition.And(inner) =>
        val nodes = spliceAnd(inner)
        if (nodes.tail.isEmpty) normalize(nodes.head)
        else QueryCondition.And(nodes.map(normalize))

      case QueryCondition.Or(inner) =>
        val nodes = spliceOr(inner)
        if (nodes.tail.isEmpty) normalize(nodes.head)
        else QueryCondition.Or(nodes.map(normalize))

      case QueryCondition.Not(inner) =>
        inner match {
          case QueryCondition.Not(inner2) =>
            normalize(inner2)
          case _ =>
            QueryCondition.Not(normalize(inner))
        }
      case _ => c
    }

  private def spliceAnd(nodes: Nel[QueryCondition]): Nel[QueryCondition] =
    nodes.flatMap {
      case QueryCondition.And(inner) =>
        spliceAnd(inner)
      case node =>
        Nel.of(node)
    }

  private def spliceOr(nodes: Nel[QueryCondition]): Nel[QueryCondition] =
    nodes.flatMap {
      case QueryCondition.Or(inner) =>
        spliceOr(inner)
      case node =>
        Nel.of(node)
    }
}
