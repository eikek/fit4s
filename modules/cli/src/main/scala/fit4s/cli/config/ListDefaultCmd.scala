package fit4s.cli.config

import cats.effect._
import fs2.Stream

import fit4s.cli.{ConfigValues, OutputFormat, SharedOpts}

import com.monovore.decline.Opts

@annotation.nowarn
object ListDefaultCmd extends SharedOpts {

  final case class Options(format: OutputFormat)

  val opts: Opts[Options] =
    outputFormatOpts.map(Options.apply)

  def apply(opts: Options): IO[ExitCode] =
    Stream
      .emits(ConfigValues.getAll.toList.sortBy(_._1))
      .map { case (key, default) =>
        s"$key=${default.getOrElse("")}"
      }
      .evalMap(IO.println)
      .compile
      .drain
      .as(ExitCode.Success)

}
