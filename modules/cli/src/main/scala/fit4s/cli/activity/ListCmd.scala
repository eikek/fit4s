package fit4s.cli.activity

import cats.effect._
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityQuery.OrderBy
import fit4s.activities.data.{ActivityListResult, Page}
import fit4s.activities.records.ActivitySessionRecord
import fit4s.activities.{ActivityLog, ActivityQuery}
import fit4s.cli.FormatDefinition._
import fit4s.cli.{ActivitySelection, SharedOpts, CliConfig, CliError, Styles}
import fit4s.profile.types.Sport

import java.time.ZoneId

object ListCmd extends SharedOpts {

  final case class Options(query: ActivitySelection, page: Page, filePathOnly: Boolean)

  val opts: Opts[Options] = {
    val filesOnly = Opts.flag("files-only", "Only print filenames").orFalse
    (activitySelectionOps, pageOpts, filesOnly).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        zone = cliCfg.timezone

        query <- ActivitySelection
          .makeCondition(opts.query, zone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        list = log.activityList(ActivityQuery(query, OrderBy.StartTime, opts.page))

        _ <- list
          .map(makeString(zone, opts.filePathOnly))
          .evalMap(IO.println)
          .compile
          .drain
      } yield ExitCode.Success
    }

  def makeString(zoneId: ZoneId, filesOnly: Boolean)(r: ActivityListResult): String =
    if (filesOnly) filePathString(r)
    else activityString(zoneId)(r)

  def filePathString(r: ActivityListResult): String =
    (r.location.location / r.activity.path).absolute.toString

  def activityString(zoneId: ZoneId)(r: ActivityListResult): String = {
    val lineSep = List.fill(78)('-').mkString.in(Styles.sessionSeparator)
    r.sessions.toList
      .map(sessionString(r, zoneId))
      .mkString("", "\n", "\n  ") + lineSep + "\n"
  }

  def sessionString(r: ActivityListResult, zoneId: ZoneId)(
      s: ActivitySessionRecord
  ): String = {
    implicit val zone: ZoneId = zoneId
    implicit val sport: Sport = s.sport

    List(
      r.activity.id.show.in(Styles.activityId),
      r.activity.created.show.in(Styles.activityDate),
      r.activity.name.in(Styles.activityName),
      (s.sport -> s.subSport).show.in(Styles.sport),
      s.distance.show.in(Styles.distance),
      s.movingTime.show.in(Styles.duration),
      s.totalAscend.show.in(Styles.elevation),
      (s.avgHr -> s.maxHr).show.in(Styles.heartRate),
      (s.avgSpeed -> s.maxSpeed).show.in(Styles.speed),
      s.calories.show.in(Styles.calories),
      s.avgTemp.show.in(Styles.temperature(s.avgTemp)),
      r.tags.show.in(Styles.tags),
      r.activity.device.show.in(Styles.device)
    )
      .mkString("  ")
  }
}
