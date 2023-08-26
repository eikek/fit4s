package fit4s.geocode.data

final case class Address(
    house_number: Option[String],
    road: Option[String],
    village: Option[String],
    city: Option[String],
    county: Option[String],
    state: Option[String],
    country_code: Option[String],
    country: Option[String],
    postcode: Option[String]
)

object Address {
  val empty = Address(None, None, None, None, None, None, None, None, None)

  def fromMap(m: Map[String, String]): Address =
    Address(
      house_number = m.get("house_number"),
      road = m.get("road"),
      village = m.get("village"),
      city = m.get("city").orElse(m.get("town")),
      county = m.get("county"),
      state = m.get("state"),
      country_code = m.get("country_code"),
      country = m.get("country"),
      postcode = m.get("postcode")
    )
}
