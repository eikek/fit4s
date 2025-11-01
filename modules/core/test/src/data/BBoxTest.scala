package fit4s.core.data

import fit4s.core.LargeSample

import munit.FunSuite

class BBoxTest extends FunSuite:
  val cfg = Polyline.Config()

  def p(lat: Double, lng: Double): Position =
    Position.degree(lat, lng)

  def sc(n: Long): Semicircle = Semicircle.semicircle(n)

  test("union"):
    val b1 = BBox(p(47.44070, 8.77994), p(47.44010, 8.77800))
    val b2 = BBox(p(47.44170, 8.77954), p(47.43810, 8.77200))

    val bu = b1.union(b2)
    assertEquals(
      bu,
      BBox(Position(b2.northEast.latitude, b1.northEast.longitude), b2.southWest)
    )

  test("contains"):
    val b1 = BBox(p(47.44070, 8.77994), p(47.44010, 8.77800))
    assert(b1.contains(p(47.44069, 8.77844)))
    assert(!b1.contains(p(47.44071, 7.5466)))

  test("include"):
    val b1 = BBox(p(47.44070, 8.77994), p(47.44010, 8.77800))
    val pos = p(47.44071, 7.5466)
    val b2 = b1.include(pos)
    assertEquals(b2, BBox(p(47.44071, 8.77994), p(47.44010, 7.5466)))
    assert(b2.contains(pos))

  test("rectangle"):
    val b1 = BBox(p(47.44071, 8.77994), p(47.44011, 8.77801))
    val line = List(
      LatLng(47.44071, 8.77994),
      LatLng(47.44011, 8.77994),
      LatLng(47.44011, 8.77801),
      LatLng(47.44071, 8.77801),
      LatLng(47.44071, 8.77994)
    )
    assertEquals(b1.rectangle.map(_.toLatLng.withPrecision(5)), line)

  test("from"):
    val bb = BBox.fromPositions(LargeSample.coordinates.map(_.toPosition)).get
    LargeSample.coordinates.foreach(ll =>
      assert(bb.contains(ll), s"$bb did not contain ${ll.toPosition}")
    )
    assertEquals(
      bb,
      BBox(Position(sc(565989639), sc(104796057)), Position(sc(565919016), sc(104742251)))
    )

    val bb2 = BBox.fromLatLngs(LargeSample.coordinates).get
    assertEquals(bb2, bb)
