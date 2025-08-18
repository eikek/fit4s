package fit4s.core.data

final case class LatLng(lat: Double, lng: Double):
  infix def -(p: LatLng): LatLng =
    LatLng(lat - p.lat, lng - p.lng)

  infix def +(p: LatLng): LatLng =
    LatLng(lat + p.lat, lng + p.lng)

  def toPosition: Position = Position(Semicircle.degree(lat), Semicircle.degree(lng))

  def toRadian: (Double, Double) = (Math.toRadians(lat), Math.toRadians(lng))

  def distance(other: LatLng): Distance =
    Distance.km(LatLng.calculateDistanceInKilometer(this.toRadian, other.toRadian))

  def isWithin(other: LatLng, delta: Distance): Boolean =
    distance(other) <= delta

  def inRange(other: LatLng, delta: Double): Boolean =
    val d1 = Math.abs(lat - other.lat)
    val d2 = Math.abs(lng - other.lng)
    return d1 <= delta && d2 <= delta

  def tuple: (Double, Double) = (lat, lng)

object LatLng:
  val zero: LatLng = LatLng(0, 0)

  def fromPosition(p: Position): LatLng =
    LatLng(p.latitude.toDegree, p.longitude.toDegree)

  private[data] def calculateDistanceInKilometer(
      p1: (Double, Double),
      p2: (Double, Double)
  ): Double = {
    val (lat1, lng1) = p1
    val (lat2, lng2) = p2
    val latDst = lat1 - lat2
    val lngDst = lng1 - lng2
    val sinLat = Math.sin(latDst / 2)
    val sinLng = Math.sin(lngDst / 2)
    val a = sinLat * sinLat +
      (Math.cos(lat1) *
        Math.cos(lat2) *
        sinLng * sinLng)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    6371 * c
  }

  given Display[LatLng] = Display.instance(_.toString)
