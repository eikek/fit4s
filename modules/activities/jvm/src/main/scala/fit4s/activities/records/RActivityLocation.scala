package fit4s.activities.records

import scala.collection.immutable.Seq

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path
import fs2.{Chunk, Stream}

import fit4s.activities.data.{Location, LocationId, Page}
import fit4s.activities.records.DoobieImplicits.{*, given}

import doobie.*
import doobie.syntax.all.*

object RActivityLocation:
  private[activities] val table = fr"activity_location"

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("location"))

  def getOrCreateLocations(dir: List[Path]): ConnectionIO[Map[Path, LocationId]] =
    dir
      .traverse { p =>
        findByPath(p).flatMap:
          case Some(r) => Sync[ConnectionIO].pure(p -> r.id)
          case None    => insert(p).map(p -> _.id)
      }
      .map(_.toMap)

  def insert(location: Path): ConnectionIO[Location] =
    fr"INSERT INTO $table (location) VALUES ($location)".update
      .withUniqueGeneratedKeys[LocationId]("id")
      .map(id => Location(id, location))

  def findByPath(location: Path): ConnectionIO[Option[Location]] =
    fr"SELECT id, location FROM $table WHERE location = $location"
      .query[Location]
      .option

  def findById(id: LocationId): ConnectionIO[Option[Location]] =
    sql"SELECT id, location FROM $table WHERE id = $id"
      .query[Location]
      .option

  def setLocation(id: LocationId, path: Path): ConnectionIO[Int] =
    sql"UPDATE $table SET location = $path WHERE id = $id".update.run

  def exists(location: Path): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE location = $location"
      .query[Int]
      .unique
      .map(_ > 0)

  def delete(id: LocationId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE id = $id".update.run

  def insertAll(
      records: Chunk[Path]
  ): Stream[ConnectionIO, Location] =
    val sql = s"INSERT INTO ${table.internals.sql} (location) VALUES (?)"
    Update[Path](sql)
      .updateManyWithGeneratedKeys[Location]("id", "location")
      .apply[Chunk](records)

  def listAll: ConnectionIO[Vector[Location]] =
    fr"SELECT id, location FROM $table ORDER BY id ASC"
      .query[Location]
      .to[Vector]

  def listSream(
      contains: Option[String],
      page: Page
  ): Stream[ConnectionIO, (Location, Long)] =
    val where =
      contains.map(c => sql"WHERE loc.location ilike $c").getOrElse(Fragment.empty)

    sql"""SELECT loc.id,loc.location, count(pa.id)
          FROM $table loc
          INNER JOIN ${RActivity.table} pa ON pa.location_id = loc.id
          $where
          GROUP BY loc.id, loc.location
          ORDER BY id
          ${page.asFragment}"""
      .query[(Location, Long)]
      .streamWithChunkSize(100)

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[Location]): ConnectionIO[Int] =
      Update[Location](sql).updateMany(tags)

  def streamAll: Stream[ConnectionIO, Location] =
    sql"SELECT id,location FROM $table".query[Location].streamWithChunkSize(100)
