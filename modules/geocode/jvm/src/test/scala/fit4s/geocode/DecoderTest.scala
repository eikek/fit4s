package fit4s.geocode

import cats.syntax.all.*
import munit.*
import io.bullet.borer.Json
import fit4s.geocode.data.Address

class DecoderTest extends FunSuite with NominatimDecoder {

  test("decode optional fields in Address") {
    val jsonStr = """{"road": "Strasse", "city": "Winterthur"}"""

    val decoded = Json.decode(jsonStr.getBytes).to[Address].value
    assertEquals(
      decoded,
      Address.empty.copy(road = "Strasse".some, city = "Winterthur".some)
    )
  }

  test("decode address") {
    val jsonStr =
      """{"house_number":"2A","road":"Am Strasse","neighbourhood":"Neudorf","suburb":"Rout","city_district":"Kernstadt","town":"Teilingen","municipality":"Teilingen","county":"Landkreis","state":"Thüringen","ISO3166-2-lvl4":"DE-TH","postcode":"84654","country":"Deutschland","country_code":"de"} """

    val decoded = Json.decode(jsonStr.getBytes).to[Address].value
    assertEquals(
      decoded,
      Address(
        house_number = "2A".some,
        road = "Am Strasse".some,
        county = "Landkreis".some,
        city = "Teilingen".some,
        state = "Thüringen".some,
        postcode = "84654".some,
        country = "Deutschland".some,
        village = None,
        country_code = "de".some
      )
    )
  }
}
