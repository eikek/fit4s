package fit4s.core.internal

import fit4s.core.data.LatLng
import fit4s.core.{LargeSample, TestSyntax}

import munit.FunSuite

class PolylineCodecTest extends FunSuite with TestSyntax:
  val c = PolylineCodec

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

  test("encode double"):
    values.foreach { case (num, str) =>
      assertEquals(
        c.encodeDoubles(List(num), 5),
        str
      )
    }

  test("encode single numbers in larger sample"):
    LargeSample.coordinates.zip(LargeSample.correctDecoded6).foreach { case (op, dp) =>
      val enc1 = c.encodeDoubles(List(op.lat), 6)
      val dec1 = c.decodeDoubles(enc1, 6).head
      assertEquals(dec1, dp.lat)

      val enc2 = c.encodeDoubles(List(op.lng), 6)
      val dec2 = c.decodeDoubles(enc2, 6).head
      assertEquals(dec2, dp.lng)
    }

    LargeSample.coordinatesDiff.zip(LargeSample.correctDecodedDiff6).take(2).foreach {
      case (op, dp) =>
        val enc1 = c.encode(List(op), 6)
        val dec1 = c.decode(enc1, 6).head
        assertEquals(dec1, dp)
    }

  test("encode some samples"):
    val testData = List(
      LargeSample.coordinates.take(
        10
      ) -> """_spnyA{vzvOdC\fC^~Bd@|Bf@|Bd@zBh@vBf@tBl@jBbA"""
    )
    testData.foreach { case (ps, enc) =>
      assertEquals(PolylineCodec.encode(ps, 6), enc)
      assertEquals(PolylineCodec.decode(enc, 6), LargeSample.correctDecoded6.take(10))
    }

  test("decode polyline"):
    val points =
      PolylineCodec.decode("""_spnyA{vzvOdC\fC^~Bd@|Bf@|Bd@zBh@vBf@tBl@jBbA""", 6)
    assertEquals(points, LargeSample.correctDecoded6.take(10))

    val enc = PolylineCodec.encode(LargeSample.coordinates, 6)
    assertEquals(enc, LargeSample.correctEncoded6)

  test("decode1"):
    for (n <- 1 to 43) {
      val dec = decodeN(LargeSample.correctEncoded6, 6).toVector.take(n)
      val expect = LargeSample.correctDecoded6.take(n)
      assertEquals(dec, expect)
    }
    val dec = decodeN(LargeSample.correctEncoded6, 6).toVector
    val expect = LargeSample.correctDecoded6.toVector
    assertEquals(dec, expect)

  test("decodeN"):
    for (n <- 1 to 34) {
      val (dec, _) =
        PolylineCodec.decodeN(LargeSample.correctEncoded6, 6, LatLng.zero, count = n).get
      val expect = LargeSample.correctDecoded6.take(n)
      assertEquals(dec, expect)
    }

  def decodeN(str: String, precision: Int): LazyList[LatLng] =
    PolylineCodec.decode1(str, precision, LatLng.zero) match
      case None            => LazyList.empty
      case Some((el, rem)) => el #:: decodeN(rem, precision)
