package fit4s.cli

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.core.Activity
import fit4s.core.Fit
import fit4s.core.data.*

import com.monovore.decline.Opts

object ViewCmd extends CmdCommons:
  enum OutFile:
    case Given(path: Path)
    case Default

    def isGiven: Boolean = this != Default

    def file: Path = this match
      case Given(p) => p
      case Default  => Path.fromNioPath(Files.createTempFile("fit_view_", ".html"))

  final case class Options(
      fitFile: Path,
      precision: Polyline.Precision,
      out: OutFile,
      overwrite: Boolean,
      openFile: Boolean
  )

  val opts: Opts[Options] =
    (
      inputFileArg,
      polylinePrecisionOpt,
      outputFile.map(p => OutFile.Given(p)).withDefault(OutFile.Default),
      Opts.flag("overwrite", "Overwrite existing file", "o").orFalse,
      Opts.flag("no-open", "Do not open the file, only write", "n").orFalse.map(!_)
    ).mapN(Options.apply)

  def apply(cfg: Options): IO[ExitCode] =
    for {
      fits <- readFit(cfg.fitFile)
      tracks <- reportError(IO.pure(makeFitTracks(fits, cfg)))
      html = FitHtml(fits, tracks, cfg.fitFile)

      file <- IO.blocking {
        val out = cfg.out.file
        if cfg.out.isGiven && Files.exists(out.toNioPath) && !cfg.overwrite
        then throw CliError(s"File already exists: ${out}")
        else writeHtml(out, html)
        out
      }
      _ <- IO.println(s"Written to: ${file.absolute}")
      _ <- if (cfg.openFile) open(file) else IO.unit
    } yield ExitCode.Success

  def open(file: Path): IO[Unit] = IO.blocking {
    Desktop.getDesktop().open(file.toNioPath.toFile())
  }

  def writeHtml(out: Path, html: scalatags.Text.all.doctype): Unit =
    val os = Files.newOutputStream(
      out.toNioPath,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
    html.writeBytesTo(os)
    os.close()

  def makeFitTracks(fits: Vector[Fit], opts: Options): Either[String, SessionTracks] =
    val init: Either[String, SessionTracks] = Right(
      SessionTracks(Vector.empty, opts, Vector.empty)
    )
    fits.foldLeft(init) { (tracks, fit) =>
      tracks.flatMap(pls => makeFitTrack(fit, opts).map(next => pls ++ next))
    }

  def makeFitTrack(fit: Fit, opts: Options): Either[String, SessionTracks] =
    Activity.from(fit).flatMap {
      case None =>
        fit.getLatLngs(Timespan.all).map(c => SessionTracks(Vector.empty, opts, c))

      case Some(act) =>
        Right(
          SessionTracks(
            act.sessionRecords.map { sr =>
              SessionTrack(
                sr.session.timespan,
                sr.session.sport,
                sr.records.flatMap(_.position.map(_.toLatLng))
              )
            },
            opts,
            act.unrelatedRecords.flatMap(_.position.map(_.toLatLng))
          )
        )
    }

  final case class SessionTrack(
      time: Timespan,
      name: String,
      track: Vector[LatLng] = Vector.empty
  ):
    def addPos(p: LatLng) = copy(track = track.appended(p))
    def line(using cfg: Polyline.Config): Polyline = Polyline(track*)

  final case class SessionTracks(
      sessions: Vector[SessionTrack],
      opts: Options,
      unrelated: Vector[LatLng] = Vector.empty
  ):
    given Polyline.Config = Polyline.Config(precision = opts.precision)

    def isEmpty: Boolean = sessions.isEmpty && unrelated.isEmpty
    def nonEmpty: Boolean = !isEmpty

    def lines: Vector[Polyline] =
      (sessions.map(_.line) :+ Polyline(unrelated*)).filter(_.nonEmpty)

    def ++(other: SessionTracks): SessionTracks =
      SessionTracks(sessions ++ other.sessions, opts, unrelated ++ other.unrelated)
