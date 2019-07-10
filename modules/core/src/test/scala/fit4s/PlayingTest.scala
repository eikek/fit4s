package fit4s

import fs2._
import cats.effect._
import scodec.bits.ByteVector
import minitest._

object PlayingTest extends SimpleTestSuite {

  implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
  val blocker = Blocker.liftExecutionContext(scala.concurrent.ExecutionContext.global)

  test("codecs should encode and decode") {
    val fit = IO(getClass.getResourceAsStream("/fit/activity/79AH0319.FIT"))
    io.readInputStream(fit, 8192, blocker).
      chunks.
      map(c => ByteVector(c.toArray).toBitVector).
      map(bv => (FileHeader.codec ~ Record.codec).decode(bv)).
      map(println).
      compile.drain.
      unsafeRunSync
  }
}
