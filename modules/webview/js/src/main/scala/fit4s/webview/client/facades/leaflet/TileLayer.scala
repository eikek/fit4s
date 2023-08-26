package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.TileLayer")
@js.native
class TileLayer(val urlTemplate: String, val tileLayerOptions: UndefOr[TileLayer.Options])
    extends GridLayer

object TileLayer {
  trait Options extends js.Object {

    val maxZoom: UndefOr[Int] = js.undefined
    val attribution: UndefOr[String] = js.undefined

  }

  object Options {

    def apply(maxZoomLevel: Int, attr: String): Options =
      new Options {
        override val maxZoom = maxZoomLevel
        override val attribution = attr
      }
  }
}
