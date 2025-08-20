package fit4s.core.internal

import fit4s.core.LargeSample
import fit4s.core.data.Distance
import fit4s.core.data.Polyline

import munit.FunSuite

class PolylineUtilsTest extends FunSuite:

  test("empty iterator"):
    val iter = PolylineUtils.latngIterator(Polyline.empty, 10)
    assert(!iter.hasNext, s"expected empty iterator, got ${iter.toList}")

  test("small iterator"):
    val pl = Polyline(LargeSample.coordinates.take(5)*)
    val iter = PolylineUtils.latngIterator(pl, 10)
    assertEquals(iter.toVector, pl.toLatLngs)

  test("chunked iterator"):
    val pl = Polyline(LargeSample.coordinates*)
    val iter = PolylineUtils.latngIterator(pl, 80)
    assertEquals(iter.toVector, pl.toLatLngs)

  test("empty distance"):
    assertEquals(PolylineUtils.distance(Polyline.empty, 10), Distance.zero)

  test("single point distance"):
    assertEquals(
      PolylineUtils.distance(Polyline(LargeSample.coordinates.head), 10),
      Distance.zero
    )

  test("distance"):
    val pl = Polyline(LargeSample.coordinates*)
    val dst = PolylineUtils.distance(pl, 30)
    assertEquals(dst.roundTo(6), Distance.meter(850.818555))
