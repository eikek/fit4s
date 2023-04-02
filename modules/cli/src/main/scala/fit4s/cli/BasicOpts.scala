package fit4s.cli

import cats.data.{NonEmptyList, Validated}
import cats.syntax.all._
import com.monovore.decline.{Argument, Opts}
import fit4s.activities.data.TagName
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
      .arguments[java.nio.file.Path](metavar = "fileOrDir")
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
