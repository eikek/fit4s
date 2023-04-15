package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{StravaScope, StravaTokenId, StravaTokenResponse}
import DoobieImplicits._
import cats.effect.kernel.Clock

import java.time.{Duration, Instant}

final case class RStravaToken(
    id: StravaTokenId,
    tokenType: String,
    accessToken: String,
    expiresAt: Instant,
    expiresIn: Duration,
    refreshToken: String,
    scope: StravaScope,
    createdAt: Instant
)

object RStravaToken {

  def fromResponse(
      r: StravaTokenResponse,
      scope: StravaScope
  ): ConnectionIO[RStravaToken] =
    Clock[ConnectionIO].realTimeInstant.map(now =>
      RStravaToken(
        id = StravaTokenId(-1),
        tokenType = r.token_type,
        accessToken = r.access_token,
        expiresAt = r.expiresAt,
        expiresIn = r.expiresIn,
        refreshToken = r.refresh_token,
        scope = scope,
        createdAt = now
      )
    )

  private[activities] val table = fr"strava_token"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))

    List(
      c("id"),
      c("token_type"),
      c("access_token"),
      c("expires_at"),
      c("expires_in"),
      c("refresh_token"),
      c("scope"),
      c("created_at")
    )
  }

  private val colsNoId = columnList(None).tail.commas
  private val cols = columnList(None).commas

  def insert(r: RStravaToken): ConnectionIO[RStravaToken] =
    (sql"INSERT INTO $table ($colsNoId) VALUES (${r.tokenType}, ${r.accessToken}, " ++
      sql"${r.expiresAt}, ${r.expiresIn}, ${r.refreshToken}, ${r.scope}, ${r.createdAt})").update
      .withUniqueGeneratedKeys[StravaTokenId]("id")
      .map(id => r.copy(id = id))

  def findCurrent: ConnectionIO[Option[RStravaToken]] =
    for {
      now <- Clock[ConnectionIO].realTimeInstant
      tok <-
        sql"SELECT $cols FROM $table WHERE expires_at > $now ORDER BY expired_at DESC LIMIT 1 "
          .query[RStravaToken]
          .option
    } yield tok

  def findLatest: ConnectionIO[Option[RStravaToken]] =
    sql"SELECT $cols FROM $table WHERE created_at = (SELECT MAX(created_at) FROM $table)"
      .query[RStravaToken]
      .option

  def deleteExpired: ConnectionIO[Int] =
    for {
      now <- Clock[ConnectionIO].realTimeInstant
      n <- sql"DELETE FROM $table WHERE expired_at < $now".update.run
    } yield n

  def deleteAll: ConnectionIO[Int] =
    sql"DELETE FROM $table".update.run
}
