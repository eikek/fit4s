package fit4s.codec

import munit.FunSuite

class ByteSizeTest extends FunSuite:

  test("fromString"):
    assertEquals(ByteSize.fromString("4M").getOrElse(sys.error), ByteSize.mb(4))
    assertEquals(ByteSize.fromString("3.15k").getOrElse(sys.error), ByteSize.kibi(3.15))
    assertEquals(ByteSize.fromString("1.5G").getOrElse(sys.error), ByteSize.gb(1.5))
