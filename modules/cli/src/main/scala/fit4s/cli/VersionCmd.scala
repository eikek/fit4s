package fit4s.cli

import cats.effect._

import com.monovore.decline.Opts

object VersionCmd extends SharedOpts:

  case class Options(format: OutputFormat)

  val opts: Opts[Options] =
    outputFormatOpts.map(Options.apply)

  def apply(opts: Options): IO[ExitCode] =
    IO.println(opts.format.fold(showJson, showText)).as(ExitCode.Success)

  def showText: String = {
    val name = fit4s.BuildInfo.name
    val version = fit4s.BuildInfo.version
    val commit = fit4s.BuildInfo.gitHeadCommit
      .map(_.take(8))
      .map(c => s" (#$c)")
      .getOrElse("")
    val built = fit4s.BuildInfo.builtAtString

    s"$name $version, $built$commit"
  }

  def showJson: String =
    fit4s.BuildInfo.toJson
