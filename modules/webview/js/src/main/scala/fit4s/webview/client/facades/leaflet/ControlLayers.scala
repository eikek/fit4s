package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.Control.Layers")
@js.native
@annotation.nowarn
class ControlLayers(baseLayers: js.Dictionary[Layer]) extends Control
object ControlLayers {
  def apply(baseLayers: Map[String, Layer]): ControlLayers =
    new ControlLayers(baseLayers.toJSDictionary)
}
