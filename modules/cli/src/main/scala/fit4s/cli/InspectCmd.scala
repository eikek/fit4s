package fit4s.cli

import cats.effect.{ExitCode, IO}
import fit4s.cli.JsonEncoder._
import fit4s.{FitFile, MessageType}
import fs2.io.file.{Files, Path}
import io.circe.syntax._
import scodec.bits.ByteVector

object InspectCmd {

  final case class Config(
      fitFile: Path
  )

  def apply(cfg: Config): IO[ExitCode] =
    for {
      fit <- reportError(readFit(cfg.fitFile))
      validOnly = fit
        .filterDataRecords(dm => dm.isKnownSuccess)
        .filterRecords(_.header.messageType == MessageType.DataMessage)
      json = validOnly.asJson
      _ <- IO.println(json.spaces2)
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
