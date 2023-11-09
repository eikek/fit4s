package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.Marker")
@js.native
class Marker extends Layer {

  def setLatLng(latLng: LatLng): Unit = js.native

}

object Marker {
  trait Options extends js.Object {
    val icon: UndefOr[Icon] = js.undefined
    val draggable: UndefOr[Boolean] = js.undefined
    val autoPan: UndefOr[Boolean] = js.undefined
    val autoPanPadding: UndefOr[Point] = js.undefined
    val autoPanSpeed: UndefOr[Int] = js.undefined
    val keyboard: UndefOr[Boolean] = js.undefined
    val title: UndefOr[String] = js.undefined
    val alt: UndefOr[String] = js.undefined
    val zIndexOffset: UndefOr[Int] = js.undefined
    val opacity: UndefOr[Int] = js.undefined
    val riseOnHover: UndefOr[Boolean] = js.undefined
    val riseOffset: UndefOr[Int] = js.undefined
    val pane: UndefOr[String] = js.undefined
    val bubblingMouseEvents: UndefOr[Boolean] = js.undefined
  }
}
