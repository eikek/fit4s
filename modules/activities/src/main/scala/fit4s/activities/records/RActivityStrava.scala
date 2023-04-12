package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data._

final case class RActivityStrava(
    id: ActivityStravaId,
    activityId: ActivityId,
    stravaId: StravaExternalId
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

  // private val columnsWithId =
  //  columnList(None).foldSmash1(Fragment.empty, sql",", Fragment.empty)

  private val columnsNoId =
    columnList(None).tail.foldSmash1(Fragment.empty, sql",", Fragment.empty)

  def insert(r: RActivityStrava): ConnectionIO[Int] =
    insert(r.activityId, r.stravaId)

  def insert(activityId: ActivityId, stravaId: StravaExternalId): ConnectionIO[Int] =
    sql"INSERT INTO $table ($columnsNoId) VALUES (${activityId}, ${stravaId})".update.run

  def removeForActivity(activityId: ActivityId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE activity_id = $activityId".update.run

  def removeForStravaId(stravaId: StravaExternalId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE strava_id = $stravaId".update.run

  def find(id: ActivityId): ConnectionIO[Option[StravaExternalId]] =
    sql"SELECT strava_id FROM $table WHERE activity_id = $id"
      .query[StravaExternalId]
      .option
}
