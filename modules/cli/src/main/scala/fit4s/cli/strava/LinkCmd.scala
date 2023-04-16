package fit4s.cli.strava

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityQuery
import fit4s.activities.ActivityQuery.Condition
import fit4s.activities.ActivityQuery.Condition.TagAnyMatch
import fit4s.activities.StravaSupport.PublishResult
import fit4s.activities.data.{Page, TagName}
import fit4s.cli.{ActivitySelection, CliConfig, CliError, SharedOpts}

object LinkCmd extends SharedOpts {

  final case class Options(
      condition: ActivitySelection,
      page: Page,
      noStravaTag: TagName,
      bikeTagPrefix: TagName,
      shoeTagPrefix: TagName,
      commuteTag: TagName
  )

  val opts: Opts[Options] = {
    val noStrava = Opts
      .option[TagName](
        "no-strava-tag",
        s"Activities with this tag are not considered when linking to strava. Default: ${defaultNoStravaTag.name}"
      )
      .withDefault(defaultNoStravaTag)

    (
      activitySelectionOps,
      pageOpts,
      noStrava,
      bikeTagPrefix,
      shoeTagPrefix,
      commuteTag
    ).mapN(Options)
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
          case Some(data) =>
            IO.println(
              s"Looking for Strava activities between ${data.lowestStart} and ${data.recentStart}.\n" +
                s"Potentially update ${data.count} activities currently without a Strava ID."
            )
          case None =>
            IO.println("No activities found needed to sync from strava.")
        }

        result <- log.strava.linkActivities(
          cfg,
          query,
          opts.bikeTagPrefix.some,
          opts.shoeTagPrefix.some,
          opts.commuteTag.some
        )

        _ <- IO.println(
          s"Looked at ${result.fold(_.allCount, 0)} strava activities in that time range."
        )
        _ <- IO.println(showResult(result))
      } yield ExitCode.Success
    }

  def amendNoStravaTag(cond: Option[Condition], tag: TagName) = {
    val excludeTag = Condition.Not(TagAnyMatch(NonEmptyList.of(tag)))
    cond match {
      case Some(c) =>
        Condition.And(NonEmptyList.of(c, excludeTag)).some
      case None => excludeTag.some
    }
  }

  def showResult(r: PublishResult): String =
    r match {
      case PublishResult.NoActivitiesFound =>
        "No activities found without a strava link."

      case PublishResult.Success(
            uploads,
            linked,
            existed,
            notFound,
            ambiguous,
            alreadyLinked
          ) =>
        val basic = s"Uploaded: $uploads, Linked: $linked, Existed: $existed"
        val nf =
          if (notFound > 0)
            s"$notFound activities could not be found in the local database"
          else ""

        val amb = ambiguous match {
          case Nil => ""
          case nonEmpty =>
            val lines =
              nonEmpty
                .map(am =>
                  s"Strava: ${am.stravaId.id} -> ${am.activities.map(_.id).toList.mkString(", ")}"
                )
                .mkString("  ", "\n", "\n")
            s"For these strava activities, there were no exact matches found: \n$lines"
        }

        val al = alreadyLinked match {
          case Nil => ""
          case nonEmpty =>
            val ids =
              nonEmpty
                .map(el => s"${el.activityId} -> ${el.stravaId}")
                .mkString("  ", "\n", "\n")

            s"These are cases where a local activity was found, but it is already linked to a different strava activity:\n$ids"

        }

        List(basic, nf, amb, al).filter(_.nonEmpty).mkString("\n")
    }
}
