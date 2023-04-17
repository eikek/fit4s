package fit4s.activities.impl

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.StravaSupport.PublishResult
import fit4s.activities.data._
import fit4s.activities.impl.StravaExportExtract.ExportData
import fit4s.activities.records._
import fit4s.activities._
import fs2.Stream
import fs2.io.file.Path
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Method}

import java.time.{Instant, ZoneId}
import scala.concurrent.duration.FiniteDuration
import scala.math.Ordering.Implicits.infixOrderingOps

final class StravaImpl[F[_]: Async](
    zoneId: ZoneId,
    config: StravaConfig,
    client: Client[F],
    oauth: StravaOAuth[F],
    xa: Transactor[F],
    placeAttach: GeoPlaceAttach[F],
    gearCache: Cache[F, String, StravaGear]
) extends StravaSupport[F] {

  private[this] val logger = scribe.cats.effect[F]

  def initOAuth(
      cfg: StravaAuthConfig,
      timeout: FiniteDuration
  ): F[Option[RStravaToken]] =
    nonInteractiveOAuth(cfg).flatMap {
      case Some(t) => Option(t).pure[F]
      case None    => oauth.init(cfg, timeout)
    }

  def nonInteractiveOAuth(
      cfg: StravaAuthConfig
  ): F[Option[RStravaToken]] =
    Clock[F].realTimeInstant.flatMap { now =>
      RStravaToken.findLatest.transact(xa).flatMap {
        case Some(t) if t.expiresAt.plusSeconds(20) > now =>
          logger.debug(s"Latest token is still valid.").as(Option(t))

        case t =>
          oauth.refresh(cfg, t)
      }
    }

  def deleteTokens: F[Int] =
    RStravaToken.deleteAll.transact(xa)

  def listActivities(
      cfg: StravaAuthConfig,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    for {
      token <- nonInteractiveOAuth(cfg)
        .map(_.toRight(new Exception(s"No authentication token available.")))
        .rethrow

      uri = (config.apiUrl / "athlete" / "activities")
        .withQueryParam("before", before.getEpochSecond)
        .withQueryParam("after", after.getEpochSecond)
        .withQueryParam("page", page)
        .withQueryParam("per_page", perPage)

      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      result <- client.expect[List[StravaActivity]](
        Method.GET(uri).withHeaders(Authorization(credentials))
      )
    } yield result
  }

  def findGear(cfg: StravaAuthConfig, gearId: String): F[Option[StravaGear]] =
    gearCache.cached(findGearRaw(cfg, _))(gearId)

  def findGearRaw(cfg: StravaAuthConfig, gearId: String): F[Option[StravaGear]] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    for {
      token <- nonInteractiveOAuth(cfg)
        .map(_.toRight(new Exception(s"No authentication token available.")))
        .rethrow

      uri = config.apiUrl / "gear" / gearId

      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      result <- client.expectOption[StravaGear](
        Method.GET(uri).withHeaders(Authorization(credentials))
      )
    } yield result
  }

  def getAthlete(cfg: StravaAuthConfig): F[StravaAthlete] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    for {
      token <- nonInteractiveOAuth(cfg)
        .map(_.toRight(new Exception(s"No authentication token available.")))
        .rethrow

      uri = config.apiUrl / "athlete"

      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      result <- client.expect[StravaAthlete](
        Method.GET(uri).withHeaders(Authorization(credentials))
      )
    } yield result
  }

  def getUnlinkedActivities(query: ActivityQuery): F[Option[UnlinkedStravaStats]] =
    NonStravaActivities.stats(query).transact(xa)

  def linkActivities(
      cfg: StravaAuthConfig,
      query: ActivityQuery,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): F[PublishResult] =
    (for {
      nonStravaActs <- OptionT(
        NonStravaActivities
          .stats(query)
          .transact(xa)
      ).toRight(PublishResult.NoActivitiesFound.widen)

      stravaSync = new StravaSync[F](xa, findGear(cfg, _))

      result <- EitherT.right[PublishResult](
        listAllActivities(
          cfg,
          nonStravaActs.lowestStart.minusSeconds(60),
          nonStravaActs.recentStart.plusSeconds(60),
          150
        ).chunks
          .evalMap(stravaSync.sync(bikeTagPrefix, shoeTagPrefix, commuteTag))
          .compile
          .foldMonoid
      )

    } yield result.widen).merge

  def unlink(aq: ActivityQuery): F[Int] =
    RActivityStrava.removeAll(aq).transact(xa)

  def uploadActivities(
      cfg: StravaAuthConfig,
      query: ActivityQuery,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): Stream[F, StravaActivityId] = for {
    athlete <- Stream.eval(getAthlete(cfg))
    getToken = nonInteractiveOAuth(cfg)
      .map(_.toRight(new Exception(s"No authentication token available.")))
      .rethrow
    strava = new StravaUpload[F](client, config, getToken)
    stravaId <- NonStravaActivities
      .list(query)
      .transact(xa)
      .evalMap { activityData =>
        val commute = commuteTag.exists(ct => activityData.tags.exists(_.name === ct))
        val gearId =
          findGearInTags(bikeTagPrefix, activityData.tags, athlete.bikes)
            .orElse(findGearInTags(shoeTagPrefix, activityData.tags, athlete.shoes))
            .map(_.id)

        strava
          .uploadFit(
            activityData.id,
            activityData.location / activityData.file,
            activityData.name,
            activityData.notes,
            commute
          )
          .flatTap { stravaId =>
            RActivityStrava.insert(activityData.id, stravaId).transact(xa)
          }
          .flatTap { stravaId =>
            gearId match {
              case Some(gId) =>
                val updateData =
                  StravaUpdatableActivity.empty
                    .copy(gearId = gId.some)

                strava.updateActivity(stravaId, updateData)

              case None => ().pure[F]
            }
          }
      }
  } yield stravaId

  private def findGearInTags(
      tagPrefix: Option[TagName],
      tags: Set[RTag],
      gears: List[StravaGear]
  ): Option[StravaGear] =
    tagPrefix.flatMap { prefix =>
      tags
        .find(_.name.startsWith(prefix))
        .flatMap(tag =>
          gears.find(_.name.equalsIgnoreCase(tag.name.stripPrefix(prefix).name))
        )
    }

  def loadExport(
      stravaExport: Path,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]] =
    Stream
      .resource(
        StravaExportExtract
          .activities[F](stravaExport)
      )
      .flatMap(
        importFrom(
          stravaExport,
          bikeTagPrefix,
          shoeTagPrefix,
          commuteTag,
          tagged,
          callback
        )
      )

  private def importFrom(
      exportLocation: Path,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      moreTags: Set[TagName],
      callback: ImportCallback[F]
  )(exportData: Vector[ExportData]): Stream[F, ImportResult[ActivityId]] = {
    val bikeTags = bikeTagPrefix
      .map(makeGearTags(exportData.flatMap(_.bike)))
      .getOrElse(Set.empty)
    val shoeTags = shoeTagPrefix
      .map(makeGearTags(exportData.flatMap(_.shoe)))
      .getOrElse(Set.empty)

    val givenTags = moreTags ++ bikeTags ++ shoeTags ++ commuteTag.iterator.toSet

    val locationAndTag = for {
      loc <- RActivityLocation
        .getOrCreateLocations(List(exportLocation))
        .map(_.apply(exportLocation))
      tagIds <- RTag
        .getOrCreate(givenTags.toList)
        .map(_.map(r => r.name -> r.id).toMap)
    } yield (loc, tagIds)

    for {
      (locId, allTags) <- Stream.eval(locationAndTag.transact(xa))
      now <- Stream.eval(Clock[F].realTimeInstant)
      entry <- Stream.emits(exportData)
      _ <- Stream.eval(callback.onFile(entry.fitFile))
      entryTags = selectTagIds(allTags, bikeTagPrefix, shoeTagPrefix, commuteTag)(entry)

      cioResult <- FitFileImport.addSingle(
        entryTags,
        None,
        locId,
        entry.relativePath,
        zoneId,
        now
      )(entry.fitFile)
      result <- Stream.eval(cioResult.transact(xa))
      _ <- Stream.eval(result match {
        case ImportResult.Failure(ImportResult.FailureReason.Duplicate(id, _, _)) =>
          // associate tags anyways
          NonEmptyList
            .fromList(entryTags.toList)
            .map { nel =>
              (RActivityTag.remove(id, nel) *> RActivityTag.insert1(id, nel))
                .transact(xa)
                .void
            }
            .getOrElse(Async[F].unit) *> updateStravaMeta(id, entry)

        case ImportResult.Success(id) =>
          updateStravaMeta(id, entry)

        case _ => Async[F].unit
      })
      _ <- Stream.eval(placeAttach.applyResult(result))
    } yield result
  }

  private def updateStravaMeta(id: ActivityId, entry: ExportData): F[Unit] =
    for {
      _ <- entry.name.traverse_(n => RActivity.updateName(id, n).transact(xa))
      _ <- entry.description.traverse_(d =>
        RActivity.updateNotes(id, d.some).transact(xa)
      )
      _ <- entry.id
        .traverse_ { stravaId =>
          RActivityStrava.removeForStravaId(stravaId) *>
            RActivityStrava.insert(id, stravaId)
        }
        .transact(xa)
    } yield ()

  private def selectTagIds(
      all: Map[TagName, TagId],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(entry: ExportData): Set[TagId] = {
    val names =
      bikeTagPrefix
        .map(makeGearTags(entry.bike.iterator.toVector))
        .getOrElse(Set.empty) ++
        shoeTagPrefix
          .map(makeGearTags(entry.shoe.iterator.toVector))
          .getOrElse(Set.empty) ++
        (if (entry.commute) commuteTag.iterator.toSet else Set.empty)

    names.foldLeft(Set.empty[TagId]) { (res, t) =>
      res ++ all.get(t).iterator.toSet
    }
  }

  private def makeGearTags(tags: Vector[String])(prefix: TagName) =
    tags
      .flatMap(TagName.fromString(_).toOption)
      .map(prefix / _)
      .toSet
}
