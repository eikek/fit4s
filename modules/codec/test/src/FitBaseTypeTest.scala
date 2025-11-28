package fit4s.codec

import munit.FunSuite
import scodec.bits.BitVector
import scodec.bits.ByteOrdering

class FitBaseTypeTest extends FunSuite:

  test("decode sint8"):
    val codec = FitBaseType.Sint8.codec(ByteOrdering.BigEndian, ByteSize.bits(8))
    val result = codec.decode(BitVector.fromValidHex("ff")).require.value
    assertEquals(result, Vector(-1))
