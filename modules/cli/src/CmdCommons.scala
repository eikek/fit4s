package fit4s.cli

import java.nio.file.Files

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.core.Fit
import fit4s.core.data.Polyline

import com.monovore.decline.Argument
import com.monovore.decline.Opts

trait CmdCommons:
  given Argument[Path] =
    Argument[java.nio.file.Path].map(Path.fromNioPath)

  val inputFileArg = Opts
    .argument[Path]("file")
    .validate(s"file must be a file")(p => !Files.isDirectory(p.toNioPath))

  val outputFile = Opts
    .option[Path]("out", "The file to write")

  val polylinePrecisionOpt = Opts
    .option[Int]("precision", "The precision for the polyline.")
    .mapValidated {
      case 4 => Polyline.Precision.low.validNel
      case 5 => Polyline.Precision.default.validNel
      case 6 => Polyline.Precision.high.validNel
      case n => s"Precision must be a number in [4,6]".invalidNel
    }
    .withDefault(Polyline.Precision.default)

  def readFit(file: Path) =
    reportError(IO.blocking(Fit.fromNIO(file.toNioPath).toEither))

  def reportError[E, A](v: IO[Either[E, A]]): IO[A] =
    v.flatMap {
      case Right(a) => IO.pure(a)
      case Left(e)  => IO.raiseError(new CliError(e.toString))
    }
