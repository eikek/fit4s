package fit4s.core.data

import fit4s.core.GetFieldNumber
import fit4s.core.MessageReader
import fit4s.profile.SessionMsg

final case class BBox(northEast: Position, southWest: Position):
  private val ng = Numeric[Semicircle]

  def distance: Distance = northEast.distance(southWest)

  def rectangle(using Polyline.Config): Polyline =
    Polyline.positions(
      northEast,
      Position(southWest.latitude, northEast.longitude),
      southWest,
      Position(northEast.latitude, southWest.longitude),
      northEast
    )

  def contains(p: LatLng): Boolean = contains(p.toPosition)
  def contains(p: Position): Boolean =
    northEast.latitude >= p.latitude &&
      northEast.longitude >= p.longitude &&
      southWest.latitude <= p.latitude &&
      southWest.longitude <= p.longitude

  /** Creates a bounding box containing this and other. */
  def union(other: BBox): BBox =
    val neLat = ng.max(northEast.latitude, other.northEast.latitude)
    val neLng = ng.max(northEast.longitude, other.northEast.longitude)
    val swLat = ng.min(southWest.latitude, other.southWest.latitude)
    val swLng = ng.min(southWest.longitude, other.southWest.longitude)
    BBox(Position(neLat, neLng), Position(swLat, swLng))

  /** Return a bounding box that includes the given position. */
  def include(p: Position): BBox =
    if contains(p) then this
    else
      val neLat = ng.max(northEast.latitude, p.latitude)
      val neLng = ng.max(northEast.longitude, p.longitude)
      val swLat = ng.min(southWest.latitude, p.latitude)
      val swLng = ng.min(southWest.longitude, p.longitude)
      BBox(Position(neLat, neLng), Position(swLat, swLng))

  def include(p: LatLng): BBox = include(p.toPosition)

object BBox:

  def fromPositions(positions: Iterable[Position]): Option[BBox] =
    positions.headOption.map { head =>
      val ng = Numeric[Semicircle]
      var (neLat, neLng, swLat, swLng) =
        (head.latitude, head.longitude, head.latitude, head.longitude)

      positions.drop(1).foreach { p =>
        neLat = ng.max(neLat, p.latitude)
        neLng = ng.max(neLng, p.longitude)
        swLat = ng.min(swLat, p.latitude)
        swLng = ng.min(swLng, p.longitude)
      }
      BBox(Position(neLat, neLng), Position(swLat, swLng))
    }

  def fromLatLngs(positions: Iterable[LatLng]): Option[BBox] =
    positions.headOption.map { head =>
      var (neLat, neLng, swLat, swLng) = (head.lat, head.lng, head.lat, head.lng)
      positions.drop(1).foreach { p =>
        neLat = math.max(neLat, p.lat)
        neLng = math.max(neLng, p.lng)
        swLat = math.min(swLat, p.lat)
        swLng = math.min(swLng, p.lng)
      }
      BBox(Position.degree(neLat, neLng), Position.degree(swLat, swLng))
    }

  def messageReader[
      NL: GetFieldNumber,
      NN: GetFieldNumber,
      SL: GetFieldNumber,
      SN: GetFieldNumber
  ](necLat: NL, necLng: NN, swcLat: SL, swcLng: SN): MessageReader[BBox] =
    (Position.reader(necLat, necLng) :: Position.reader(swcLat, swcLng).tuple).as[BBox]

  /** MessageReader for the SessionMsg. */
  val sessionReader: MessageReader[BBox] =
    MessageReader.forMsg(SessionMsg) { m =>
      messageReader(m.necLat, m.necLong, m.swcLat, m.swcLong)
    }
