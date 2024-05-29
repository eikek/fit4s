package fit4s.activities.records

import java.time.{Duration, Instant}

import scala.collection.immutable.Seq

import cats.effect.kernel.Clock
import cats.syntax.all.*
import fs2.Stream

import fit4s.activities.records.DoobieImplicits.*
import fit4s.strava.data.*

import doobie.*
import doobie.implicits.*

final case class RStravaToken(
    id: StravaTokenId,
    tokenType: String,
    accessToken: StravaAccessToken,
    expiresAt: Instant,
    expiresIn: Duration,
    refreshToken: StravaRefreshToken,
    scope: StravaScope,
    createdAt: Instant
):
  def toTokenAndScope: TokenAndScope =
    TokenAndScope(
      StravaTokenResponse(
        token_type = tokenType,
        access_token = accessToken,
        expires_at = expiresAt,
        expires_in = expiresIn,
        refresh_token = refreshToken
      ),
      scope
    )

object RStravaToken:
  def fromResponse(tr: TokenAndScope): ConnectionIO[RStravaToken] =
    fromResponse(tr.tokenResponse, tr.scope)

  def fromResponse(
      r: StravaTokenResponse,
      scope: StravaScope
  ): ConnectionIO[RStravaToken] =
    Clock[ConnectionIO].realTimeInstant.map(now =>
      RStravaToken(
        id = StravaTokenId(-1),
        tokenType = r.token_type,
        accessToken = r.access_token,
        expiresAt = r.expires_at,
        expiresIn = r.expires_in,
        refreshToken = r.refresh_token,
        scope = scope,
        createdAt = now
      )
    )

  private[activities] val table = fr"strava_token"

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
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

  private val colsNoId = columnList(None).tail.commas
  private val cols = columnList(None).commas

  def streamAll: Stream[ConnectionIO, RStravaToken] =
    sql"SELECT $cols FROM $table".query[RStravaToken].streamWithChunkSize(100)

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[RStravaToken]): ConnectionIO[Int] =
      Update[RStravaToken](sql).updateMany(tags)

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
      last <-
        sql"SELECT id FROM $table WHERE created_at = (SELECT MAX(created_at) FROM $table)"
          .query[StravaTokenId]
          .option
      n <- last.fold(0.pure[ConnectionIO])(id =>
        sql"DELETE FROM $table WHERE expires_at < $now AND id <> $id".update.run
      )
    } yield n

  def deleteAll: ConnectionIO[Int] =
    sql"DELETE FROM $table".update.run
