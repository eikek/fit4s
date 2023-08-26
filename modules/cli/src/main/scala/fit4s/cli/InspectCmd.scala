package fit4s.cli

import cats.effect.{ExitCode, IO}
import fs2.io.file.{Files, Path}

import fit4s.cli.JsonEncoder._
import fit4s.{FitFile, MessageType}

import com.monovore.decline.Opts
import io.bullet.borer.Json
import scodec.bits.ByteVector

object InspectCmd {

  final case class Options(
      fitFile: Path
  )

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
      fits <- reportError(readFit(cfg.fitFile))
      validOnly = fits.map(
        _.filterDataRecords(dm => dm.isKnownMessage)
          .filterRecords(_.header.messageType == MessageType.DataMessage)
      )
      json = Json.encode(validOnly.toList)
      _ <- IO.println(json.toUtf8String)
    } yield ExitCode.Success

  def readFit(file: Path) =
    Files[IO]
      .readAll(file)
      .chunks
      .compile
      .fold(ByteVector.empty)(_ ++ _.toByteVector)
      .map(FitFile.decode)
      .map(_.toEither)

  def reportError[E, A](v: IO[Either[E, A]]): IO[A] =
    v.flatMap {
      case Right(a) => IO.pure(a)
      case Left(e)  => IO.raiseError(new CliError(e.toString))
    }
}
