package fit4s.activities.records

import java.time.Instant

import fit4s.activities.data.{ActivitySessionDataId, ActivitySessionId}
import fit4s.activities.records.DoobieMeta._
import fit4s.data._

import doobie._
import doobie.implicits._

final case class RActivitySessionData(
    id: ActivitySessionDataId,
    activitySessionId: ActivitySessionId,
    timestamp: Instant,
    position: Option[Position],
    altitude: Option[Distance],
    heartRate: Option[HeartRate],
    cadence: Option[Cadence],
    distance: Option[Distance],
    speed: Option[Speed],
    power: Option[Power],
    grade: Option[Grade],
    temperature: Option[Temperature],
    calories: Option[Calories]
)

object RActivitySessionData {
  private[activities] val table = fr"activity_session_data"
  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))

    List(
      c("id"),
      c("activity_session_id"),
      c("timestamp"),
      c("position_lat"),
      c("position_long"),
      c("altitude"),
      c("heartrate"),
      c("cadence"),
      c("distance"),
      c("speed"),
      c("power"),
      c("grade"),
      c("temperature"),
      c("calories")
    )
  }

  private val columnsNoId =
    columnList(None).tail.foldSmash1(Fragment.empty, fr",", Fragment.empty)

  def insert(r: RActivitySessionData): ConnectionIO[ActivitySessionDataId] =
    (sql"INSERT INTO $table ($columnsNoId) VALUES (" ++
      sql"${r.activitySessionId}, ${r.timestamp}, ${r.position.map(_.latitude)}, " ++
      sql"${r.position.map(_.longitude)}, ${r.altitude}, ${r.heartRate}, " ++
      sql"${r.cadence}, ${r.distance}, ${r.speed}, " ++
      sql"${r.power}, ${r.grade}, ${r.temperature}, ${r.calories}" ++
      sql")").update
      .withUniqueGeneratedKeys[ActivitySessionDataId]("id")

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique
}
