package fit4s.activities.impl

import cats.data.NonEmptyList
import doobie.Fragment
import doobie.implicits._
import fit4s.activities.Query
import fit4s.activities.Query.Condition
import fit4s.activities.records.{ActivityRecord, ActivityTagRecord, TagRecord}
import fit4s.activities.records.DoobieMeta._

object QueryBuilder {

  val activityT = ActivityRecord.table
  val tagT = TagRecord.table
  val activityTagT = ActivityTagRecord.table

  def build(q: Query): Fragment =
    ???

  def condition(c: Condition): Fragment =
    c match {
      case Query.TagStarts(name) =>
        fr"tag.name like $name"

      case Query.TagMatch(names) =>
        combineFragments(names.toList.map(n => fr"tag.name = $n"))

      case _ => ???

    }

  def combineFragments(cs: List[Fragment]): Fragment =
    cs match {
      case a :: Nil => a
      case _        => cs.foldSmash1(fr"(", fr" AND ", fr")")
    }
}
