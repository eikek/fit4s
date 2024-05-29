package fit4s.activities.records

import scala.collection.immutable.Seq

import cats.data.NonEmptyList
import cats.effect.kernel.Sync
import fs2.Stream

import fit4s.activities.data.*
import fit4s.activities.impl.ActivityQueryBuilder
import fit4s.activities.records.DoobieImplicits.*

import doobie.*
import doobie.implicits.*

final case class RActivityTag(
    id: ActivityTagId,
    activityId: ActivityId,
    tagId: TagId
)

object RActivityTag:
  private[activities] val table = fr"activity_tag"

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("activity_id"), c("tag_id"))

  private val colsNoId = columnList(None).tail.commas
  private val colsWithId = columnList(None).commas

  def streamAll: Stream[ConnectionIO, RActivityTag] =
    sql"SELECT $colsWithId FROM $table".query[RActivityTag].streamWithChunkSize(100)

  def insert1(activityId: ActivityId, tagIds: NonEmptyList[TagId]): ConnectionIO[Int] =
    val values = tagIds.toList.map(tagId => fr"($activityId, $tagId)").commas

    fr"INSERT INTO $table ($colsNoId) VALUES $values".update.run

  def insert2(tagId: TagId, activityIds: NonEmptyList[ActivityId]): ConnectionIO[Int] =
    val values = activityIds.toList
      .map(activityId => fr"($activityId, $tagId)")
      .commas

    fr"INSERT INTO $table ($colsNoId) VALUES $values".update.run

  def remove(activityId: ActivityId, tags: NonEmptyList[TagId]): ConnectionIO[Int] =
    val tagIds = tags.map(id => sql"$id").foldSmash1(sql"(", sql", ", sql")")
    fr"DELETE FROM $table WHERE activity_id = $activityId AND tag_id in $tagIds".update.run

  def removeTags(
      actQuery: Option[QueryCondition],
      tags: List[TagId]
  ): ConnectionIO[Int] =
    if (tags.isEmpty) Sync[ConnectionIO].pure(0)
    else
      val actSql = ActivityQueryBuilder.activityIdFragment(actQuery)
      val tagIds = tags.map(id => sql"$id").commas
      sql"DELETE FROM $table WHERE tag_id in ($tagIds) AND activity_id in ($actSql)".update.run

  def insertAll(actQuery: Option[QueryCondition], tags: NonEmptyList[TagId]) =
    val actSql = ActivityQueryBuilder.activityIdFragment(actQuery)
    val tids = tags.toList.map(id => sql"($id)").commas

    sql"""
      INSERT INTO $table ($colsNoId)
        WITH actids as ($actSql)
        SELECT * FROM actids
        CROSS JOIN (VALUES $tids) as t(tid)""".update.run

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[RActivityTag]): ConnectionIO[Int] =
      Update[RActivityTag](sql).updateMany(tags)
