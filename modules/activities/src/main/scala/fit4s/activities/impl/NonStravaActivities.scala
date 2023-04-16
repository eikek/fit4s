package fit4s.activities.impl

import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.ActivityQuery
import fit4s.activities.data.UnlinkedStravaStats
import fit4s.activities.records.DoobieImplicits._
import fit4s.activities.records.{RActivitySession, RActivityStrava}

import java.time.Instant

object NonStravaActivities {

  def stats(query: ActivityQuery): ConnectionIO[Option[UnlinkedStravaStats]] = {
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
  }

}
