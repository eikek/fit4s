package fit4s.activities.impl

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.github.tototoshi.csv.CSVReader
import fs2.io.file.{Files, Path}
import fs2.Stream

/** Extract activities from a strava export. */
object StravaExportExtract {

  case class ExportData(
      fitFile: Path,
      relativePath: String,
      exportFile: Path,
      name: Option[String],
      description: Option[String],
      bike: Option[String],
      shoe: Option[String],
      commute: Boolean
  )

  /** The export may be a zip file or the extracted directory. */
  def activities[F[_]: Async](stravaExport: Path): Resource[F, Vector[ExportData]] =
    unpackPhase(stravaExport).evalMap { dir =>
      findActivities[F](dir)
        .map(_.map(_.copy(exportFile = stravaExport)))
    }

  def findActivities[F[_]: Async](dir: Path): F[Vector[ExportData]] =
    for {
      bikes <- readBikesCsv[F](dir / "bikes.csv").attempt
      shoes <- readShoesCsv[F](dir / "shoes.csv").attempt
      act <- readActivitiesCsv[F](
        dir,
        shoes.getOrElse(Set.empty),
        bikes.getOrElse(Set.empty)
      )(dir / "activities.csv")
    } yield act.toVector

  private def unpackPhase[F[_]: Async](stravaExport: Path): Resource[F, Path] =
    Resource
      .eval(detectType[F](stravaExport))
      .flatMap {
        case InputDirType.ZipFile(_)   => unzip[F](stravaExport)
        case InputDirType.Directory(_) => Resource.pure[F, Path](stravaExport)
        case _ =>
          Resource.eval(
            Async[F].raiseError(
              new Exception(s"The strava export is neither a directory nor a zip file")
            )
          )
      }

  private def readActivitiesCsv[F[_]: Async](
      dir: Path,
      shoes: Set[String],
      bikes: Set[String]
  )(
      file: Path
  ) = Async[F].blocking {
    val reader = CSVReader.open(file.toNioPath.toFile)
    val data = reader.allWithHeaders()
    data.flatMap { m =>
      val (bike, shoe) = m.get("Activity Gear") match {
        case Some(g) if bikes.contains(g) => (g.some, None)
        case Some(g) if shoes.contains(g) => (None, g.some)
        case g                            => (g, None)
      }
      m.get("Filename").map { fn =>
        ExportData(
          fitFile = dir / fn,
          relativePath = fn,
          exportFile = dir,
          name = m.get("Activity Name"),
          description = m.get("Activity Description"),
          bike = bike,
          shoe = shoe,
          commute = m.get("Commute").flatMap(_.toDoubleOption).exists(_ > 0)
        )
      }
    }
  }

  private def readBikesCsv[F[_]: Async](file: Path): F[Set[String]] = Async[F].blocking {
    val reader = CSVReader.open(file.toNioPath.toFile)
    reader
      .allWithHeaders()
      .flatMap(_.get("Bike Name"))
      .toSet
  }

  private def readShoesCsv[F[_]: Async](file: Path): F[Set[String]] = Async[F].blocking {
    val reader = CSVReader.open(file.toNioPath.toFile)
    reader
      .allWithHeaders()
      .flatMap(_.get("Shoe Name"))
      .toSet
  }

  private def unzip[F[_]: Async](zipFile: Path): Resource[F, Path] =
    Files[F]
      .tempDirectory(None, "fit4s-zip-", None)
      .evalMap(target =>
        Stream
          .emit(zipFile)
          .through(
            Zip[F]().unzipFiles(
              nameFilter = activityZipFilter,
              targetDir = _ => Some(target)
            )
          )
          .compile
          .drain
          .as(target)
      )

  private def detectType[F[_]: Async](path: Path): F[InputDirType] =
    Files[F].isRegularFile(path).flatMap {
      case true =>
        if (path.extName.equalsIgnoreCase(".zip"))
          Async[F].pure(InputDirType.ZipFile(path))
        else Async[F].pure(InputDirType.Unknown)
      case false =>
        Files[F].isDirectory(path).map {
          case true  => InputDirType.Directory(path)
          case false => InputDirType.Unknown
        }
    }

  private def activityZipFilter: Zip.NameFilter =
    n => n.startsWith("activities") || n == "bikes.csv" || n == "shoes.csv"

  sealed trait InputDirType extends Product {
    def widen: InputDirType = this
  }
  object InputDirType {
    case object Unknown extends InputDirType
    case class ZipFile(path: Path) extends InputDirType
    case class Directory(path: Path) extends InputDirType
  }
}
