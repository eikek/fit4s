package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

import org.scalajs.dom.HTMLElement

@JSGlobal("L.Layer")
@js.native
class Layer extends js.Object:
  def addTo(map: LeafletMap): Layer = js.native

  def remove(): Layer = js.native

  def removeFrom(map: LeafletMap): Layer = js.native

  def getPane(name: UndefOr[String] = ""): HTMLElement = js.native
