package fit4s.cli

import java.time.ZoneId

import cats.data.Validated
import cats.effect.{Clock, IO, Resource}
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.ActivityLog
import fit4s.activities.data.*
import fit4s.activities.dump.ExportData
import fit4s.profile.types.Sport

import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.{Argument, Opts}

trait SharedOpts {
  final val defaultNoStravaTag = TagName.unsafeFromString("No-Strava")

  given sportArgument: Argument[Sport] =
    Argument.from[Sport]("sport") { str =>
      Sport.all.find(_.typeName.equalsIgnoreCase(str)) match {
        case Some(s) => Validated.validNel(s)
        case None    => Validated.invalidNel(s"Unknown sport '$str'.")
      }
    }

  given tagNameArgument: Argument[TagName] =
    Argument.from[TagName]("tag") { str =>
      TagName.fromString(str).toValidatedNel
    }

  given pathArgument: Argument[Path] =
    Argument.readPath.map(Path.fromNioPath)

  given activityIdArgument: Argument[ActivityId] =
    Argument.readLong.map(ActivityId.apply)

  given locationIdArgument: Argument[LocationId] =
    Argument.readLong.map(LocationId.apply)

  given Argument[Host] =
    Argument.from[Host]("host") { str =>
      Host.fromString(str).toValidNel(s"Invalid host: $str")
    }

  given Argument[Port] =
    Argument.from[Port]("port") { str =>
      Port.fromString(str).toValidNel(s"Invalid port: $str")
    }

  val noStravaTag = Opts
    .option[TagName](
      "no-strava-tag",
      s"Activities with this tag are not considered when linking/uploading to strava. Default: ${defaultNoStravaTag.name}"
    )
    .withDefault(defaultNoStravaTag)

  val sequential: Opts[Boolean] =
    Opts.flag("sequential", "Whether to import using a single threads").orFalse

  val initialTags: Opts[List[TagName]] =
    Opts
      .options[TagName](
        "tag",
        help = "Associate these tags to all imported activities"
      )
      .orEmpty

  val activitySelectionOps: Opts[ActivitySelection] = {
    val w = Opts
      .flagOption[Int]("week", "Current week", metavar = "weeks-back")
      .map(ActivitySelection.ForWeek.apply)
      .validate("Week back number must be >= 1") {
        case ActivitySelection.ForWeek(Some(b)) if b < 1 => false
        case _                                           => true
      }

    val y = Opts
      .flagOption[Int]("year", "A specific year or current", metavar = "year")
      .map(ActivitySelection.ForYear.apply)

    val cq = Opts
      .option[String]("query", "A custom query")
      .map(_.trim)
      .map(ActivitySelection.Custom.apply)
      .withDefault(ActivitySelection.NoQuery)

    cq.orElse(w.orElse(y))
  }

  val pageOpts: Opts[Page] = {
    val limit = Opts
      .option[Int]("limit", "Maximum number of entries to return")
      .withDefault(Int.MaxValue)
      .validate(s"limit must be > 0")(_ > 0)
    val offset = Opts
      .option[Int]("offset", "How many entries to skip")
      .withDefault(0)
      .validate(s"offset must be >= 0")(_ >= 0)

    (limit, offset).mapN(Page.apply).withDefault(Page.unlimited)
  }

  val outputFormatOpts: Opts[OutputFormat] = {
    val json = Opts.flag("json", "Print results in JSON").as(OutputFormat.Json)
    val text =
      Opts.flag("text", "Print results in human readable form").as(OutputFormat.Text)

    json.orElse(text).withDefault(OutputFormat.Text)
  }

  val bikeTagPrefix = Opts
    .option[TagName]("bike-tag", "Prefix for tagging the used bike. Default: Bike")
    .withDefault(TagName.unsafeFromString("Bike"))

  val shoeTagPrefix = Opts
    .option[TagName]("shoe-tag", "Prefix for tagging used shoes. Default: Shoes")
    .withDefault(TagName.unsafeFromString("Shoe"))

  val commuteTag = Opts
    .option[TagName]("commute-tag", "Tag used to mark commutes. Default: Commute")
    .withDefault(TagName.unsafeFromString("Commute"))

  def resolveQuery(selection: ActivitySelection, zoneId: ZoneId) =
    for {
      currentTime <- Clock[IO].realTimeInstant

      query <- ActivitySelection
        .makeCondition(selection, zoneId, currentTime)
        .fold(err => IO.raiseError(new CliError(err)), IO.pure)
    } yield query

  def resolveStravaAuth(cliConfig: CliConfig) =
    IO(cliConfig.stravaAuthConfig).flatMap {
      case Some(c) => IO.pure(c)
      case None =>
        IO.raiseError(new Exception(s"No strava client_id and client_secret configured!"))
    }

  def activityLog(cliConfig: CliConfig): Resource[IO, ActivityLog[IO]] =
    ActivityLog[IO](
      cliConfig.jdbcConfig,
      cliConfig.nominatimConfig,
      cliConfig.stravaConfig,
      cliConfig.httpTimeout
    )
}

object SharedOpts extends SharedOpts
