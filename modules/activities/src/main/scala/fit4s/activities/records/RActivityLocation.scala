package fit4s.activities.records

import cats.effect._
import cats.syntax.all._
import fs2.io.file.Path
import doobie._
import doobie.implicits._
import fs2.{Chunk, Stream}
import DoobieMeta._

import fit4s.activities.data.LocationId

final case class RActivityLocation(id: LocationId, location: Path)

object RActivityLocation {
  private[activities] val table = fr"activity_location"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("location"))
  }

  def getOrCreateLocations(dir: List[Path]): ConnectionIO[Map[Path, LocationId]] =
    dir
      .traverse { p =>
        find(p).flatMap {
          case Some(r) => Sync[ConnectionIO].pure(p -> r.id)
          case None    => insert(p).map(p -> _.id)
        }
      }
      .map(_.toMap)

  def insert(location: Path): ConnectionIO[RActivityLocation] =
    fr"INSERT INTO $table (location) VALUES ($location)".update
      .withUniqueGeneratedKeys[LocationId]("id")
      .map(id => RActivityLocation(id, location))

  def find(location: Path): ConnectionIO[Option[RActivityLocation]] =
    fr"SELECT id, location FROM $table WHERE location = $location"
      .query[RActivityLocation]
      .option

  def exists(location: Path): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE location = $location"
      .query[Int]
      .unique
      .map(_ > 0)

  def insertAll(
      records: Chunk[Path]
  ): Stream[ConnectionIO, RActivityLocation] = {
    val sql = s"INSERT INTO ${table.internals.sql} (location) VALUES (?)"
    Update[Path](sql)
      .updateManyWithGeneratedKeys[RActivityLocation]("id", "location")
      .apply[Chunk](records)
  }

  def listAll: ConnectionIO[Vector[RActivityLocation]] =
    fr"SELECT id, location FROM $table ORDER BY location"
      .query[RActivityLocation]
      .to[Vector]
}
