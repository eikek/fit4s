package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{TagId, TagName}
import fs2.Stream
import DoobieMeta._

final case class TagRecord(id: TagId, name: TagName)

object TagRecord {
  private val table = fr"tag"

  def insert(name: TagName): ConnectionIO[TagRecord] =
    fr"INSERT INTO $table (name) VALUES ($name)".update
      .withUniqueGeneratedKeys[TagId]("id")
      .map(id => TagRecord(id, name))

  def find(name: TagName): ConnectionIO[Option[TagRecord]] =
    fr"SELECT id, name FROM $table WHERE name like $name"
      .query[TagRecord]
      .option

  def exists(name: TagName): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE name = $name"
      .query[Int]
      .unique
      .map(_ > 0)

  def listAll: Stream[ConnectionIO, TagRecord] =
    fr"SELECT (id, name) FROM $table ORDER BY name"
      .query[TagRecord]
      .streamWithChunkSize(100)
}
