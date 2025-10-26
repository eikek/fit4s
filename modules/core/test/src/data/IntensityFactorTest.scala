package fit4s.core.data

import munit.FunSuite

class IntensityFactorTest extends FunSuite:

  test("combine 1"):
    val factors = List(
      Duration.secs(120) -> IntensityFactor.iff(0.9),
      Duration.secs(120) -> IntensityFactor.iff(0.9)
    )
    assertEquals(IntensityFactor.combine(factors), Some(IntensityFactor.iff(0.9)))

  test("combine 2"):
    val factors = List(
      Duration.secs(120) -> IntensityFactor.iff(0.9),
      Duration.secs(60) -> IntensityFactor.iff(0.5)
    )
    val expect = (120 * 0.9 + 60 * 0.5) / 180.0
    assertEquals(IntensityFactor.combine(factors), Some(IntensityFactor.iff(expect)))
