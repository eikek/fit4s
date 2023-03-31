package fit4s.activities

import cats.data.NonEmptyList
import fit4s.activities.data.TagName
import fit4s.data.Distance
import fit4s.profile.types.Sport
import fs2.io.file.Path

import java.time.{Duration, Instant}

trait Query {}

object Query {

  sealed trait Condition

  case class TagStarts(name: TagName) extends Condition

  case class TagMatch(names: NonEmptyList[TagName]) extends Condition

  case class LocationMatch(paths: NonEmptyList[Path]) extends Condition

  case class LocationStarts(path: String) extends Condition

  case class FileIdMatch(fileId: NonEmptyList[String]) extends Condition

  case class SportMatch(sports: NonEmptyList[Sport]) extends Condition

  case class StartedBefore(time: Instant) extends Condition

  case class StartedAfter(time: Instant) extends Condition

  case class DistanceGE(distance: Distance) extends Condition

  case class DistanceLE(distance: Distance) extends Condition

  case class ElapsedGE(time: Duration) extends Condition

  case class ElapsedLE(time: Duration) extends Condition

  case class MovedGE(time: Duration) extends Condition

  case class MovedLE(time: Duration) extends Condition

  case class NotesMatch(text: String) extends Condition

  case class And(elements: NonEmptyList[Condition]) extends Condition

  case class Or(elements: NonEmptyList[Condition]) extends Condition

  case class Not(condition: Condition) extends Condition
}
