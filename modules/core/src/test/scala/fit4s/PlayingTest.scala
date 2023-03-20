package fit4s

import cats.effect._
import fit4s.data.ActivitySummary
import io.circe.syntax._
import fit4s.profile.types.MesgNum
import fs2.{Chunk, Stream}
import munit.CatsEffectSuite

import java.io.InputStream
import java.time.Duration

class PlayingTest extends CatsEffectSuite with JsonCodec {
  val quotes = "\"\"\""

  test("codecs should encode and decode") {
    val file = IO(getClass.getResourceAsStream("/fit/activity/2023-03-20-13-29-51.fit"))
    for {
      fit <- parseFit(file)
      _ <- printActivitySummary(fit)
    } yield ()
  }

  def parseFit(in: IO[InputStream]): IO[FitFile] =
    fs2.io
      .readInputStream(in, 8192)
      .chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .evalMap(bv =>
        Clock[IO]
          .timed(IO(FitFile.decodeUnsafe(bv.toByteVector)))
      )
      .compile
      .lastOrError
      .flatMap { case (duration, result) =>
        IO.println(s"Read+Parsing FIT file took: ${duration.toMillis}ms").as(result)
      }

  def printActivitySummary(fit: FitFile) =
    IO {
      val t1 = System.nanoTime()
      val a1 = ActivitySummary.from2(fit).fold(sys.error, identity)
      val t2 = Duration.ofNanos(System.nanoTime() - t1)
      println(s"Reading summary took: $t2")
      println(a1)
    }

  def printDecoded(fit: FitFile) =
    Stream
      .emits(fit.dataRecords)
      .filter(_.isKnownSuccess)
      .map(r =>
        r.definition.profileMsg.getOrElse(r.definition.globalMessageNumber) -> r.decoded
      )
      // .take(1000)
      .evalMap(IO.println)

  def printTestCases(fit: FitFile): Stream[IO, Unit] =
    Stream
      .emits(MesgNum.all.map(n => n -> fit.findData(n)))
      .filter { case (_, list) => list.nonEmpty }
      .map { case (mesg, data) =>
        println(
          s"""
             |test("decode $mesg data") {
             |  val data = ByteVector.fromValidHex("${data.head.raw.toHex}")
             |  val definition = io.circe.parser.decode[FitMessage.DefinitionMessage]($quotes${data.head.definition.asJson.noSpaces}$quotes).fold(throw _, identity)
             |  val profileMsg = definition.profileMsg.getOrElse(sys.error(s"no profile message"))
             |}
             |""".stripMargin
        )
      }
}
