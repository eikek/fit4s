package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, JSName}

@JSGlobal("L.LatLngBounds")
@js.native
class LatLngBounds(southWest: LatLng, northEast: LatLng) extends js.Object {
  def isValid(): Boolean = js.native

  def getSouthEast(): LatLng = js.native

  def getSouthWest(): LatLng = js.native

  def getNorthEast(): LatLng = js.native

  def getNorthWest(): LatLng = js.native

  def getWest(): Double = js.native

  def getSouth(): Double = js.native

  def getEast(): Double = js.native

  def getNorth(): Double = js.native

  def getCenter(): LatLng = js.native

  def intersects(latLngBounds: LatLngBounds): Boolean = js.native

  @JSName("equals")
  def equalsLatLngBounds(latLngBounds: LatLngBounds): Boolean = js.native

  def toBBoxString(): String = js.native
}

object LatLngBounds {
  def apply(northEast: LatLng, southWest: LatLng): LatLngBounds =
    new LatLngBounds(southWest, northEast)
}
