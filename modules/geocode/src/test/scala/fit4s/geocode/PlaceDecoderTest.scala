package fit4s.geocode

import fit4s.data.Semicircle
import munit.FunSuite
import io.circe.parser

class PlaceDecoderTest extends FunSuite with NominatimDecoder {

  test("decode place") {
    val jsonString =
      """
        |{"place_id":224823312,"licence":"Data © OpenStreetMap contributors, ODbL 1.0. https://osm.org/copyright","osm_type":"way","osm_id":566770669,"lat":"47.457635249999996","lon":"8.424877484038056","display_name":"3, Trüebenbachweg, Buchs (ZH), Bezirk Dielsdorf, Zurich, 8107, Switzerland","address":{"house_number":"3","road":"Trüebenbachweg","village":"Buchs (ZH)","county":"Bezirk Dielsdorf","state":"Zurich","ISO3166-2-lvl4":"CH-ZH","postcode":"8107","country":"Switzerland","country_code":"ch"},"boundingbox":["47.457551","47.4576987","8.4247722","8.4249838"]}""".stripMargin

    val decodedPlace =
      parser.decode[Place](jsonString)

    assertEquals(
      decodedPlace.fold(throw _, identity),
      Place(
        PlaceId(224823312),
        Semicircle.degree(47.457635249999996),
        Semicircle.degree(8.424877484038056),
        "3, Trüebenbachweg, Buchs (ZH), Bezirk Dielsdorf, Zurich, 8107, Switzerland",
        Address(
          Some(3),
          Some("Trüebenbachweg"),
          Some("Buchs (ZH)"),
          None,
          Some("Bezirk Dielsdorf"),
          Some("Zurich"),
          Some("ch"),
          Some("Switzerland"),
          Some("8107")
        ),
        BoundingBox(
          Semicircle.degree(47.457551),
          Semicircle.degree(47.4576987),
          Semicircle.degree(8.4247722),
          Semicircle.degree(8.4249838)
        )
      )
    )
  }
}
