package fit4s.core.internal

import fit4s.core.LargeSample
import fit4s.core.data.Distance
import fit4s.core.data.Polyline

import munit.FunSuite

class PolylineUtilsTest extends FunSuite:
  val cfg = Polyline.Config()

  test("empty iterator"):
    val iter = PolylineUtils.latngIterator(Polyline.empty(cfg), 10)
    assert(!iter.hasNext, s"expected empty iterator, got ${iter.toList}")

  test("small iterator"):
    val pl = Polyline(cfg)(LargeSample.coordinates.take(5)*)
    val iter = PolylineUtils.latngIterator(pl, 10)
    assertEquals(iter.toVector, pl.toLatLngs)

  test("chunked iterator"):
    val pl = Polyline(cfg)(LargeSample.coordinates*)
    val iter = PolylineUtils.latngIterator(pl, 80)
    assertEquals(iter.toVector, pl.toLatLngs)

  test("empty distance"):
    assertEquals(PolylineUtils.distance(Polyline.empty(cfg), 10), Distance.zero)

  test("single point distance"):
    assertEquals(
      PolylineUtils.distance(Polyline(cfg)(LargeSample.coordinates.head), 10),
      Distance.zero
    )

  test("distance"):
    val pl = Polyline(cfg)(LargeSample.coordinates*)
    val dst = PolylineUtils.distance(pl, 30)
    assertEquals(dst.roundTo(6), Distance.meter(850.818555))
