package fit4s.activities.impl

//import cats.data.NonEmptyList
import doobie.{Fragment, Query0}
import doobie.implicits._
import fit4s.activities.ActivityQuery
import fit4s.activities.ActivityQuery.{Condition, OrderBy}
import fit4s.activities.data.TagName
import fit4s.activities.records._
import fit4s.activities.records.DoobieMeta._
import fs2.io.file.Path

object ActivityQueryBuilder {

  val activityT = ActivityRecord.table
  val activitySessionT = ActivitySessionRecord.table
  val activityLocT = ActivityLocationRecord.table
  val tagT = TagRecord.table
  val activityTagT = ActivityTagRecord.table

  def buildQuery(q: ActivityQuery): Query0[ActivitySessionRecord] =
    selectFragment(q).query[ActivitySessionRecord]

  def selectFragment(q: ActivityQuery): Fragment = {
    val cols = ActivitySessionRecord
      .columnList(Some("act"))
      .foldSmash1(Fragment.empty, sql", ", Fragment.empty)
    val select = fr"SELECT $cols"
    val from = join
    val where = q.condition match {
      case Some(c) => fr"WHERE" ++ condition(c)
      case None    => Fragment.empty
    }
    val order = orderBy(q.order)

    select ++ from ++ where ++ order
  }

  def join: Fragment =
    sql"""
        FROM $activitySessionT act
        INNER JOIN $activityT pa ON act.activity_id = pa.id
        INNER JOIN $activityLocT loc ON loc.id = pa.location_id
        LEFT JOIN $activityTagT at ON at.activity_id = pa.id
        LEFT JOIN $tagT tag ON at.tag_id = tag.id
      """

  def orderBy(order: OrderBy): Fragment =
    order match {
      case OrderBy.Distance  => fr"ORDER BY act.distance"
      case OrderBy.StartTime => fr"ORDER BY act.start_time"
    }

  def condition(c: Condition): Fragment =
    c match {
      case Condition.TagAllStarts(names) =>
        combineFragments(
          names.toList.map(name => fr"tag.name like ${wildcardEnd(name)}"),
          fr" AND "
        )

      case Condition.TagAnyStarts(names) =>
        combineFragments(
          names.toList.map(name => fr"tag.name like ${wildcardEnd(name)}"),
          fr" OR "
        )

      case Condition.TagAllMatch(names) =>
        combineFragments(names.toList.map(n => fr"tag.name = $n"), fr" AND ")

      case Condition.TagAnyMatch(names) =>
        combineFragments(names.toList.map(n => fr"tag.name = $n"), fr" OR ")

      case Condition.LocationAllMatch(nel) =>
        combineFragments(nel.toList.map(l => fr"loc.location = $l"), fr" AND ")

      case Condition.LocationAnyMatch(nel) =>
        combineFragments(nel.toList.map(l => fr"loc.location = $l"), fr" OR ")

      case Condition.LocationAllStarts(nel) =>
        combineFragments(
          nel.toList.map(p => fr"loc.location like ${wildcardEnd(p)}"),
          fr" AND "
        )

      case Condition.LocationAnyStarts(nel) =>
        combineFragments(
          nel.toList.map(p => fr"loc.location like ${wildcardEnd(p)}"),
          fr" OR "
        )

      case Condition.FileIdMatch(id) =>
        fr"pa.file_id = $id"

      case Condition.SportMatch(sport) =>
        fr"act.sport = $sport"

      case Condition.SubSportMatch(sport) =>
        fr"act.sub_sport = $sport"

      case Condition.StartedBefore(time) =>
        fr"act.start_time <= $time"

      case Condition.StartedAfter(time) =>
        fr"act.start_time >= $time"

      case Condition.DistanceGE(m) =>
        fr"act.distance >= $m"

      case Condition.DistanceLE(m) =>
        fr"act.distance <= $m"

      case Condition.ElapsedGE(t) =>
        fr"act.elapsed_time >= $t"

      case Condition.ElapsedLE(t) =>
        fr"act.elapsed_time <= $t"

      case Condition.MovedGE(t) =>
        fr"act.moving_time >= $t"

      case Condition.MovedLE(t) =>
        fr"act.moving_time <= $t"

      case Condition.NotesMatch(text) =>
        fr"pa.notes like ${wildcard(text)}"

      case Condition.DeviceMatch(device) =>
        fr"pa.device = $device"

      case Condition.And(elements) =>
        val inner = elements.map(condition).toList
        combineFragments(inner, fr" AND ")

      case Condition.Or(elements) =>
        val inner = elements.map(condition).toList
        combineFragments(inner, fr" OR ")

      case Condition.Not(el) =>
        val inner = condition(el)
        fr"NOT($inner)"
    }

  def combineFragments(cs: List[Fragment], sep: Fragment): Fragment =
    cs match {
      case a :: Nil => a
      case _        => cs.foldSmash1(fr"(", sep, fr")")
    }

  def wildcardEnd(s: TagName): String = s"${s.name}%"
  def wildcardEnd(p: Path): String = s"${p.toString}%"
  def wildcard(notes: String): String = s"%$notes%"
}
