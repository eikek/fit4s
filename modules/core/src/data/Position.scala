package fit4s.core.data

import fit4s.core.GetFieldNumber
import fit4s.core.MessageReader

final case class Position(latitude: Semicircle, longitude: Semicircle):
  infix def -(p: Position): Position =
    Position(latitude - p.latitude, longitude - p.longitude)

  infix def +(p: Position): Position =
    Position(latitude + p.latitude, longitude + p.longitude)

  def toLatLng: LatLng = LatLng(latitude.toDegree, longitude.toDegree)

  def toRadian: (Double, Double) =
    (latitude.toRadian, longitude.toRadian)

  def distance(other: Position): Distance =
    Distance.km(Position.calculateDistanceInKilometer(this, other))

  def isWithin(other: Position, delta: Distance): Boolean =
    distance(other) <= delta

  def inRange(other: Position, delta: Semicircle): Boolean =
    val d1 = Math.abs(latitude.toSemicircle - other.latitude.toSemicircle)
    val d2 = Math.abs(longitude.toSemicircle - other.longitude.toSemicircle)
    return d1 <= delta.toSemicircle && d2 <= delta.toSemicircle

object Position:
  val zero: Position = Position(Semicircle.semicircle(0), Semicircle.semicircle(0))

  def degree(lat: Double, lng: Double): Position =
    Position(Semicircle.degree(lat), Semicircle.degree(lng))

  def latlng(p: LatLng): Position =
    degree(p.lat, p.lng)

  def optional(
      latitude: Option[Semicircle],
      longitude: Option[Semicircle]
  ): Option[Position] =
    latitude.flatMap(lat => longitude.map(lng => Position(lat, lng)))

  def reader[A: GetFieldNumber, B: GetFieldNumber](
      lat: A,
      lng: B
  ): MessageReader[Position] =
    (MessageReader.field[A](lat).as[Semicircle] ::
      MessageReader.field[B](lng).as[Semicircle].tuple).as[Position]

  private def calculateDistanceInKilometer(p1: Position, p2: Position): Double =
    LatLng.calculateDistanceInKilometer(p1.toRadian, p2.toRadian)

  given (using s: Display[Semicircle]): Display[Position] =
    Display.instance(p => s"Position(${s.show(p.latitude)}, ${s.show(p.longitude)})")
