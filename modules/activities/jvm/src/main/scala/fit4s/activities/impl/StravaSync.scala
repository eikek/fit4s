package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.data.OptionT
import cats.effect.*
import cats.syntax.all.*
import fs2.Chunk

import fit4s.activities.StravaExternalId
import fit4s.activities.StravaSupport.LinkResult
import fit4s.activities.data.*
import fit4s.activities.records.*
import fit4s.profile.types.Sport
import fit4s.strava.data.{StravaActivity, StravaActivityId, StravaGear}

import doobie.*
import doobie.syntax.all.*

final class StravaSync[F[_]: Async](
    xa: Transactor[F],
    findGear: String => F[Option[StravaGear]]
):
  private val logger = scribe.cats.effect[F]

  private val existed = LinkResult.Success.empty.copy(existed = 1)
  private val linked = LinkResult.Success.empty.copy(linked = 1)
  private val notFound = LinkResult.Success.empty.copy(notFound = 1)

  private def ambiguous(strava: StravaActivityId, activities: NonEmptyList[ActivityId]) =
    LinkResult.Success.empty
      .copy(ambiguous = List(LinkResult.Ambiguous(strava, activities)))

  private def alreadyLinked(activityId: ActivityId, stravaId: StravaActivityId) =
    LinkResult.Success.empty
      .copy(alreadyLinked = List(LinkResult.AlreadyLinked(activityId, stravaId)))

  def sync(
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(stravaActivities: Chunk[StravaActivity]): F[LinkResult.Success] =
    stravaActivities
      .traverse(syncSingle(bikeTagPrefix, shoeTagPrefix, commuteTag))
      .map(_.fold)

  def syncSingle(
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(sa: StravaActivity): F[LinkResult.Success] =
    def doLink(locAct: ActivityId) =
      for {
        _ <- logger.debug(s"Link local activity ${locAct.id} to ${sa.id}")
        _ <- link(locAct, sa.id)
        _ <- linkGearTags(
          locAct,
          sa,
          bikeTagPrefix,
          shoeTagPrefix,
          commuteTag
        )
      } yield linked

    RActivityStrava
      .findByStravaId(sa.id)
      .transact(xa)
      .flatMap:
        case Some(r) =>
          logger
            .debug(s"Activity ${sa.id} already linked to ${r.activityId}")
            .as(existed)

        case None =>
          val fromExternalId =
            for {
              saExtId <- OptionT
                .fromOption[F](StravaExternalId.fromString(sa.external_id).toOption)
              _ <- OptionT.liftF(
                logger
                  .debug(
                    s"Search local activity from strava external id: ${sa.external_id}"
                  )
              )
              act <- OptionT(RActivity.findByStravaExternalId(saExtId).transact(xa))
              res <- OptionT.liftF(doLink(act.id))
            } yield res

          fromExternalId.getOrElseF:
            logger.debug(
              s"Look for local activity using strava start_date: ${sa.start_date}"
            ) *>
              RActivitySession
                .findByStartTime(sa.start_date, 60, sa.fitSport, None)
                .transact(xa)
                .map(_.groupBy(_.activityId).toList)
                .map(NonEmptyList.fromList)
                .flatMap:
                  case Some(NonEmptyList((head, _), Nil)) =>
                    doLink(head)

                  case None =>
                    logger
                      .debug(s"No local activity found for strava ${sa.id}")
                      .as(notFound)

                  case Some(tooMany) =>
                    logger
                      .debug(
                        s"Too many ${tooMany.size} local activities found for strava ${sa.id}"
                      )
                      .as(ambiguous(sa.id, tooMany.map(_._1)))

  def link(local: ActivityId, strava: StravaActivityId): F[LinkResult.Success] =
    RActivityStrava
      .findByActivityId(local)
      .transact(xa)
      .flatMap:
        case Some(otherStravaId) => alreadyLinked(local, otherStravaId).pure[F]
        case None =>
          RActivityStrava
            .insert(local, strava)
            .transact(xa)
            .as(linked)

  def linkGearTags(
      local: ActivityId,
      sa: StravaActivity,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): F[Unit] =
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

    sa.gear_id
      .flatTraverse(findGear)
      .flatMap:
        case None => ().pure[F]
        case Some(gear) =>
          val tag = sa.fitSport match
            case Some(s) if s == Sport.Cycling => bikeTag(gear.name)
            case Some(s) if s == Sport.Running => shoeTag(gear.name)
            case _                             => None

          val tags = List(tag, tag1).flatten
          RTag
            .getOrCreate(tags)
            .map(NonEmptyList.fromList)
            .transact(xa)
            .flatMap:
              case None => ().pure[F]
              case Some(nel) =>
                (RActivityTag.remove(local, nel.map(_.id)) *>
                  RActivityTag.insert1(local, nel.map(_.id))).transact(xa).void
