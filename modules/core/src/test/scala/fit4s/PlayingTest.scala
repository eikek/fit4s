package fit4s

import cats.effect._
import io.circe.syntax._
import fit4s.profile.types.MesgNum
import fs2.{Chunk, Stream}
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite with JsonCodec {
  val quotes = "\"\"\""

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/activity/796A4003.FIT"))
    fs2.io
      .readInputStream(fit, 8192)
      .chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .evalMap(bv => IO(FitFile.decodeUnsafe(bv.toByteVector)))
      .evalTap(fit =>
        IO.println(
          s"Record count: ${fit.records.size}\nFit file: ${fit.toString.take(180)}...\n"
        )
      )
      .flatMap(printDecoded)
      .compile
      .drain
      .unsafeRunSync()
  }

  def printDecoded(fit: FitFile) =
    Stream
      .emits(fit.dataRecords)
      .filter(_.isKnownSuccess)
      .map(r =>
        r.definition.profileMsg.getOrElse(r.definition.globalMessageNumber) -> r.decoded
      )
      .take(1000)
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
