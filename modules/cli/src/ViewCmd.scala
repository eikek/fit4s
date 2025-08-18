package fit4s.cli

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.core.Fit
import fit4s.core.data.Polyline

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
      track <- makeTrack(fits, cfg)
      html = FitHtml(fits, track, cfg.fitFile)

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

  def makeTrack(fits: Vector[Fit], opts: Options): IO[Polyline] =
    given Polyline.Config = Polyline.Config(precision = opts.precision)
    val init: Either[String, Polyline] = Right(Polyline.empty)
    val pl = fits.map(_.track).foldLeft(init) { (res, pl) =>
      res.flatMap(p => pl.map(p ++ _))
    }
    reportError(IO.pure(pl))
