package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.FitFile
import fit4s.cli.activity.{ImportCmd, SummaryCmd}
import fit4s.data.{ActivitySummary, Calories, Distance}
import fit4s.profile.types.Sport
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}
import scodec.Attempt

import java.time.{Duration, ZoneId}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Config] =
    Opts.subcommand("import", "Import fit files") {
      fileOrDirArgs.map(ImportCmd.Config.apply)
    }

  val summaryArgs: Opts[SummaryCmd.Config] =
    Opts.subcommand("summary", "Show a activity summary from imported activities") {
      val year = Opts
        .option[Int]("year", "Summary of given year")
        .orNone
      val sport = BasicOpts.sport.orNone

      (year, sport).mapN(SummaryCmd.Config.apply)
    }

  val opts = importArgs
    .map(Config.Import)
    .orElse(summaryArgs.map(Config.Summary))

  sealed trait Config extends Product
  object Config {
    final case class Import(cfg: ImportCmd.Config) extends Config
    final case class Summary(cfg: SummaryCmd.Config) extends Config
  }

  def apply(cfg: Config): IO[ExitCode] =
    cfg match {
      case Config.Import(c)  => ImportCmd(c)
      case Config.Summary(c) => SummaryCmd(c)
    }

  // ------------------------------------------------

  def adhocSummary(directory: Path): IO[ExitCode] =
    readAll(directory)
      .through(makeSummary)
      .evalMap { summary =>
        IO {
          val keys = summary.keys.toVector.sortBy(k => k.year.toString + k.sport.toString)

          println("\n\n")
          val lines = keys.map { key =>
            val s = summary.getOrElse(key, SummaryValues.zero)
            s"${key.year} ${key.sport}\t ${s.distance} ${s.duration} ${s.calories}"
          }
          println(lines.mkString("\n"))
        }
      }
      .compile
      .drain
      .as(ExitCode.Success)

  private def readAll(dir: Path): Stream[IO, ActivitySummary] =
    Files[IO]
      .walk(dir)
      .filter(p => p.extName.equalsIgnoreCase(".fit"))
      .evalFilter(p => Files[IO].isRegularFile(p))
      .evalTap(p => stderr(s"Reading $p"))
      .flatMap(p => Files[IO].readAll(p).through(parseFit))
      .flatMap(skipErrors)
      .map(ActivitySummary.from)
      .flatMap {
        case Right(a) => Stream.emit(a)
        case Left(err) =>
          Stream.eval(stderr(s"ERROR: Not an activity fit: $err")).drain
      }

  def skipErrors(
      fit: Attempt[FitFile]
  ): Stream[IO, FitFile] =
    fit.toEither match {
      case Right(el) => Stream.emit(el)
      case Left(err) =>
        Stream.eval(stderr(s"Parsing fit file failed: $err")).drain
    }

  def parseFit(in: Stream[IO, Byte]): Stream[IO, Attempt[FitFile]] =
    in.chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .map(bv => FitFile.decode(bv.toByteVector))

  def makeSummary(in: Stream[IO, ActivitySummary]): Stream[IO, Map[Key, SummaryValues]] =
    Stream.eval {
      stderr("Creating summary ...") >>
        in.compile.toVector
          .map(_.groupBy(Group.key))
          .map(
            _.view
              .mapValues { activities =>
                val distance = Distance.meter(activities.map(_.distance).map(_.meter).sum)
                val calories = Calories.kcal(activities.map(_.calories).map(_.kcal).sum)
                val duration = Duration.ofMinutes(
                  activities
                    .map(a => if (a.movingTime.isZero) a.elapsedTime else a.movingTime)
                    .map(_.toMinutes)
                    .sum
                )
                SummaryValues(distance, calories, duration)
              }
              .toMap
          )
    }

  private def stderr(str: String): IO[Unit] = IO {
    Console.print(s"\r$str")
  }

  case class SummaryValues(distance: Distance, calories: Calories, duration: Duration)
  object SummaryValues {
    val zero: SummaryValues = SummaryValues(Distance.zero, Calories.zero, Duration.ZERO)
  }
  case class Key(sport: Sport, year: Int)
  case class Group(key: Key, distance: Distance)

  object Group {

    def key(a: ActivitySummary): Key =
      Key(a.sport, a.startTime.atZone(ZoneId.of("Europe/Paris")).getYear)

    def from(a: ActivitySummary): Group =
      Group(key(a), a.distance)
  }
}
