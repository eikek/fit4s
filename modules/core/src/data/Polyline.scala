package fit4s.core.data

import java.nio.charset.StandardCharsets

import fit4s.core.internal.PolylineCodec
import fit4s.core.internal.PolylineUtils

import scodec.bits.ByteVector

/** A polyline storing sequence of coordinates in a compressed form.
  *
  * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
  */
sealed trait Polyline:
  self =>

  def cfg: Polyline.Config
  def add(pos: LatLng): Polyline
  infix def :+(pos: LatLng): Polyline = add(pos)

  def prepend(pos: LatLng): Polyline

  def ++(next: Polyline): Polyline

  def isEmpty: Boolean
  def nonEmpty: Boolean = !isEmpty
  def size: Int

  def asBytes: ByteVector

  def encoded: String

  def firstPosition: Option[LatLng]
  def lastPosition: Option[LatLng]
  def toLatLngs: Vector[LatLng]

  /** Decodes the next `n` coordinates from this polyline. Returns the remaining polyline.
    * Note that this involves encoding the new head of the remaining list, so it is not
    * efficient if this is called many times with a low count. Rather get some larger
    * chunks for processing.
    *
    * The resulting vector may contain less than `n` elements if no more are available, it
    * is never empty however. None is returned for an empty list.
    */
  def decodeN(n: Int): Option[(Vector[LatLng], Polyline)]

  /** Creates an iterator that decodes `chunkSize` coordinates at once. */
  final def iterator(chunkSize: Int = 100): Iterator[LatLng] =
    PolylineUtils.latngIterator(this, chunkSize)

  /** Calculates the distance of this polyline. */
  final def distance: Distance =
    PolylineUtils.distance(this, 100)

