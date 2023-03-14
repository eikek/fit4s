package fit4s

import cats.effect._
import fs2.Chunk
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite {

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/activity/796A4003.FIT"))
    fs2.io
      .readInputStream(fit, 8192)
      .chunks
      .fold(Chunk.empty[Byte])(_ ++ _)
      .map(_.toBitVector)
      .map(bv => FitFile.codec.decode(bv))
      .map(_.toEither.left.map(e => new Exception(e.messageWithContext)))
      .rethrow
      .map(_.value)
      .evalMap(fit => IO.println(s"Record count: ${fit.records.size}\nFit file: $fit"))
      .compile
      .drain
      .unsafeRunSync()
  }
}
