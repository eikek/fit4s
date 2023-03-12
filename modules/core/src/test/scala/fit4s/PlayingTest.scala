package fit4s

import cats.effect._
import scodec.bits.ByteVector
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite {

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/activity/79AH0319.FIT"))
    fs2.io
      .readInputStream(fit, 8192)
      .chunks
      .map(c => ByteVector(c.toArray).toBitVector)
      .map(bv => (FileHeader.codec ~ Record.codec).decode(bv))
      .map(println)
      .compile
      .drain
      .unsafeRunSync()
  }
}
