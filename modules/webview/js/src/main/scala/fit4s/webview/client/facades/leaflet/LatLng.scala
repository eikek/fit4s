package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, JSName}

import fit4s.data.Position

@JSGlobal("L.LatLng")
@js.native
class LatLng(val lat: Double, val lng: Double, val alt: UndefOr[Double] = js.undefined)
    extends js.Object {
  def this(lat: Double, lng: Double) = this(lat, lng, js.undefined)

  def distanceTo(other: LatLng): Double = js.native

  @JSName("equals")
  def equalsLatLng(other: LatLng): Boolean = js.native

  def wrap(left: Double, right: Double): LatLng = js.native
}

object LatLng {
  def apply(latitude: Double, longitude: Double): LatLng = new LatLng(latitude, longitude)

  def apply(pos: Position): LatLng =
    new LatLng(pos.latitude.toDegree, pos.longitude.toDegree)
}
