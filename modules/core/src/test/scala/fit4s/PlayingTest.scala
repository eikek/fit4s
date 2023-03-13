package fit4s

import cats.effect._
import scodec.bits.ByteVector
import munit.CatsEffectSuite

class PlayingTest extends CatsEffectSuite {

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/monitor/78TH1430.FIT"))
    fs2.io
      .readInputStream(fit, 8192)
      .chunks
      .map(c => ByteVector(c.toArray).toBitVector)
      .map(bv => FitFile.codec.decode(bv))
      .map(println)
      .compile
      .drain
      .unsafeRunSync()
  }
}
