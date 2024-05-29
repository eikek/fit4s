package fit4s.activities.impl

import cats.syntax.all.*
import fs2.io.file.Path

enum ActivityFile(val path: Path):
  case Tcx(override val path: Path) extends ActivityFile(path)
  case Fit(override val path: Path) extends ActivityFile(path)

  def fold[A](f: Fit => A, g: Tcx => A): A = this match
    case x: Fit => f(x)
    case x: Tcx => g(x)

object ActivityFile:
  def apply(f: Path): Option[ActivityFile] =
    extNames(f) match
      case ".fit" :: _          => ActivityFile.Fit(f).some
      case ".gz" :: ".fit" :: _ => ActivityFile.Fit(f).some
      case ".tcx" :: _          => ActivityFile.Tcx(f).some
      case ".gz" :: ".tcx" :: _ => ActivityFile.Tcx(f).some
      case _                    => None

  private def extNames(p: Path): List[String] =
    val fname = p.fileName.toString.toLowerCase
    val lidx = fname.lastIndexOf('.', fname.size)
    val pidx = fname.lastIndexOf('.', lidx - 1)

    val last = if (lidx > 0) fname.substring(lidx) else ""
    val prev = if (pidx > 0 && pidx < lidx) fname.substring(pidx, lidx) else ""

    List(last, prev).filter(_.nonEmpty)
