package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.Icon")
@js.native
class Icon extends js.Object

object Icon {

  trait Options extends js.Object {
    val url: UndefOr[String] = js.undefined
    val retinaUrl: UndefOr[String] = js.undefined
    val iconSize: UndefOr[Point] = js.undefined
    val iconAnchor: UndefOr[Point] = js.undefined
    val popupAnchor: UndefOr[Point] = js.undefined
    val tooltipAnchor: UndefOr[Point] = js.undefined
    val shadowUrl: UndefOr[String] = js.undefined
    val shadowRetinaUrl: UndefOr[String] = js.undefined
    val shadowSize: UndefOr[Point] = js.undefined
    val shadowAnchor: UndefOr[Point] = js.undefined
    val className: UndefOr[String] = js.undefined
  }
}
