package fit4s.cli

import cats.effect.{ExitCode, IO}
import fs2.io.file.Path

import fit4s.borer.Fit4sJsonEncoder.given
import fit4s.core.Fit

import com.monovore.decline.Opts
import io.bullet.borer.Json

object InspectCmd extends CmdCommons:
  final case class Options(fitFile: Path)

  val opts: Opts[Options] =
    Opts
      .argument[java.nio.file.Path](metavar = "file")
      .map(Path.fromNioPath)
      .validate(s"file must be a file")(p =>
        !java.nio.file.Files.isDirectory(p.toNioPath)
      )
      .map(Options.apply)

  def apply(cfg: Options): IO[ExitCode] =
    for {
      fits <- readFit(cfg.fitFile)
      _ <- IO.blocking(dumpAll(fits))
    } yield ExitCode.Success

  def dumpAll(fits: Vector[Fit]): Unit =
    fits.foreach(f => println(Json.encode(f).toUtf8String))
