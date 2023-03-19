package fit4s

import cats.effect._
import io.circe.syntax._
import fit4s.profile.types.MesgNum
import fs2.{Chunk, Stream}
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite with JsonCodec {
  val quotes = "\"\"\""

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/activity/2023-03-16-06-25-37.fit"))
    fs2.io
      .readInputStream(fit, 8192)
      .chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .map(_.toBitVector)
      .map(bv => FitFile.codec.decode(bv))
      .map(_.toEither.left.map(e => new Exception(e.messageWithContext)))
      .rethrow
      .map(_.value)
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
      .evalTap(dr =>
        dr.definition.profileMsg match {
          case Some(_) => IO.unit
          case None =>
            IO.println(
              s"No profile message found. MesgNum=${dr.definition.globalMessageNumber}"
            )
        }
      )
      .map(r => r.definition.profileMsg -> r.decoded)
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
