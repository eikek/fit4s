package fit4s.cli.activity

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.data.*
import fit4s.cli.*
import fit4s.cli.FormatDefinition.*
import fit4s.common.syntax.all.*
import fit4s.profile.types.Sport

import com.monovore.decline.Opts
import io.bullet.borer.Json

object ListCmd extends SharedOpts {

  final case class Options(
      query: ActivitySelection,
      page: Page,
      filePathOnly: Boolean,
      format: OutputFormat
  )

  val opts: Opts[Options] = {
    val filesOnly = Opts.flag("files-only", "Only print filenames").orFalse
    (activitySelectionOps, pageOpts, filesOnly, outputFormatOpts).mapN(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        query <- resolveQuery(opts.query, cliCfg.timezone)

        list = log.activityList(ActivityQuery(query, opts.page))

        _ <- list
          .evalMap(
            opts.format.fold(
              printJson(opts.filePathOnly),
              printText(cliCfg.timezone, opts.filePathOnly)
            )
          )
          .compile
          .drain
      } yield ExitCode.Success
    }

  def printJson(filesOnly: Boolean): ActivityListResult => IO[Unit] = {
    if (filesOnly)
      filePathString.andThen(Json.encode(_).toUtf8String).andThen(IO.println)
    (Json
      .encode(_: ActivityListResult)(using RecordJsonEncoder.encodeList))
      .andThen(_.toUtf8String)
      .andThen(IO.println)
  }

  def printText(zoneId: ZoneId, filesOnly: Boolean): ActivityListResult => IO[Unit] =
    makeString(zoneId, filesOnly).andThen(IO.println)

  def makeString(zoneId: ZoneId, filesOnly: Boolean)(r: ActivityListResult): String =
    if (filesOnly) filePathString(r)
    else activityString(zoneId)(r)

  def filePathString(r: ActivityListResult): String =
    (r.location.locationPath / r.activity.path).absolute.toString

  def activityString(zoneId: ZoneId)(r: ActivityListResult): String = {
    val lineSep = List.fill(78)('-').mkString.in(Styles.sessionSeparator)
    r.sessions.toList
      .map(sessionString(r, zoneId))
      .mkString("", "\n", "\n  ") + lineSep
  }

  def sessionString(r: ActivityListResult, zoneId: ZoneId)(
      s: ActivitySession
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
