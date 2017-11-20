package fit4s

import fs2.{io, Task}
import scodec.bits.ByteVector
import org.scalatest._

class PlayingTest extends FlatSpec with Matchers {

  "codecs" should "encode and decode" in {
    val fit = Task.delay(getClass.getResourceAsStream("/fit/activity/79AH0319.FIT"))
    io.readInputStream(fit, 8192).
      chunks.
      map(c => ByteVector(c.toArray).toBitVector).
      map(bv => (FileHeader.codec ~ RecordHeader.codec).decode(bv)).
      map(println).
      run.
      unsafeRun
  }
}
