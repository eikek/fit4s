package fit4s.activities

import cats.data.{NonEmptyList => Nel}
import fit4s.activities.ActivityQuery.{Condition, OrderBy}
import fit4s.activities.data.{ActivityId, Page, TagName}
import fit4s.data.{DeviceProduct, Distance}
import fit4s.profile.types.{Sport, SubSport}
import fs2.io.file.Path

import java.time.{Duration, Instant}

final case class ActivityQuery(condition: Option[Condition], order: OrderBy, page: Page)

object ActivityQuery {

  sealed trait OrderBy
  object OrderBy {
    case object StartTime extends OrderBy
    case object Distance extends OrderBy
  }

  sealed trait Condition extends Product
  object Condition {
    case class TagAllStarts(name: Nel[TagName]) extends Condition

    case class TagAnyStarts(name: Nel[TagName]) extends Condition

    case class TagAllMatch(names: Nel[TagName]) extends Condition

    case class TagAnyMatch(names: Nel[TagName]) extends Condition

    case class LocationAllMatch(paths: Nel[Path]) extends Condition

    case class LocationAnyMatch(paths: Nel[Path]) extends Condition

    case class LocationAllStarts(path: Nel[Path]) extends Condition

    case class LocationAnyStarts(path: Nel[Path]) extends Condition

    case class FileIdMatch(fileId: String) extends Condition

    case class ActivityIdMatch(activityId: Nel[ActivityId]) extends Condition

    case class DeviceMatch(device: DeviceProduct) extends Condition

    case class SportMatch(sports: Sport) extends Condition

    case class SubSportMatch(sports: SubSport) extends Condition

    case class StartedBefore(time: Instant) extends Condition

    case class StartedAfter(time: Instant) extends Condition

    case class DistanceGE(distance: Distance) extends Condition

    case class DistanceLE(distance: Distance) extends Condition

    case class ElapsedGE(time: Duration) extends Condition

    case class ElapsedLE(time: Duration) extends Condition

    case class MovedGE(time: Duration) extends Condition

    case class MovedLE(time: Duration) extends Condition

    case class NotesMatch(text: String) extends Condition

    case class And(elements: Nel[Condition]) extends Condition

    case class Or(elements: Nel[Condition]) extends Condition

    case class Not(condition: Condition) extends Condition

    private[activities] def normalizeCondition(c: Condition): Condition =
      c match {
        case Condition.And(inner) =>
          val nodes = spliceAnd(inner)
          if (nodes.tail.isEmpty) normalizeCondition(nodes.head)
          else Condition.And(nodes.map(normalizeCondition))

        case Condition.Or(inner) =>
          val nodes = spliceOr(inner)
          if (nodes.tail.isEmpty) normalizeCondition(nodes.head)
          else Condition.Or(nodes.map(normalizeCondition))

        case Condition.Not(inner) =>
          inner match {
            case Condition.Not(inner2) =>
              normalizeCondition(inner2)
            case _ =>
              Condition.Not(normalizeCondition(inner))
          }
        case _ => c
      }

    private def spliceAnd(nodes: Nel[Condition]): Nel[Condition] =
      nodes.flatMap {
        case Condition.And(inner) =>
          spliceAnd(inner)
        case node =>
          Nel.of(node)
      }

    private def spliceOr(nodes: Nel[Condition]): Nel[Condition] =
      nodes.flatMap {
        case Condition.Or(inner) =>
          spliceOr(inner)
        case node =>
          Nel.of(node)
      }
  }
}
