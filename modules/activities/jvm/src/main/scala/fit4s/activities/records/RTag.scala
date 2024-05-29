package fit4s.activities.records

import scala.collection.immutable.Seq

import cats.effect.kernel.Sync
import cats.syntax.all.*
import fs2.Stream

import fit4s.activities.data.*
import fit4s.activities.records.DoobieImplicits.*

import doobie.*
import doobie.implicits.*

object RTag:
  val softDelete: Tag =
    Tag(TagId(-999L), TagName.unsafeFromString("System/Deleted"))

  private[activities] val table = fr"tag"

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("name"))

  def getOrCreate(names: List[TagName]): ConnectionIO[List[Tag]] =
    names
      .traverse { name =>
        find(name).flatMap:
          case Some(r) => Sync[ConnectionIO].pure(r)
          case None    => insert(name)
      }

  def insert(name: TagName): ConnectionIO[Tag] =
    fr"INSERT INTO $table (name) VALUES ($name)".update
      .withUniqueGeneratedKeys[TagId]("id")
      .map(id => Tag(id, name))

  def find(name: TagName): ConnectionIO[Option[Tag]] =
    fr"SELECT id, name FROM $table WHERE name ilike $name"
      .query[Tag]
      .option

  def findAll(names: List[TagName]): ConnectionIO[List[Tag]] =
    if (names.isEmpty) List.empty[Tag].pure[ConnectionIO]
    else
      val values = names.map(t => sql"${t.name.toLowerCase}").commas

      sql"""SELECT id, name FROM $table WHERE lower(name) in ($values)"""
        .query[Tag]
        .to[List]

  def exists(name: TagName): ConnectionIO[Boolean] =
    fr"SELECT count(id) FROM $table WHERE name = $name"
      .query[Int]
      .unique
      .map(_ > 0)

  def listAll(nameLike: Option[String], page: Page): Stream[ConnectionIO, Tag] =
    val cond =
      nameLike.map(_.trim).filter(_.nonEmpty) match
        case Some(like) => fr"WHERE name ilike $like"
        case None       => Fragment.empty

    fr"SELECT id, name FROM $table $cond ORDER BY name ${page.asFragment}"
      .query[Tag]
      .streamWithChunkSize(100)

  def streamAll: Stream[ConnectionIO, Tag] =
    sql"SELECT id, name FROM $table".query[Tag].streamWithChunkSize(100)

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[Tag]): ConnectionIO[Int] =
      Update[Tag](sql).updateMany(tags)

  def rename(from: TagName, to: TagName): ConnectionIO[Int] =
    sql"UPDATE $table SET name = $to WHERE name = $from".update.run

  def delete(tag: TagName): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE name = $tag".update.run

  def delete(tagId: TagId): ConnectionIO[Int] =
    sql"DELETE FROM $table WHERE id = $tagId".update.run
