package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.StravaSupport.PublishResult
import fit4s.activities.data._
import fit4s.activities.records.{RActivitySession, RActivityStrava, RActivityTag, RTag}
import fit4s.profile.types.Sport
import fit4s.strava.data.{StravaActivity, StravaActivityId, StravaGear}
import fs2.Chunk

final class StravaSync[F[_]: Async](
    xa: Transactor[F],
    findGear: String => F[Option[StravaGear]]
) {
  private[this] val logger = scribe.cats.effect[F]

  private val existed = PublishResult.Success.empty.copy(existed = 1)
  private val linked = PublishResult.Success.empty.copy(linked = 1)
  private val notFound = PublishResult.Success.empty.copy(notFound = 1)

  private def ambiguous(strava: StravaActivityId, activities: NonEmptyList[ActivityId]) =
    PublishResult.Success.empty
      .copy(ambiguous = List(PublishResult.Ambiguous(strava, activities)))

  private def alreadyLinked(activityId: ActivityId, stravaId: StravaActivityId) =
    PublishResult.Success.empty
      .copy(alreadyLinked = List(PublishResult.AlreadyLinked(activityId, stravaId)))

  def sync(
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(stravaActivities: Chunk[StravaActivity]): F[PublishResult.Success] =
    stravaActivities
      .traverse(syncSingle(bikeTagPrefix, shoeTagPrefix, commuteTag))
      .map(_.fold)

  def syncSingle(
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(sa: StravaActivity): F[PublishResult.Success] =
    RActivityStrava.findByStravaId(sa.id).transact(xa).flatMap {
      case Some(r) =>
        logger
          .debug(s"Activity ${sa.id} already linked to ${r.activityId}")
          .as(existed)

      case None =>
        RActivitySession
          .findByStartTime(sa.startDate, 60, sa.fitSport, None)
          .transact(xa)
          .map(_.groupBy(_.activityId).toList)
          .map(NonEmptyList.fromList)
          .flatMap {
            case Some(NonEmptyList((head, _), Nil)) =>
              for {
                _ <- logger.debug(s"Link local activity ${head.id} to ${sa.id}")
                _ <- link(head, sa.id)
                _ <- linkGearTags(
                  head,
                  sa,
                  bikeTagPrefix,
                  shoeTagPrefix,
                  commuteTag
                )
              } yield linked

            case None =>
              logger.debug(s"No local activity found for strava ${sa.id}").as(notFound)

            case Some(tooMany) =>
              logger
                .debug(
                  s"Too many ${tooMany.size} local activities found for strava ${sa.id}"
                )
                .as(ambiguous(sa.id, tooMany.map(_._1)))
          }
    }

  def link(local: ActivityId, strava: StravaActivityId): F[PublishResult.Success] =
    RActivityStrava.findByActivityId(local).transact(xa).flatMap {
      case Some(otherStravaId) => alreadyLinked(local, otherStravaId).pure[F]
      case None =>
        RActivityStrava
          .insert(local, strava)
          .transact(xa)
          .as(linked)
    }

  def linkGearTags(
      local: ActivityId,
      sa: StravaActivity,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): F[Unit] = {
    val tag1 =
      (commuteTag, Option(sa.commute).filter(identity)).mapN((tn, _) => tn)

    def bikeTag(name: String) =
      (
        bikeTagPrefix.filter(_ => sa.fitSport.contains(Sport.Cycling)),
        TagName.fromString(name).toOption
      ).mapN(_ / _)

    def shoeTag(name: String) =
      (
        shoeTagPrefix.filter(_ => sa.fitSport.contains(Sport.Running)),
        TagName.fromString(name).toOption
      ).mapN(_ / _)

    sa.gearId.flatTraverse(findGear).flatMap {
      case None => ().pure[F]
      case Some(gear) =>
        val tag = sa.fitSport match {
          case Some(s) if s == Sport.Cycling => bikeTag(gear.name)
          case Some(s) if s == Sport.Running => shoeTag(gear.name)
          case _                             => None
        }

        val tags = List(tag, tag1).flatten
        RTag
          .getOrCreate(tags)
          .map(NonEmptyList.fromList)
          .transact(xa)
          .flatMap {
            case None => ().pure[F]
            case Some(nel) =>
              (RActivityTag.remove(local, nel.map(_.id)) *>
                RActivityTag.insert1(local, nel.map(_.id))).transact(xa).void
          }
    }
  }
}
