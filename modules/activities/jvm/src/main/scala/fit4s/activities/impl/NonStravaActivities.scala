package fit4s.activities.impl

import java.time.Instant

import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.Path

import fit4s.activities.StravaSupport.ActivityData
import fit4s.activities.data.{ActivityId, ActivityQuery, UnlinkedStravaStats}
import fit4s.activities.records.*
import fit4s.activities.records.DoobieImplicits.*
import fit4s.data.FileId

import doobie.*
import doobie.implicits.*

object NonStravaActivities:

  def stats(query: ActivityQuery): ConnectionIO[Option[UnlinkedStravaStats]] =
    val selected = ActivityQueryBuilder.activityIdFragment(query.condition)
    val limits = query.page.asFragment

    sql"""SELECT MIN(start_time), MAX(start_time), COUNT(DISTINCT activity_id)
          FROM ${RActivitySession.table}
          WHERE activity_id NOT IN (SELECT activity_id FROM ${RActivityStrava.table})
            AND activity_id IN ($selected)
          $limits
          """
      .query[(Option[Instant], Option[Instant], Int)]
      .map { case (start, end, count) =>
        (start, end).mapN(UnlinkedStravaStats(_, _, count))
      }
      .unique

  def list(query: ActivityQuery): Stream[ConnectionIO, ActivityData] =
    val selected = ActivityQueryBuilder.activityIdFragment(query.condition)
    val limits = query.page.asFragment

    sql"""SELECT a.id, a.file_id, a.name, a.notes, l.location, a.path
          FROM ${RActivity.table} a
          INNER JOIN ${RActivityLocation.table} l on a.location_id = l.id
          WHERE a.id NOT IN (SELECT activity_id FROM ${RActivityStrava.table})
                AND a.id IN ($selected)
          $limits
         """
      .query[(ActivityId, FileId, String, Option[String], Path, String)]
      .stream
      .evalMap { case (aId, fId, name, notes, loc, path) =>
        ActivityQueryBuilder
          .tagsForActivity(aId)
          .map(tags => ActivityData(aId, fId, name, notes, loc, path, tags.toSet))
      }
