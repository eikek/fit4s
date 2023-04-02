package fit4s.data

final case class Position(latitude: Semicircle, longitude: Semicircle)

object Position {

  def optional(
      latitude: Option[Semicircle],
      longitude: Option[Semicircle]
  ): Option[Position] =
    latitude.flatMap(lat => longitude.map(lng => Position(lat, lng)))
}
