package fit4s.cli.dump

import scala.concurrent.duration.*

import cats.Applicative
import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*

import fit4s.activities.dump.DumpFormat
import fit4s.activities.dump.DumpFormat.*
import fit4s.activities.dump.ExportData
import fit4s.activities.dump.ExportData.ProgressObserve

final case class Progress[F[_]](
    progress: ProgressObserve[F],
    counters: Ref[F, Map[Class[_], Int]]
) {

  def makeSummary(implicit F: Applicative[F]): F[String] =
    counters.get.map { m =>
      m.toList
        .sortBy(t => Progress.classLabel(t._1))
        .map { case (k, n) =>
          s"${Progress.classLabel(k)}: $n"
        }
        .mkString("\n")
    }
}

object Progress {

  def apply[F[_]: Async]: F[Progress[F]] = for {
    data <- Ref[F].of(Map.empty[Class[_], Int])
    start <- Clock[F].monotonic
    lastTime <- Ref[F].of(start)
    cp = ExportData.ProgressObserve.count(data)
    cns = Console.make[F]
    pr = ExportData.ProgressObserve(v =>
      (Clock[F].monotonic, lastTime.get).flatMapN { (cur, last) =>
        if (cur - last >= 1.second) lastTime.set(cur) *> cns.print(s"\r${label(v)} ...")
        else ().pure[F]
      }
    )
  } yield Progress(cp >> pr, data)

  private def classLabel(c: Class[_]): String =
    c.getSimpleName().drop(1)

  private def label(v: DumpFormat): String = v match
    case DActivity(value)         => show"Activity: ${value.id}"
    case DTag(value)              => show"Tag: ${value.id}"
    case DLocation(value)         => show"Location: ${value.id}"
    case DSession(value)          => show"Session: ${value.id}"
    case DLap(value)              => show"Lap: ${value.id}"
    case DSessionData(value)      => show"SessionData: ${value.id}"
    case DActivityTag(value)      => show"ActivityTag: ${value.id}"
    case DActivityStrava(value)   => show"ActivityStrava: ${value.id}"
    case DGeoPlace(value)         => show"GeoPlace: ${value.id}"
    case DActivityGeoPlace(value) => show"ActivityGeoPlace: ${value.id}"
    case DStravaToken(value)      => show"StravaToken: ${value.id}"

}
