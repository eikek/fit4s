package fit4s.activities.records

import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.data.{Page, TagId, TagName}
import fs2.Stream
import DoobieMeta._
import cats.effect.kernel.Sync

final case class RTag(id: TagId, name: TagName)

object RTag {
  val softDelete: RTag =
    RTag(TagId(-999L), TagName.unsafeFromString("System/Deleted"))

  private[activities] val table = fr"tag"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("name"))
  }

  def getOrCreate(names: List[TagName]): ConnectionIO[List[RTag]] =
    names
      .traverse { name =>
        find(name).flatMap {
          case Some(r) => Sync[ConnectionIO].pure(r)
          case None    => insert(name)
        }
      }

  def insert(name: TagName): ConnectionIO[RTag] =
    fr"INSERT INTO $table (name) VALUES ($name)".update
      .withUniqueGeneratedKeys[TagId]("id")
      .map(id => RTag(id, name))

  def find(name: TagName): ConnectionIO[Option[RTag]] =
    fr"SELECT id, name FROM $table WHERE name ilike $name"
      .query[RTag]
      .option

  def findAll(names: List[TagName]): ConnectionIO[List[RTag]] =
    if (names.isEmpty) List.empty[RTag].pure[ConnectionIO]
    else {
      val values = names
        .map(t => sql"${t.name.toLowerCase}")
        .foldSmash1(Fragment.empty, sql",", Fragment.empty)

      sql"""SELECT id, name FROM table WHERE lower(name) in ($values)"""
        .query[RTag]
        .to[List]
    }

  def exists(name: TagName): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE name = $name"
      .query[Int]
      .unique
      .map(_ > 0)

  def listAll(nameLike: Option[String], page: Page): Stream[ConnectionIO, RTag] = {
    val cond =
      nameLike.map(_.trim).filter(_.nonEmpty) match {
        case Some(like) => fr"WHERE name ilike $like"
        case None       => Fragment.empty
      }

    fr"SELECT id, name FROM $table $cond ORDER BY name LIMIT ${page.limit} OFFSET ${page.offset}"
      .query[RTag]
      .streamWithChunkSize(100)
  }

  def rename(from: TagName, to: TagName): ConnectionIO[Int] =
    sql"UPDATE $table SET name = $to WHERE name = $from".update.run

  def delete(tag: TagName): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE name = $tag".update.run
}
