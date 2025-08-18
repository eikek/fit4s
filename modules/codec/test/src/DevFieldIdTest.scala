package fit4s.codec

import munit.FunSuite

class DevFieldIdTest extends FunSuite:

  test("encode/decode"):
    for {
      devIdx <- 0 to 255
      num <- 0 to 255
    } {
      val key = DevFieldId(devIdx.toShort, num.toShort)
      val back = key.extracted
      assertEquals(back, devIdx.toShort -> num.toShort)
    }

  test("extract unknown"):
    for (n <- 0 to ((1 << 16) - 1))
      assertEquals(DevFieldId.fromInt(n), None)
