package fit4s.codec

import fit4s.codec.StreamDecoder.FitPart

import munit.FunSuite
import scodec.Err

class StreamDecoderTest extends FunSuite:

  test("stop on demand"):
    val consumer = new StreamDecoderTest.TestConsumer {
      override def onPart(part: FitPart): Boolean =
        super.onPart(part)
        false
    }
    StreamDecoder.decode(TestData.Activities.edge146.source, consumer)
    assertEquals(consumer.parts.size, 1)
    assert(consumer.doneCalled, s"onDone was not called")

  test("call onDone after all"):
    val consumer = new StreamDecoderTest.TestConsumer()
    StreamDecoder.decode(TestData.Activities.edge146.source, consumer)
    assertEquals(consumer.parts.size, 5654)
    assert(consumer.doneCalled, s"onDone was not called")

object StreamDecoderTest:

  class TestConsumer extends StreamDecoder.FitPartConsumer:
    var doneCalled: Boolean = false
    var error: Option[Err] = None
    var parts: List[FitPart] = Nil
    def onDone(): Unit =
      doneCalled = true
    def onPart(part: FitPart): Boolean =
      parts = part :: parts
      true
    def onError(err: Err): Unit =
      error = Some(err)
