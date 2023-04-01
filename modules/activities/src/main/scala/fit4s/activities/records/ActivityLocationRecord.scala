package fit4s.activities.records

import cats.effect._
import cats.syntax.all._
import fs2.io.file.Path
import doobie._
import doobie.implicits._
import fs2.{Chunk, Stream}
import DoobieMeta._

import fit4s.activities.data.LocationId

final case class ActivityLocationRecord(id: LocationId, location: Path)

object ActivityLocationRecord {
  private[activities] val table = fr"activity_location"

  def getOrCreateLocations(dir: List[Path]): ConnectionIO[Map[Path, LocationId]] =
    dir
      .traverse { p =>
        find(p).flatMap {
          case Some(r) => Sync[ConnectionIO].pure(p -> r.id)
          case None    => insert(p).map(p -> _.id)
        }
      }
      .map(_.toMap)

  def insert(location: Path): ConnectionIO[ActivityLocationRecord] =
    fr"INSERT INTO $table (location) VALUES ($location)".update
      .withUniqueGeneratedKeys[LocationId]("id")
      .map(id => ActivityLocationRecord(id, location))

  def find(location: Path): ConnectionIO[Option[ActivityLocationRecord]] =
    fr"SELECT id, location FROM $table WHERE location = $location"
      .query[ActivityLocationRecord]
      .option

  def exists(location: Path): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE location = $location"
      .query[Int]
      .unique
      .map(_ > 0)

  def insertAll(
      records: Chunk[Path]
  ): Stream[ConnectionIO, ActivityLocationRecord] = {
    val sql = s"INSERT INTO ${table.internals.sql} (location) VALUES (?)"
    Update[Path](sql)
      .updateManyWithGeneratedKeys[ActivityLocationRecord]("id", "location")
      .apply[Chunk](records)
  }

  def listAll: Stream[ConnectionIO, ActivityLocationRecord] =
    fr"SELECT id, location FROM $table ORDER BY location"
      .query[ActivityLocationRecord]
      .streamWithChunkSize(100)
}
