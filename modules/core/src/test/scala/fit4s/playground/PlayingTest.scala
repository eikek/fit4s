package fit4s.playground

import cats.effect._
import fit4s.FitFile
import fit4s.FitMessage.DataMessage
import fit4s.data.{ActivitySession, Distance}
import fit4s.json.JsonCodec
import fit4s.playground.PlayingTest.Group
import fit4s.profile.messages.Msg
import fit4s.profile.types.{MesgNum, Sport}
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}
import io.circe.syntax._
import munit.CatsEffectSuite
import scodec.Attempt

import java.io.InputStream
import java.time.{Duration, ZoneId}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

@annotation.nowarn
class PlayingTest extends CatsEffectSuite with JsonCodec {
  val quotes = "\"\"\""

  override def munitTimeout = new FiniteDuration(300, TimeUnit.HOURS)

  test("codecs should encode and decode") {
    val dir = Path("/home/eike/workspace/garmin/garmin-connect/activities")
    val sysTime =
      Path( // 1862739919_ACTIVITY.fit=1992-01-18T18:36:18Z   ok:1862739926_ACTIVITY.fit=2012-09-27T16:26:14Z
        // "/Users/ekettner/personal/fit4s/modules/core/src/test/resources/fit/activity/1862739919_ACTIVITY.fit"
        // "local/garmin-sdk/examples/MonitoringFile.fit"
        // "modules/core/src/test/resources/fit/activity/2023-03-16-06-25-37.fit"
        "modules/core/src/test/resources/fit/activity_poolswim_with_hr.fit"
      )

    Files[IO]
      .readAll(sysTime)
      .through(parseFit)
      .evalMap(requireFit)
      .map(_._2)
      .evalMap(fit => printActivitySummary(fit))
      .debug()
      .compile
      .drain

//    readAllIn(dir)
//      .filter(_.id.createdAt.exists(_.isSystemTime))
//      .debug()
//      .compile
//      .drain

//    val file = IO(getClass.getResourceAsStream("/fit/activity/2023-03-20-13-29-51.fit"))
//    for {
//      fit <- parseFit(file)
//      _ <- printActivitySummary(fit)
//    } yield ()
  }

  def parseFit(in: IO[InputStream]): IO[FitFile] =
    fs2.io
      .readInputStream(in, 8192)
      .through(parseFit)
      .evalMap(requireFit)
      .compile
      .lastOrError
      .flatMap { case (duration, result) =>
        IO.println(s"Read+Parsing FIT file took: ${duration.toMillis}ms").as(result)
      }

  def requireFit(
      fit: (FiniteDuration, Attempt[FitFile])
  ): IO[(FiniteDuration, FitFile)] = {
    val (d, a) = fit
    val file =
      a.toEither.left
        .map(err => new Exception(err.messageWithContext))
        .fold(IO.raiseError, IO.pure)
    file.map(d -> _)
  }

  def skipErrors(
      fit: (FiniteDuration, Attempt[FitFile])
  ): Stream[IO, (FiniteDuration, FitFile)] = {
    val (d, f) = fit
    f.toEither match {
      case Right(el) => Stream.emit(d -> el)
      case Left(err) =>
        Stream.eval(IO.println(s"ERROR: parsing fit file failed: $err")).drain
    }
  }

  def parseFit(in: Stream[IO, Byte]) =
    in.chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .evalMap(bv =>
        Clock[IO]
          .timed(IO(FitFile.decode(bv.toByteVector)))
      )

  def printActivitySummary(fit: FitFile) =
    IO {
      val t1 = System.nanoTime()
      val s1 = fit.findFirstData(MesgNum.Session).fold(sys.error, identity)
      val a1 = ActivitySession.from(s1).fold(sys.error, identity)
      val d1 = Duration.ofNanos(System.nanoTime() - t1)

      println(s"DEBUG: Reading summary took: $d1")
      println(a1)
    }

  def readAllIn(dir: Path) =
    Files[IO]
      .walk(dir)
      .evalFilter(p => Files[IO].isRegularFile(p))
      .filter(p => p.extName.equalsIgnoreCase(".fit"))
      .debug(p => s"INFO: Reading file $p...")
      .flatMap(p => Files[IO].readAll(p).through(parseFit))
      .flatMap(skipErrors)
      .evalMap { case (duration, fit) =>
        IO.println(s"DEBUG: Read+parsing FIT file took: ${duration.toMillis}ms").as(fit)
      }
      .map(_.findFirstData(MesgNum.Session).fold(sys.error, identity))
      .map(ActivitySession.from)
      .flatMap {
        case Right(a) => Stream.emit(a)
        case Left(err) =>
          Stream.eval(IO.println(s"ERROR: Not an activity fit: $err")).drain
      }

  def makeSummary(in: Stream[IO, ActivitySession]) =
    IO.println("INFO: Creating summary ...") >>
      in.compile.toVector
        .map(_.groupBy(Group.key))
        .map(
          _.view
            .mapValues(activities =>
              Distance.meter(activities.map(_.distance).map(_.meter).sum)
            )
            .toMap
        )

  def printDecoded(fit: FitFile) =
    Stream
      .emits(fit.dataRecords)
      .filter(_.isKnownMessage)
      .map(r =>
        r.definition.profileMsg
          .getOrElse(r.definition.globalMessageNumber) -> r.dataFields
      )
      // .take(1000)
      .evalMap(IO.println)

  def printTestCases(fit: FitFile): Stream[IO, Unit] =
    Stream
      .emits(MesgNum.all.map(n => n -> fit.findData(n)))
      .filter { case (_, list) => list.nonEmpty }
      .map { case (mesg, data) =>
        println(makeTestCaseStub(s"decode $mesg data", data.head))
      }

  def filterRecordsWithComponents(
      fit: FitFile
  ): Vector[(Msg, List[Msg.FieldAttributes], DataMessage)] =
    fit.dataRecords
      .filter(_.definition.profileMsg.isDefined)
      .flatMap { dm =>
        val profileMsg = dm.definition.profileMsg.get
        val fieldsWithComponents = profileMsg.allFields.filter(_.components.nonEmpty)
        val names = fieldsWithComponents.map(_.fieldName).toSet
        val result = dm.definition.fields.flatMap(f =>
          profileMsg.findField(f.fieldDefNum).filter(pf => names.contains(pf.fieldName))
        )
        if (result.isEmpty) Vector.empty
        else Vector((profileMsg, result, dm))
      }

  def makeTestCaseStub(title: String, dm: DataMessage): String =
    s"""
       |test("$title") {
       |  val data = ByteVector.fromValidHex("${dm.raw.toHex}")
       |  val definition = io.circe.parser.decode[FitMessage.DefinitionMessage]($quotes${dm.definition.asJson.noSpaces}$quotes).fold(throw _, identity)
       |
       |}
       |""".stripMargin

}

object PlayingTest {
  case class Key(sport: Sport, year: Int)
  case class Group(key: Key, distance: Distance)

  object Group {

    def key(a: ActivitySession): Key =
      Key(a.sport, a.startTime.asInstant.atZone(ZoneId.of("Europe/Paris")).getYear)

    def from(a: ActivitySession): Group =
      Group(key(a), a.distance)

    // implicit val semigroup: Semigroup[Group] =
  }
}
