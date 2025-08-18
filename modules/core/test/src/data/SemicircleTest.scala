package fit4s.core.data

import munit.FunSuite

class SemicircleTest extends FunSuite:

  val v1 = Semicircle.semicircle(566561821)
  val v2 = Semicircle.semicircle(104585524)

  test("conversions"):
    assertEquals(v1.toRadian, Math.toRadians(v1.toDegree))
    assertEquals(v1, Semicircle.degree(v1.toDegree))
