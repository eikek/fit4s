package fit4s.activities.records

import doobie._
import doobie.implicits._
import fs2.Stream

final case class TagRecord(id: Long, name: String)

object TagRecord {
  private val table = fr"tag"

  def insert(name: String): ConnectionIO[TagRecord] =
    fr"INSERT INTO $table (name) VALUES ($name)".update
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => TagRecord(id, name))

  def find(name: String): ConnectionIO[Option[TagRecord]] =
    fr"SELECT id, name FROM $table WHERE name like $name"
      .query[TagRecord]
      .option

  def exists(name: String): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE name = $name"
      .query[Int]
      .unique
      .map(_ > 0)

  def listAll: Stream[ConnectionIO, TagRecord] =
    fr"SELECT (id, name) FROM $table ORDER BY name"
      .query[TagRecord]
      .streamWithChunkSize(100)
}
