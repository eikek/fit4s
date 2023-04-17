package fit4s.cli.strava

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityQuery.Condition
import fit4s.activities.ActivityQuery.Condition.TagAnyMatch
import fit4s.activities.data.{Page, TagName}
import fit4s.activities.{ActivityLog, ActivityQuery}
import fit4s.cli.{ActivitySelection, CliConfig, CliError, SharedOpts}
import fit4s.strava.StravaAppCredentials

object UploadCmd extends SharedOpts {

  final case class Options(
      condition: ActivitySelection,
      page: Page,
      noStravaTag: TagName,
      bikeTagPrefix: TagName,
      shoeTagPrefix: TagName,
      commuteTag: TagName
  )

  val opts: Opts[Options] =
    (
      activitySelectionOps,
      pageOpts,
      noStravaTag,
      bikeTagPrefix,
      shoeTagPrefix,
      commuteTag
    ).mapN(Options)

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
                opts.bikeTagPrefix.some,
                opts.shoeTagPrefix.some,
                opts.commuteTag.some
              )
              .evalMap(id => IO.println(s"Created strava activity ${id.id}"))
              .compile
              .drain
      }
    } yield ()

  def amendNoStravaTag(cond: Option[Condition], tag: TagName) = {
    val excludeTag = Condition.Not(TagAnyMatch(NonEmptyList.of(tag)))
    cond match {
      case Some(c) =>
        Condition.And(NonEmptyList.of(c, excludeTag)).some
      case None => excludeTag.some
    }
  }
}
