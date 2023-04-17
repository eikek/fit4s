package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.ActivityQuery
import fit4s.activities.data._
import fit4s.activities.impl.ActivityQueryBuilder
import fit4s.activities.records.DoobieImplicits._

final case class RActivityStrava(
    id: ActivityStravaId,
    activityId: ActivityId,
    stravaId: StravaActivityId
)

object RActivityStrava {
  private[activities] val table = fr"activity_strava"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))

    List(
      c("id"),
      c("activity_id"),
      c("strava_id")
    )
  }

  private val columnsWithId =
    columnList(None).commas

  private val columnsNoId =
    columnList(None).tail.commas

  def insert(r: RActivityStrava): ConnectionIO[Int] =
    insert(r.activityId, r.stravaId)

  def insert(activityId: ActivityId, stravaId: StravaActivityId): ConnectionIO[Int] =
    sql"INSERT INTO $table ($columnsNoId) VALUES (${activityId}, ${stravaId})".update.run

  def removeForActivity(activityId: ActivityId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE activity_id = $activityId".update.run

  def removeForStravaId(stravaId: StravaActivityId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE strava_id = $stravaId".update.run

  def removeAll(query: ActivityQuery): ConnectionIO[Int] = {
    val subq = ActivityQueryBuilder.activityIdFragment(query.condition)
    sql"DELETE FROM $table WHERE activity_id in ($subq) ${query.page.asFragment}".update.run
  }

  def findByActivityId(id: ActivityId): ConnectionIO[Option[StravaActivityId]] =
    sql"SELECT strava_id FROM $table WHERE activity_id = $id"
      .query[StravaActivityId]
      .option

  def findByStravaId(id: StravaActivityId): ConnectionIO[Option[RActivityStrava]] =
    sql"SELECT $columnsWithId FROM $table WHERE strava_id = $id "
      .query[RActivityStrava]
      .option
}
