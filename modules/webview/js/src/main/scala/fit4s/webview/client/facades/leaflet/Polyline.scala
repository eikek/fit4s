package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.Polyline")
@js.native
class Polyline extends Path {
  def getBounds(): LatLngBounds = js.native

  def setLatLngs(value: js.Array[LatLng]): Polyline = js.native
}

object Polyline {
  trait Options extends js.Object {
    val color: UndefOr[String] = js.undefined
    val fillOpacity: UndefOr[Double] = js.undefined
  }
}
