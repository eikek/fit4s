package fit4s.activities

import cats.data.NonEmptyList
import cats.effect._
import cats.kernel.Monoid
import cats.syntax.all._
import doobie.util.transactor.Transactor
import fit4s.activities.StravaSupport.PublishResult
import fit4s.activities.data._
import fit4s.activities.impl._
import fit4s.activities.records.RStravaToken
import fit4s.geocode.ReverseLookup
import fs2.io.file.Path
import fs2.{Chunk, Stream}
import org.http4s.client.Client

import java.time.{Instant, ZoneId}
import scala.concurrent.duration.FiniteDuration

trait StravaSupport[F[_]] {
  def unlink(aq: ActivityQuery): F[Int]

  def initOAuth(cfg: StravaAuthConfig, timeout: FiniteDuration): F[Option[RStravaToken]]

  def deleteTokens: F[Int]

  def listActivities(
      cfg: StravaAuthConfig,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]]

  final def listAllActivities(
      cfg: StravaAuthConfig,
      after: Instant,
      before: Instant,
      chunkSize: Int
  ): Stream[F, StravaActivity] =
    Stream
      .iterate(1)(_ + 1)
      .evalMap(page => listActivities(cfg, after, before, page, chunkSize))
      // strava docs say that pages could be less than per_page, so check for empty result
      .takeWhile(_.nonEmpty)
      .flatMap(page => Stream.chunk(Chunk.seq(page)))

  def findGear(cfg: StravaAuthConfig, gearId: String): F[Option[StravaGear]]

  def getAthlete(cfg: StravaAuthConfig): F[StravaAthlete]

  def linkActivities(
      cfg: StravaAuthConfig,
      query: ActivityQuery,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): F[PublishResult]

  def getUnlinkedActivities(query: ActivityQuery): F[Option[UnlinkedStravaStats]]

  /** Before this, the `linkActivities` method should run to avoid uploading duplicates or
    * make sure to select correct activities using the query.
    */
  def uploadActivities(
      cfg: StravaAuthConfig,
      query: ActivityQuery,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  ): Stream[F, StravaExternalId]

  def loadExport(
      stravaExport: Path,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]
}

object StravaSupport {

  def apply[F[_]: Async](
      zoneId: ZoneId,
      stravaCfg: StravaConfig,
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F],
      client: Client[F]
  ): F[StravaSupport[F]] =
    for {
      lookup <- GeoLookupDb(reverseLookup, xa)
      oauth = new StravaOAuth[F](stravaCfg, client, xa)
      gearCache <- Cache.memory[F, String, StravaGear](stravaCfg.gearCacheSize)
      strava = new StravaImpl[F](
        zoneId,
        stravaCfg,
        client,
        oauth,
        xa,
        new GeoPlaceAttach[F](xa, lookup),
        gearCache
      )
    } yield strava

  sealed trait PublishResult extends Product {
    final def widen: PublishResult = this
    def fold[A](success: PublishResult.Success => A, notFound: => A): A
  }
  object PublishResult {
    case class AlreadyLinked(activityId: ActivityId, stravaId: StravaExternalId)
    case class Ambiguous(stravaId: StravaExternalId, activities: NonEmptyList[ActivityId])

    case object NoActivitiesFound extends PublishResult {
      def fold[A](success: PublishResult.Success => A, notFound: => A): A = notFound
    }
    case class Success(
        uploads: Int,
        linked: Int,
        existed: Int,
        notFound: Int,
        ambiguous: List[Ambiguous],
        alreadyLinked: List[AlreadyLinked]
    ) extends PublishResult {
      def fold[A](success: PublishResult.Success => A, notFound: => A) = success(this)

      def +(other: Success): Success = Success(
        uploads + other.uploads,
        linked + other.linked,
        existed + other.existed,
        notFound + other.notFound,
        (ambiguous ::: other.ambiguous).distinct,
        (alreadyLinked ::: other.alreadyLinked).distinct
      )

      lazy val allCount =
        uploads + linked + existed + notFound + ambiguous.size + alreadyLinked.size
    }
    object Success {
      val empty: Success = Success(0, 0, 0, 0, Nil, Nil)
      implicit val monoid: Monoid[Success] =
        Monoid.instance(empty, _ + _)

    }
  }
}
