package fit4s.activities

import java.time.{Instant, ZoneId}

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.*
import cats.kernel.Monoid
import cats.syntax.all.*
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.io.net.Network

import fit4s.activities.StravaSupport.{LinkResult, UploadCallback}
import fit4s.activities.data.*
import fit4s.activities.impl.*
import fit4s.activities.records.RStravaToken
import fit4s.data.FileId
import fit4s.geocode.ReverseLookup
import fit4s.strava.data.*
import fit4s.strava.{StravaAppCredentials, StravaClient, StravaClientConfig}

import doobie.util.transactor.Transactor
import org.http4s.client.Client

trait StravaSupport[F[_]]:
  def unlink(aq: ActivityQuery): F[Int]

  def initOAuth(
      cfg: StravaAppCredentials,
      timeout: FiniteDuration
  ): F[Option[RStravaToken]]

  def deleteTokens: F[Int]

  def listActivities(
      cfg: StravaAppCredentials,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]]

  def listAllActivities(
      cfg: StravaAppCredentials,
      after: Instant,
      before: Instant,
      chunkSize: Int
  ): Stream[F, StravaActivity]

  def findGear(cfg: StravaAppCredentials, gearId: String): F[Option[StravaGear]]

  def getAthlete(cfg: StravaAppCredentials): F[StravaAthlete]

  def linkActivities(
      cfg: StravaAppCredentials,
      query: ActivityQuery,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): F[LinkResult]

  def getUnlinkedActivities(query: ActivityQuery): F[Option[UnlinkedStravaStats]]

  /** Before this, the `linkActivities` method should run to avoid uploading duplicates or
    * make sure to select correct activities using the query.
    */
  def uploadActivities(
      cfg: StravaAppCredentials,
      query: ActivityQuery,
      withNotes: Boolean,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      processingTimeout: FiniteDuration,
      uploadCallback: UploadCallback[F]
  ): Stream[F, Either[StravaUploadError, StravaActivityId]]

  def loadExport(
      stravaExport: Path,
      zoneId: ZoneId,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

object StravaSupport:

  def apply[F[_]: Async: Network: Files: Compression](
      stravaCfg: StravaClientConfig,
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F],
      client: Client[F]
  ): F[StravaSupport[F]] =
    for {
      lookup <- GeoLookupDb(reverseLookup, xa)
      stravaClient <- StravaClient(stravaCfg, client)
      strava = new StravaImpl[F](
        stravaClient,
        xa,
        new GeoPlaceAttach[F](xa, lookup)
      )
    } yield strava

  sealed trait LinkResult extends Product:
    final def widen: LinkResult = this
    def fold[A](success: LinkResult.Success => A, notFound: => A): A
  object LinkResult:
    case class AlreadyLinked(activityId: ActivityId, stravaId: StravaActivityId)
    case class Ambiguous(stravaId: StravaActivityId, activities: NonEmptyList[ActivityId])

    case object NoActivitiesFound extends LinkResult:
      def fold[A](success: LinkResult.Success => A, notFound: => A): A = notFound
    case class Success(
        linked: Int,
        existed: Int,
        notFound: Int,
        ambiguous: List[Ambiguous],
        alreadyLinked: List[AlreadyLinked]
    ) extends LinkResult:
      def fold[A](success: LinkResult.Success => A, notFound: => A) = success(this)

      def +(other: Success): Success = Success(
        linked + other.linked,
        existed + other.existed,
        notFound + other.notFound,
        (ambiguous ::: other.ambiguous).distinct,
        (alreadyLinked ::: other.alreadyLinked).distinct
      )

      lazy val allCount =
        linked + existed + notFound + ambiguous.size + alreadyLinked.size
    object Success:
      val empty: Success = Success(0, 0, 0, Nil, Nil)
      implicit val monoid: Monoid[Success] =
        Monoid.instance(empty, _ + _)

  trait UploadCallback[F[_]]:
    def onPollingStrava(waitedSoFar: FiniteDuration, attempts: Int): F[Unit]
    def onFile(activity: ActivityData): F[Unit]
  object UploadCallback:
    def noop[F[_]: Applicative] = new UploadCallback[F]:
      def onPollingStrava(waitedSoFar: FiniteDuration, attempts: Int) = ().pure[F]
      override def onFile(activity: ActivityData) = ().pure[F]

  final case class ActivityData(
      id: ActivityId,
      fileId: FileId,
      name: String,
      notes: Option[String],
      location: Path,
      file: String,
      tags: Set[Tag]
  ):
    def activityFile: Path = location / file
