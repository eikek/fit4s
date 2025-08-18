package fit4s.core.data

import fit4s.core.LargeSample
import fit4s.core.TestSyntax

import munit.FunSuite

class PolylineTest extends FunSuite with TestSyntax:
  val cfg = Polyline.Config()

  val p1 = LatLng(38.5, -120.2)
  val p2 = LatLng(40.7, -120.95)
  val p3 = LatLng(43.252, -126.453)

  val values = List(
    -179.9832104 -> "`~oia@",
    -120.2 -> "~ps|U",
    0d -> "?",
    38.5 -> "_p~iF",
    -6.64684921503067e-5 -> "L"
  )

  test("encode polyline"):
    val ppl = Polyline(p1, p2, p3)
    assertEquals(ppl.size, 3)
    assertEquals(ppl.firstPosition, Some(p1))
    assertEquals(ppl.lastPosition, Some(p3))

    assertEquals(ppl.encoded, "_p~iF~ps|U_ulLnnqC_mqNvxq`@")
    val ppl2 = Polyline(p1, p2)
    assertEquals(ppl2.size, 2)
    assertEquals(ppl2.encoded, "_p~iF~ps|U_ulLnnqC")
    assertEquals(ppl2.firstPosition, Some(p1))
    assertEquals(ppl2.lastPosition, Some(p2))

  test("add to polyline"):
    val ppl = Polyline(p1, p2)
    val expect = Polyline(p1, p2, p3)
    val added = ppl.add(p3)
    assertEquals(added, expect)
    assertEquals(Polyline.empty.add(p1).add(p2).add(p3), ppl.add(p3))
    // not add same position
    assertEquals(ppl.add(p2), ppl)

  test("prepend to polyline"):
    val ppl = Polyline(p2, p3)
    val expect = Polyline(p1, p2, p3)
    val added = ppl.prepend(p1)
    assertEquals(added, expect)
    // not prepend same pos
    assertEquals(added.prepend(p1), added)

  test("concat polyline"):
    val pp1 = Polyline(p1, p2)
    val pp2 = Polyline(p3)
    assertEquals(pp1 ++ pp2, Polyline(p1, p2, p3))
    assertEquals(pp2 ++ pp1, Polyline(p3, p1, p2))

  // test("iterator"):
  //   val ppl = Polyline(p1, p2, p3)
  //   val it = ppl.iterator
  //   assertEquals(it.next(), p1)
  //   assertEquals(it.next(), p2)
  //   assertEquals(it.next(), p3)
  //   assert(it.hasNext == false)

  test("encode/decode roundtrip"):
    val ppl = Polyline(p1, p2, p3)
    val ppx = Polyline.decode(ppl.encoded)
    assertEquals(ppx, ppl)

    val pll = Polyline(LargeSample.correctDecoded5*)
    val plx = Polyline.decode(pll.encoded)
    assertEquals(pll, plx)

  test("random stuff"):
    assert(Polyline.decode("$~=öpüöäp").nonEmpty)
    assert(Polyline.decode("😏🐶").isEmpty)

  test("decodeN"):
    assertEquals(Polyline.empty.decodeN(3), None)
    assertEquals(Polyline(p1).decodeN(2), Some(Vector(p1) -> Polyline.empty))
    assertEquals(Polyline(p1).decodeN(0), None)
    val pl = Polyline(p1, p2, p3)
    assertEquals(pl.decodeN(0), None)
    assertEquals(pl.decodeN(1), Some(Vector(p1) -> Polyline(p2, p3)))
    assertEquals(pl.decodeN(2), Some(Vector(p1, p2) -> Polyline(p3)))
    assertEquals(pl.decodeN(3), Some(Vector(p1, p2, p3) -> Polyline.empty))
    assertEquals(pl.decodeN(5), Some(Vector(p1, p2, p3) -> Polyline.empty))

    val plc = LargeSample.correctDecoded5
    val pll = Polyline(plc*)
    assertEquals(pll.toLatLngs, plc)
    assertEquals(pll.decodeN(23), Some(plc.take(23) -> Polyline(plc.drop(23)*)))
