package fit4s.geocode

case class Address(
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

}
