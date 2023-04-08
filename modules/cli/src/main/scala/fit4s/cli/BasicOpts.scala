package fit4s.cli

import cats.data.{NonEmptyList, Validated}
import cats.syntax.all._
import com.monovore.decline.{Argument, Opts}
import fit4s.activities.data.{Page, TagName}
import fit4s.profile.types.Sport
import fs2.io.file.Path

trait BasicOpts {

  def fileArg: Opts[Path] =
    Opts
      .argument[java.nio.file.Path](metavar = "file")
      .map(Path.fromNioPath)
      .validate(s"file must be a file")(p =>
        !java.nio.file.Files.isDirectory(p.toNioPath)
      )

  def dirArg: Opts[Path] =
    Opts
      .argument[java.nio.file.Path](metavar = "dir")
      .map(Path.fromNioPath)
      .validate(s"dir must be a directory")(p =>
        java.nio.file.Files.isDirectory(p.toNioPath)
      )

  def fileOrDirArgs: Opts[NonEmptyList[Path]] =
    Opts
      .arguments[java.nio.file.Path](metavar = "fileOrDirs")
      .map(_.map(Path.fromNioPath))

  def sport: Opts[Sport] =
    Opts.option[Sport](
      "sport",
      help = s"Select a sport"
    )

  def excludeTags: Opts[List[TagName]] =
    Opts
      .options[TagName](
        "exclude-tags",
        help = "Exclude activities with any of these tags"
      )
      .orEmpty

  def includeTags: Opts[List[TagName]] =
    Opts
      .options[TagName](
        "tags",
        help = "Include only activities with any of these tags"
      )
      .orEmpty

  def parallel: Opts[Boolean] =
    Opts.flag("parallel", "Whether to import using multiple threads").orFalse

  def addTags: Opts[NonEmptyList[TagName]] =
    Opts.options[TagName](
      "tag",
      help = "Add these tags to selected activities."
    )

  def tagFilter: Opts[Option[TagName]] =
    Opts.option[TagName]("tag", "Filter with a tag name").orNone

  def initialTags: Opts[List[TagName]] =
    Opts
      .options[TagName](
        "tags",
        help = "Associate these tags to all imported activities"
      )
      .orEmpty

  def activitySelectionOps: Opts[ActivitySelection] = {
    val w = Opts
      .flagOption[Int]("week", "Current week", metavar = "weeks-back")
      .map(ActivitySelection.ForWeek)
      .validate("Week back number must be >= 1") {
        case ActivitySelection.ForWeek(Some(b)) if b < 1 => false
        case _                                           => true
      }

    val y = Opts
      .flagOption[Int]("year", "A specific year or current", metavar = "year")
      .map(ActivitySelection.ForYear)

    val cq = Opts
      .option[String]("query", "A custom query")
      .map(_.trim)
      .map(ActivitySelection.Custom)
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

    (limit, offset).mapN(Page.apply)
  }

  implicit private val sportArgument: Argument[Sport] =
    Argument.from[Sport]("sport") { str =>
      Sport.all.find(_.typeName.equalsIgnoreCase(str)) match {
        case Some(s) => Validated.validNel(s)
        case None    => Validated.invalidNel(s"Unknown sport '$str'.")
      }
    }

  implicit private val tagNameArgument: Argument[TagName] =
    Argument.from[TagName]("tag") { str =>
      TagName.fromString(str).toValidatedNel
    }
}

object BasicOpts extends BasicOpts
