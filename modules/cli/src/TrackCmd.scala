package fit4s.cli

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.core.Fit
import fit4s.core.data.Polyline
import fit4s.core.data.Timespan

import com.monovore.decline.Opts

object TrackCmd extends CmdCommons:
  final case class Options(fitFile: Path, precision: Polyline.Precision)

  val opts: Opts[Options] =
    (inputFileArg, polylinePrecisionOpt).mapN(Options.apply)

  def apply(cfg: Options): IO[ExitCode] =
    for {
      fits <- readFit(cfg.fitFile)
      track <- makeTrack(fits, cfg)
      _ <- IO.println(track.encoded)
    } yield ExitCode.Success

  def makeTrack(fits: Vector[Fit], opts: Options): IO[Polyline] =
    given Polyline.Config = Polyline.Config(precision = opts.precision)
    val init: Either[String, Polyline] = Right(Polyline.empty)
    val pl = fits.map(_.track(Timespan.all)).foldLeft(init) { (res, pl) =>
      res.flatMap(p => pl.map(p ++ _))
    }
    reportError(IO.pure(pl))
