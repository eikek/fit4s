package fit4s

import cats.effect._
import io.circe.syntax._
import fit4s.profile.basetypes.MesgNum
import fs2.{Chunk, Stream}
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite with JsonCodec {
  val quotes = "\"\"\""

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/Settings.fit"))
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
      .flatMap(fit => Stream.emits(MesgNum.all.map(n => n -> fit.findData(n))))
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
      .compile
      .drain
      .unsafeRunSync()
  }
}
