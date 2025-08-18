package fit4s.core.internal

import scala.collection.mutable.ListBuffer

import fit4s.core.data.LatLng

object PolylineCodec:

  def encode(
      points: Iterable[LatLng],
      precision: Int,
      zero: LatLng = LatLng.zero
  ): String =
    val factor = math.pow(10, precision)
    val buf = new StringBuilder()
    points.foldLeft(zero) { (prev, current) =>
      val latDiff = roundTo(current.lat, factor) - roundTo(prev.lat, factor)
      val lngDiff = roundTo(current.lng, factor) - roundTo(prev.lng, factor)

      encodeSint(roundToInt(latDiff, factor), buf)
      encodeSint(roundToInt(lngDiff, factor), buf)
      current
    }
    buf.toString()

  def decode(str: String, precision: Int): Vector[LatLng] =
    decodeFold(str, precision, new ListBuffer[LatLng])((buf, p) => buf.append(p)).toVector

  def decodeFold[A](str: String, precision: Int, zero: A)(f: (A, LatLng) => A): A =
    val factor = math.pow(10, precision)
    val w = new Window()
    var prev = LatLng.zero
    var current = zero
    decodeInts0(
      str,
      n => {
        w.push(n / factor)
        if w.hasPair then
          val next = makeLatLng(w, prev, factor)
          val r = f(current, next)
          current = r
          prev = next
        true
      }
    )
    current

  def encodeDoubles(nums: Iterable[Double], precision: Int): String =
    val factor = math.pow(10, precision)
    val buf = new StringBuilder()
    nums.foreach { n =>
      encodeSint(roundToInt(n, factor), buf)
    }
    buf.toString

  def decodeDoubles(str: String, precision: Int): Vector[Double] =
    val factor = math.pow(10, precision)
    val buf = new ListBuffer[Double]
    decodeInts0(
      str,
      { n =>
        buf.append(roundTo(n / factor, factor)); true
      }
    )
    buf.toVector

  def decode1(str: String, precision: Int, previous: LatLng): Option[(LatLng, String)] =
    decodeN(str, precision, previous, 1).map(t => (t._1.head, t._2))

  def decodeN(
      str: String,
      precision: Int,
      previous: LatLng,
      count: Int,
      diffs: Boolean = false
  ): Option[(Vector[LatLng], String)] =
    require(count >= 0 && count < Int.MaxValue - 1, s"Invalid count: $count")
    if (str.isEmpty()) None
    else if count == 0 then None
    else
      val factor = math.pow(10, precision)
      val buf = new ListBuffer[LatLng]()
      val w = new Window()
      var prev = previous
      val index = decodeInts0(
        str,
        n =>
          w.push(n / factor)
          if w.hasPair then
            val next = makeLatLng(w, prev, factor)
            prev = next
            buf.append(next)
          buf.size < (if diffs then count else count + 1)
      )
      buf.size match
        case 0                        => None
        case n if n == count && diffs =>
          val result = buf.toVector
          Some((result, str.drop(index)))
        case n if n == count + 1 && !diffs =>
          val nextHead = buf.last
          buf.remove(count) // remove last element
          val result = buf.toVector
          val newFirst = encode(List(nextHead), precision, LatLng.zero)
          Some((result, newFirst ++ str.drop(index)))
        case n =>
          Some((buf.toVector, ""))

  private inline def roundTo(num: Double, factor: Double): Double =
    Math.round(num * factor) / factor

  private inline def roundToInt(num: Double, factor: Double): Int =
    Math.round(num * factor).toInt

  private inline def encodeSint(n: Int, buf: StringBuilder): Unit =
    val num = if (n < 0) ~(n << 1) else n << 1
    encodeUint(num, buf)

  private inline def encodeUint(n: Int, buf: StringBuilder): Unit =
    var value: Int = 0
    var num = n
    while (num >= 0x20) {
      value = (0x20 | (num & 0x1f)) + 63
      buf.append(value.toChar)
      num = num >> 5
    }
    value = num + 63
    buf.append(value.toChar)

  private inline def decodeInts0(str: String, cont: Int => Boolean): Int =
    var shift = 0
    var current = 0
    var index = 0
    var active = true
    val len = str.length()
    while (index < len && active) {
      val b = str.charAt(index).toInt - 63
      current = current | (b & 0x1f) << shift
      if b < 0x20 then
        active = cont(decodeSint(current))
        current = 0
        shift = 0
      else shift += 5
      index = index + 1
    }
    index

  private inline def decodeSint(n: Int) =
    if (n & 1) == 0 then n >> 1 else ~(n >> 1)

  private class Window {
    private var a: Double = Double.NaN
    private var b: Double = Double.NaN

    def push(n: Double) =
      if a.isNaN || (!a.isNaN && !b.isNaN) then
        a = n
        b = Double.NaN
      else if b.isNaN then b = n

    def hasPair: Boolean = !a.isNaN && !b.isNaN
    def pair: (Double, Double) = (a, b)
  }

  private inline def makeLatLng(w: Window, prev: LatLng, factor: Double) =
    val (a, b) = w.pair
    val lat = roundTo(a + prev.lat, factor)
    val lng = roundTo(b + prev.lng, factor)
    LatLng(lat, lng)
