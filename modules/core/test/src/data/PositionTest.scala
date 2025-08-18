package fit4s.core.data

import fit4s.core.TestSyntax

import munit.FunSuite

class PositionTest extends FunSuite with TestSyntax:
  val p1 = Position.degree(38.5, -120.2)
  val p2 = Position.degree(40.7, -120.95)
  val p3 = Position.degree(43.252, -126.453)

  test("distance"):
    val dst = p1.distance(p2)
    assertEquals(dst.toMeter.toInt, 252924)
    assertEquals(p1.distance(p3), p3.distance(p1))

  test("isWithin"):
    val dst = p1.distance(p2)
    val meter2 = Distance.meter(2)
    assert(p1.isWithin(p2, dst))
    assert(p1.isWithin(p2, dst + meter2))
    assert(!p1.isWithin(p2, dst - meter2))

  test("inRange"):
    val delta1 = Semicircle.semicircle(100)
    val delta2 = Semicircle.semicircle(50)
    val delta = delta1 + delta2
    val p1 = Position.degree(41.48868, 8.76636)
    val p2 = p1.copy(latitude = p1.latitude + delta)
    val p3 = p1.copy(longitude = p1.longitude - delta)
    assert(p1.inRange(p2, delta1 * 2), s"Not in range: ($p1 <-> $p2) +-${delta1 * 2}")
    assert(p1.inRange(p3, delta2 * 4.5), s"Not in range: ($p1 <-> $p2 +-${delta2 * 2.5}")
    assert(!p1.inRange(p2, delta2))
