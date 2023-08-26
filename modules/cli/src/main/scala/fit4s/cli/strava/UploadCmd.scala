package fit4s.cli.strava

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.StravaSupport.UploadCallback
import fit4s.activities.data.QueryCondition.TagAnyMatch
import fit4s.activities.data.*
import fit4s.activities.{ActivityLog, StravaSupport}
import fit4s.cli.FormatDefinition.*
import fit4s.cli.*
import fit4s.strava.StravaAppCredentials
import fit4s.strava.data.{StravaActivityId, StravaUploadError}

import com.monovore.decline.Opts

object UploadCmd extends SharedOpts {

  final case class Options(
      condition: ActivitySelection,
      page: Page,
      withNotes: Boolean,
      noStravaTag: TagName,
      bikeTagPrefix: TagName,
      shoeTagPrefix: TagName,
      commuteTag: TagName,
      processTimeout: FiniteDuration
  )

  val opts: Opts[Options] = {
    val withNotes = Opts
      .flag(
        "with-notes",
        "Send activity notes as public description when uploading to strava"
      )
      .orFalse

    val processTimeout = Opts
      .option[FiniteDuration](
        "timeout",
        "The timeout in seconds for waiting for strava to return an activity id. Applies to each upload."
      )
      .withDefault(120.seconds)

    (
      activitySelectionOps,
      pageOpts,
      withNotes,
      noStravaTag,
      bikeTagPrefix,
      shoeTagPrefix,
      commuteTag,
      processTimeout
    ).mapN(Options.apply)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      for {
        cfg <- resolveStravaAuth(cliConfig)

        currentTime <- Clock[IO].realTimeInstant

        query_ <- ActivitySelection
          .makeCondition(opts.condition, cliConfig.timezone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        cond = amendNoStravaTag(query_, opts.noStravaTag)
        query = ActivityQuery(cond, opts.page)

        stats <- log.strava.getUnlinkedActivities(query)

        _ <- stats match {
          case Some(_) =>
            linkAndUpload(log, cfg, query, opts)
          case None =>
            IO.println("No activities found needed to sync from strava.")
        }
      } yield ExitCode.Success
    }

  def linkAndUpload(
      log: ActivityLog[IO],
      authCfg: StravaAppCredentials,
      query: ActivityQuery,
      opts: Options
  ) =
    for {
      linkResult <- log.strava.linkActivities(
        authCfg,
        query,
        opts.bikeTagPrefix.some,
        opts.shoeTagPrefix.some,
        opts.commuteTag.some
      )
      unlinkedLeft <-
        linkResult.fold(
          _ =>
            log.strava
              .getUnlinkedActivities(query)
              .map(_.map(_.count).getOrElse(0)),
          0.pure[IO]
        )
      linked = linkResult.fold(_.linked, 0)

      _ <- unlinkedLeft match {
        case 0 =>
          IO.println(s"Linked $linked activities, no more left to upload.")
        case n =>
          IO.println(s"Linked $linked activities. Uploading $n others") *>
            log.strava
              .uploadActivities(
                authCfg,
                query,
                opts.withNotes,
                opts.bikeTagPrefix.some,
                opts.shoeTagPrefix.some,
                opts.commuteTag.some,
                opts.processTimeout,
                callback
              )
              .evalMap(printResult)
              .compile
              .drain
      }
    } yield ()

  def callback: UploadCallback[IO] =
    new UploadCallback[IO] {
      def onPollingStrava(waitedSoFar: FiniteDuration, attempts: Int) =
        IO.println(
          s" :: Waiting for activity id ($attempts attempts, ${waitedSoFar.toSeconds}s) ..."
        )

      def onFile(activity: StravaSupport.ActivityData) =
        IO.println(s"Uploading activity '${activity.name}: ${activity.activityFile}'")
    }

  def printResult(r: Either[StravaUploadError, StravaActivityId]): IO[Unit] =
    r match {
      case Right(id) => IO.println(s"Created strava activity ${id.id}")
      case Left(err) =>
        IO.println(s"Error uploading an activity: $err".in(Styles.error))
    }

  def amendNoStravaTag(cond: Option[QueryCondition], tag: TagName) = {
    val excludeTag = QueryCondition.Not(TagAnyMatch(NonEmptyList.of(tag)))
    cond match {
      case Some(c) =>
        QueryCondition.And(NonEmptyList.of(c, excludeTag)).some
      case None => excludeTag.some
    }
  }
}
