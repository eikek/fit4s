package fit4s.activities.impl

import doobie.implicits._
import doobie.{ConnectionIO, Fragment, Query0}
import fit4s.activities.ActivityQuery
import fit4s.activities.ActivityQuery.{Condition, OrderBy}
import fit4s.activities.data.{ActivityId, ActivitySessionSummary, Page, TagName}
import fit4s.activities.records._
import fs2.io.file.Path
import DoobieMeta._

object ActivityQueryBuilder {

  val activityT = ActivityRecord.table
  val activitySessionT = ActivitySessionRecord.table
  val activityLocT = ActivityLocationRecord.table
  val tagT = TagRecord.table
  val activityTagT = ActivityTagRecord.table

  def buildQuery(
      q: ActivityQuery
  ): Query0[(ActivityRecord, ActivityLocationRecord, ActivitySessionRecord)] =
    selectFragment(q)
      .query[(ActivityRecord, ActivityLocationRecord, ActivitySessionRecord)]

  def selectFragment(q: ActivityQuery): Fragment = {
    val cols =
      (ActivityRecord.columnList(Some("pa")) :::
        ActivityLocationRecord.columnList(Some("loc")) :::
        ActivitySessionRecord.columnList(Some("act")))
        .foldSmash1(Fragment.empty, sql", ", Fragment.empty)
    val select = fr"SELECT DISTINCT $cols"
    val from = join
    val where = makeWhere(q.condition)
    val order = q.order match {
      case OrderBy.Distance  => fr"ORDER BY act.distance, act.id"
      case OrderBy.StartTime => fr"ORDER BY act.start_time desc, act.id"
    }
    val limit =
      if (q.page == Page.unlimited) Fragment.empty
      else fr"LIMIT ${q.page.limit} OFFSET ${q.page.offset}"

    select ++ from ++ where ++ order ++ limit
  }

  def buildSummary(q: Option[ActivityQuery.Condition]): Query0[ActivitySessionSummary] =
    summaryFragment(q).query[ActivitySessionSummary]

  def summaryFragment(q: Option[ActivityQuery.Condition]): Fragment = {
    val selectGrouped =
      fr"""SELECT act.sport, min(act.start_time), max(act.end_time),
           sum(act.moving_time), sum(act.elapsed_time), sum(act.distance),
           sum(act.calories), sum(act.total_ascend), sum(act.total_descend),
           min(act.min_temp), max(act.max_temp), avg(act.avg_temp),
           min(act.min_hr), max(act.max_hr), avg(act.avg_hr), max(act.max_speed),
           avg(act.avg_speed), max(act.max_power), avg(act.avg_power), avg(act.norm_power),
           avg(act.avg_cadence), avg(act.avg_grade), avg(act.iff), avg(act.tss),
           count(act.id)
        """

    val select =
      fr"""SELECT DISTINCT act.id, act.sport, act.start_time, act.end_time,
           act.moving_time, act.elapsed_time, act.distance,
           act.calories, act.total_ascend, act.total_descend,
           act.min_temp, act.max_temp, act.avg_temp,
           act.min_hr, act.max_hr, act.avg_hr, act.max_speed,
           act.avg_speed, act.max_power, act.avg_power, act.norm_power,
           act.avg_cadence, act.avg_grade, act.iff, act.tss
        """

    val where = makeWhere(q)

    sql"""WITH
              session as ($select $join $where)
            $selectGrouped
            FROM session act
            GROUP BY act.sport
           """
  }

  def activityIdFragment(q: Option[ActivityQuery.Condition]) = {
    val select = sql"SELECT DISTINCT pa.id"
    val where = makeWhere(q)
    select ++ join ++ where
  }

  def tagsForActivity(id: ActivityId): ConnectionIO[Vector[TagRecord]] = {
    val cols =
      TagRecord.columnList(Some("t")).foldSmash1(Fragment.empty, sql", ", Fragment.empty)
    sql"""SELECT DISTINCT $cols
          FROM ${TagRecord.table} t
          INNER JOIN ${ActivityTagRecord.table} at ON at.tag_id = t.id
          WHERE at.activity_id = $id AND t.id <> ${TagRecord.softDelete.id}"""
      .query[TagRecord]
      .to[Vector]
  }

  private def join: Fragment =
    sql"""
        FROM $activityT pa
        INNER JOIN $activitySessionT act ON act.activity_id = pa.id
        INNER JOIN $activityLocT loc ON loc.id = pa.location_id
        LEFT JOIN $activityTagT at ON at.activity_id = pa.id
        LEFT JOIN $tagT tag ON at.tag_id = tag.id
      """

  private def makeWhere(cond: Option[Condition]): Fragment = {
    val hideDeleted =
      fr"WHERE (tag.id is null OR tag.id <> ${TagRecord.softDelete.id})"

    cond match {
      case Some(c) => hideDeleted ++ fr"AND (" ++ condition(c) ++ sql")"
      case None    => hideDeleted
    }
  }

  def condition(c: Condition): Fragment =
    c match {
      case Condition.TagAllStarts(names) =>
        combineFragments(
          names.toList.map(name => fr"tag.name ilike ${wildcardEnd(name)}"),
          fr" AND "
        )

      case Condition.TagAnyStarts(names) =>
        combineFragments(
          names.toList.map(name => fr"tag.name ilike ${wildcardEnd(name)}"),
          fr" OR "
        )

      case Condition.TagAllMatch(names) =>
        combineFragments(
          names.toList.map(n => fr"lower(tag.name) = lower($n)"),
          fr" AND "
        )

      case Condition.TagAnyMatch(names) =>
        combineFragments(names.toList.map(n => fr"lower(tag.name) = lower($n)"), fr" OR ")

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

      case Condition.ActivityIdMatch(ids) =>
        combineFragments(ids.toList.map(id => fr"pa.id = $id"), sql" OR ")

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

      case Condition.NotesContains(text) =>
        fr"pa.notes ilike ${wildcard(text)}"

      case Condition.NameContains(text) =>
        fr"pa.name ilike ${wildcard(text)}"

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
