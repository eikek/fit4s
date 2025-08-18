package fit4s.codec

import munit.FunSuite

class ValueAdjustTest extends FunSuite:

  test("apply all"):
    val data: Vector[FitBaseValue] = Vector(15L, 10L, 0L, 0L, 0L)
    val one = ValueAdjust.from(List(10.0), List(0), data.size)
    val result = ValueAdjust.applyAll(FitBaseType.Uint16, one, data)
    assert(
      result.forall(_.isInstanceOf[Double]),
      s"Not all values are doubles: ${result}"
    )
    assertEquals(result, Vector(1.5d, 1.0d, 0d, 0d, 0d))
