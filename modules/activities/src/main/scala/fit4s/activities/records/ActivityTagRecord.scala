package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityId, ActivityTagId, TagId}
import DoobieMeta._
import cats.data.NonEmptyList
import cats.effect.kernel.Sync
import fit4s.activities.ActivityQuery
import fit4s.activities.impl.ActivityQueryBuilder

final case class ActivityTagRecord(
    id: ActivityTagId,
    activityId: ActivityId,
    tagId: TagId
)

object ActivityTagRecord {
  private[activities] val table = fr"activity_tag"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("activity_id"), c("tag_id"))
  }

  private val colsNoId =
    columnList(None).tail.foldSmash1(Fragment.empty, sql",", Fragment.empty)

  def insert(activityId: ActivityId, tagId: NonEmptyList[TagId]): ConnectionIO[Int] = {
    val values = tagId.toList
      .map(id => fr"($activityId, $id)")
      .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

    fr"INSERT INTO $table ($colsNoId) VALUES $values".update.run
  }

  def remove(activityId: ActivityId, tags: NonEmptyList[TagId]): ConnectionIO[Int] = {
    val tagIds = tags.map(id => sql"$id").foldSmash1(sql"(", sql", ", sql")")
    fr"DELETE FROM $table WHERE activity_id = $activityId AND tag_id in $tagIds".update.run
  }

  def removeTags(tags: List[TagId]): ConnectionIO[Int] =
    if (tags.isEmpty) Sync[ConnectionIO].pure(0)
    else {
      val tagIds = tags.map(id => sql"$id").foldSmash1(sql"(", sql", ", sql")")
      sql"DELETE FROM $table WHERE tag_id in $tagIds".update.run
    }

  def insertAll(actQuery: Option[ActivityQuery.Condition], tags: NonEmptyList[TagId]) = {
    val actSql = ActivityQueryBuilder.activityIdFragment(actQuery)
    val tids = tags.toList
      .map(id => sql"($id)")
      .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

    sql"""
      INSERT INTO $table ($colsNoId)
        WITH actids as ($actSql)
        SELECT * FROM actids
        CROSS JOIN (VALUES $tids) as t(tid)""".update.run
  }
}