object Polyline:
  private val c = PolylineCodec

  opaque type Precision = Int
  object Precision:
    val low: Precision = 4
    val middle: Precision = 5
    val high: Precision = 6
    val highest: Precision = 7
    val default: Precision = middle

    extension (p: Precision) def toInt: Int = p

  /** @param precision
    *   the precision for encoding/decoding
    * @param skipNext
    *   whether to skip adding the next latlng. it is a functon `(next, previous) =>
    *   Boolean`.
    */
  final case class Config(
      precision: Precision = 5,
      skipNext: (LatLng, LatLng) => Boolean = _ == _
  ):
    def withPrecision(p: Precision): Config = copy(precision = p)
    def middlePrecision = withPrecision(Precision.middle)
    def highPrecision = withPrecision(Precision.high)
    def lowPrecision = withPrecision(Precision.low)
    def round(pos: LatLng): LatLng = c.roundTo(pos, precision)

  object Config:
    given Config = Config()

  def empty(using cfg: Config): Polyline = Empty(cfg)

  def positions(pos: Position*)(using Config): Polyline =
    apply(pos.map(_.toLatLng)*)

  def apply(pos: LatLng*)(using cfg: Config): Polyline =
    pos match
      case Seq()          => empty
      case a +: Seq()     => One(cfg.round(a), cfg)
      case a +: b +: tail => Many.from(cfg.round(a), cfg.round(b), tail*)

  def decode(encoded: String)(using cfg: Config): Polyline =
    val str = encoded.trim()
    if (str.isEmpty()) Empty(cfg)
    else apply(toLatLng(encoded, cfg.precision)*)

  def toLatLng(encoded: String, precision: Precision = 5): Vector[LatLng] =
    PolylineCodec.decode(encoded, precision)

  def encode(points: Iterable[LatLng], precision: Precision = 5): String =
    c.encode(points, precision)

  final case class Empty(cfg: Config) extends Polyline:
    def add(pos: LatLng): Polyline = One(pos, cfg)
    def prepend(pos: LatLng): Polyline = add(pos)
    def ++(next: Polyline) = next
    val isEmpty: Boolean = true
    val size = 0
    val asBytes: ByteVector = ByteVector.empty
    val iterator: Iterator[LatLng] = Iterator.empty
    val firstPosition: Option[LatLng] = None
    val lastPosition: Option[LatLng] = None
    val toLatLngs = Vector.empty
    def decodeN(n: Int): Option[(Vector[LatLng], Polyline)] = None
    override val encoded = ""
    def withPrecision(p: Precision): Polyline = Empty(cfg.withPrecision(p))

  sealed trait NonEmpty extends Polyline:
    val isEmpty = false
    def first: LatLng
    def last: LatLng

  private case class One(pos: LatLng, cfg: Config) extends NonEmpty:
    val size = 1
    def add(next: LatLng): Polyline =
      if cfg.skipNext(next, pos) then this
      else Many.from(pos, next)(using cfg)
    def prepend(prev: LatLng): Polyline =
      if cfg.skipNext(pos, prev) then this
      else Many.from(prev, pos)(using cfg)
    def ++(next: Polyline): Polyline = next.prepend(pos)
    lazy val asBytes = ByteVector.view(encoded.getBytes(StandardCharsets.UTF_8))
    lazy val encoded: String = c.encode(List(pos), cfg.precision.toInt)
    val toLatLngs = Vector(pos)
    val firstPosition: Option[LatLng] = Some(pos)
    val lastPosition: Option[LatLng] = Some(pos)
    val first = pos
    val last = pos
    def decodeN(n: Int): Option[(Vector[LatLng], Polyline)] =
      if n <= 0 then None
      else Some(Vector(pos) -> Empty(cfg))

  /** Keep track of last element for quicker append/prepend. The last is redundant as it
    * is in the byte vector as well. The first element is not.
    */
  private case class Many(
      diffs: String,
      first: LatLng,
      last: LatLng,
      size: Int,
      cfg: Config
  ) extends NonEmpty:
    private def encodeDiff(next: LatLng, prev: LatLng) =
      c.encode(List(next), cfg.precision.toInt, zero = prev)
    def withPrecision(p: Precision) =
      if p > cfg.precision then copy(cfg = cfg.withPrecision(p))
      else this

    def add(next: LatLng): Polyline =
      if cfg.skipNext(next, last) then this
      else Many(diffs.concat(encodeDiff(next, last)), first, next, size + 1, cfg)

    def prepend(pos: LatLng): Polyline =
      if cfg.skipNext(first, pos) then this
      else Many(encodeDiff(first, pos).concat(diffs), pos, last, size + 1, cfg)

    def ++(next: Polyline): Polyline = next match
      case Empty(ec)   => withPrecision(math.max(ec.precision, cfg.precision))
      case One(pos, p) => add(pos)
      case Many(bb, bFirst, bLast, sz, mc) =>
        Many(
          diffs.concat(encodeDiff(bFirst, last)).concat(bb),
          first,
          bLast,
          size + sz,
          cfg
        )
          .withPrecision(mc.precision)

    def encoded: String =
      c.encode(List(first), cfg.precision.toInt).concat(diffs)

    def toLatLngs = c.decode(encoded, cfg.precision.toInt)

    def asBytes: ByteVector = ByteVector.view(encoded.getBytes(StandardCharsets.UTF_8))

    val firstPosition: Option[LatLng] = Some(first)
    val lastPosition: Option[LatLng] = Some(last)

    def decodeN(n: Int): Option[(Vector[LatLng], Polyline)] =
      PolylineCodec
        .decodeN(diffs, cfg.precision, first, count = n, diffs = true)
        .map {
          case (latlngs, "") =>
            if n >= size then (first +: latlngs, Empty(cfg))
            // latngs.last == last
            else (first +: latlngs.init) -> One(last, cfg)

          case (Vector(a), remain) =>
            (Vector(first), Many(remain, a, last, size - 1, cfg))

          case (latlngs, remain) =>
            // if remain is not empty, the last element is not decoded
            val (result, newFirst) = (latlngs.init, latlngs.last)
            val next = Many(remain, newFirst, last, size = size - n, cfg)
            (first +: result, next)
        }

  private object Many:
    def from(p1: LatLng, p2: LatLng, more: LatLng*)(using
        cfg: Config
    ): Many =
      if more.isEmpty then
        Many(c.encode(List(p2), cfg.precision.toInt, zero = p1), p1, p2, 2, cfg)
      else
        val diffs = c.encode(p2 +: more, cfg.precision.toInt, zero = p1)
        Many(diffs, p1, more.last, more.size + 2, cfg)
