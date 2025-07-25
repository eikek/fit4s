package fit4s.activities.impl

import cats.data.NonEmptyList
import fs2.io.file.Path

import fit4s.activities.data.*
import fit4s.activities.records.*
import fit4s.activities.records.DoobieImplicits.{*, given}
import fit4s.strava.data.StravaActivityId

import doobie.*
import doobie.syntax.all.*

object ActivityQueryBuilder:

  val activityT = RActivity.table
  val activitySessionT = RActivitySession.table
  val activityLocT = RActivityLocation.table
  val tagT = RTag.table
  val activityTagT = RActivityTag.table
  val stravaT = RActivityStrava.table

  def buildQuery(
      q: ActivityQuery
  ): Query0[(Activity, Option[StravaActivityId], Location, ActivitySession)] =
    selectFragment(q)
      .query[(Activity, Option[StravaActivityId], Location, ActivitySession)]

  def selectFragment(q: ActivityQuery): Fragment =
    val cols =
      (RActivity.columnList(Some("pa")) :::
        List(RActivityStrava.columnList(Some("st")).last) :::
        RActivityLocation.columnList(Some("loc")) :::
        RActivitySession.columnList(Some("act"))).commas
    val select = fr"SELECT DISTINCT $cols"
    val from = join
    val where = makeWhere(q.condition)
    val order = fr"ORDER BY act.start_time desc, act.id"
    val limit = q.page.asFragment
    select ++ from ++ where ++ order ++ limit

  def buildSummary(q: ActivityQuery): Query0[ActivitySessionSummary] =
    summaryFragment(q).query[ActivitySessionSummary]

  def summaryFragment(q: ActivityQuery): Fragment =
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

    val where = makeWhere(q.condition)
    val page = q.page.asFragment

    sql"""WITH
              session as ($select $join $where $page)
            $selectGrouped
            FROM session act
            GROUP BY act.sport
           """

  def activityIdFragment(q: Option[QueryCondition]) =
    val select = sql"SELECT DISTINCT pa.id"
    val where = makeWhere(q)
    select ++ join ++ where

  def activityRereadDataFragment(q: QueryCondition) =
    val colList = List(fr"pa.id", fr"pa.file_id", fr"pa.path", fr"loc.location").commas
    val select = fr"SELECT DISTINCT $colList"
    val where = makeWhere(Some(q))
    select ++ join ++ where

  def tagsForActivity(id: ActivityId): ConnectionIO[Vector[Tag]] =
    val cols = RTag.columnList(Some("t")).commas
    sql"""SELECT DISTINCT $cols
          FROM ${RTag.table} t
          INNER JOIN ${RActivityTag.table} at ON at.tag_id = t.id
          WHERE at.activity_id = $id AND t.id <> ${RTag.softDelete.id}
          ORDER BY t.name"""
      .query[Tag]
      .to[Vector]

  private def join: Fragment =
    sql"""
        FROM $activityT pa
        INNER JOIN $activitySessionT act ON act.activity_id = pa.id
        INNER JOIN $activityLocT loc ON loc.id = pa.location_id
        LEFT JOIN $stravaT st ON st.activity_id = act.activity_id
      """

  private def makeWhere(cond: Option[QueryCondition]): Fragment =
    val hideTags = NonEmptyList.of(RTag.softDelete.id)
    val hideDeleted =
      fr"WHERE (pa.id not in (${activitiesWithAllTagIds(hideTags)}))"

    cond match
      case Some(c) => hideDeleted ++ sql"AND (" ++ condition(c) ++ fr")"
      case None    => hideDeleted

  def condition(c: QueryCondition): Fragment =
    c match
      case QueryCondition.TagAllStarts(names) =>
        val in = activitiesWithAllTagsLike(names)
        fr"pa.id in ($in)"

      case QueryCondition.TagAnyStarts(names) =>
        val in = activitiesWithEitherTagLike(names)
        fr"pa.id in ($in)"

      case QueryCondition.TagAllMatch(names) =>
        val in = activitiesWithAllTags(names)
        fr"pa.id in ($in)"

      case QueryCondition.TagAnyMatch(names) =>
        val in = activitiesWithEitherTag(names)
        fr"pa.id in ($in)"

      case QueryCondition.LocationAllMatch(nel) =>
        combineFragments(nel.toList.map(l => fr"loc.location = $l"), fr" AND ")

      case QueryCondition.LocationAnyMatch(nel) =>
        combineFragments(nel.toList.map(l => fr"loc.location = $l"), fr" OR ")

      case QueryCondition.LocationAllStarts(nel) =>
        combineFragments(
          nel.toList.map(p => fr"loc.location like ${wildcardEnd(p)}"),
          fr" AND "
        )

      case QueryCondition.LocationAnyStarts(nel) =>
        combineFragments(
          nel.toList.map(p => fr"loc.location like ${wildcardEnd(p)}"),
          fr" OR "
        )

      case QueryCondition.FileIdMatch(id) =>
        fr"pa.file_id = $id"

      case QueryCondition.ActivityIdMatch(ids) =>
        combineFragments(ids.toList.map(id => fr"pa.id = $id"), sql" OR ")

      case QueryCondition.SportMatch(sport) =>
        fr"act.sport = $sport"

      case QueryCondition.SubSportMatch(sport) =>
        fr"act.sub_sport = $sport"

      case QueryCondition.StartedBefore(time) =>
        fr"act.start_time <= $time"

      case QueryCondition.StartedAfter(time) =>
        fr"act.start_time >= $time"

      case QueryCondition.DistanceGE(m) =>
        fr"act.distance >= $m"

      case QueryCondition.DistanceLE(m) =>
        fr"act.distance <= $m"

      case QueryCondition.ElapsedGE(t) =>
        fr"act.elapsed_time >= $t"

      case QueryCondition.ElapsedLE(t) =>
        fr"act.elapsed_time <= $t"

      case QueryCondition.MovedGE(t) =>
        fr"act.moving_time >= $t"

      case QueryCondition.MovedLE(t) =>
        fr"act.moving_time <= $t"

      case QueryCondition.NotesContains(text) =>
        fr"pa.notes ilike ${wildcard(text)}"

      case QueryCondition.NameContains(text) =>
        fr"pa.name ilike ${wildcard(text)}"

      case QueryCondition.DeviceMatch(device) =>
        fr"pa.device = $device"

      case QueryCondition.StravaLink(flag) =>
        val ast = RActivityStrava.table
        if (flag) fr"pa.id IN (SELECT activity_id FROM $ast)"
        else fr"pa.id NOT IN (SELECT activity_id FROM $ast)"

      case QueryCondition.And(elements) =>
        val inner = elements.map(condition).toList
        combineFragments(inner, fr" AND ")

      case QueryCondition.Or(elements) =>
        val inner = elements.map(condition).toList
        combineFragments(inner, fr" OR ")

      case QueryCondition.Not(el) =>
        val inner = condition(el)
        fr"NOT($inner)"

  def combineFragments(cs: List[Fragment], sep: Fragment): Fragment =
    cs match
      case a :: Nil => a
      case _        => cs.foldSmash1(fr"(", sep, fr")")

  def wildcardEnd(s: TagName): String = s"${s.name}%"
  def wildcardEnd(p: Path): String = s"${p.toString}%"
  def wildcard(notes: String): String = s"%$notes%"

  def activitiesWithEitherTag(tags: NonEmptyList[TagName]): Fragment =
    val names =
      tags.toList
        .map(_.toLowerCase)
        .map(name => sql"$name")
        .commas
    fr"""SELECT DISTINCT ta.activity_id
      FROM $activityTagT ta
      INNER JOIN $tagT tag ON ta.tag_id = tag.id
      WHERE lower(tag.name) in ($names)"""

  def activitiesWithEitherTagLike(tags: NonEmptyList[TagName]): Fragment =
    val names =
      tags.toList
        .map(wildcardEnd)
        .map(name => sql"tag.name ilike $name")
        .foldSmash1(sql"(", sql" OR ", sql")")
    fr"""SELECT DISTINCT ta.activity_id
       FROM $activityTagT ta
       INNER JOIN $tagT tag ON ta.tag_id = tag.id
       WHERE $names"""

  def activitiesWithAllTags(tags: NonEmptyList[TagName]): Fragment =
    val inner = tags.toList
      .map(_.toLowerCase)
      .map(name => sql"""SELECT DISTINCT ta.activity_id
            FROM $activityTagT ta
            INNER JOIN $tagT tag on ta.tag_id = tag.id
            WHERE lower(tag.name) = $name""")
    inner.foldSmash1(Fragment.empty, Fragment.const(" INTERSECT "), Fragment.empty)

  def activitiesWithAllTagsLike(tags: NonEmptyList[TagName]): Fragment =
    val inner = tags.toList
      .map(wildcardEnd)
      .map(name => sql"""SELECT DISTINCT ta.activity_id
              FROM $activityTagT ta
              INNER JOIN $tagT tag on ta.tag_id = tag.id
              WHERE tag.name ilike $name""")
    inner.foldSmash1(Fragment.empty, Fragment.const(" INTERSECT "), Fragment.empty)

  def activitiesWithAllTagIds(tags: NonEmptyList[TagId]): Fragment =
    val inner = tags.toList
      .map(id => sql"""SELECT DISTINCT ta.activity_id
              FROM $activityTagT ta
              WHERE ta.tag_id = $id""")
    inner.foldSmash1(Fragment.empty, Fragment.const(" INTERSECT "), Fragment.empty)
