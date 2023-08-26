package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

import org.scalajs.dom.HTMLElement

@JSGlobal("L.GridLayer")
@js.native
class GridLayer extends Layer {

  def createTile(coords: js.Any): HTMLElement = js.native

  def bringToBack(): GridLayer = js.native

  def getAttribution(): GridLayer = js.native

  def getContainer(): GridLayer = js.native

  def setOpacity(opacity: Number): GridLayer = js.native

  def setZIndex(zIndex: Number): GridLayer = js.native

  def isLoading(): GridLayer = js.native

  def redraw(): GridLayer = js.native

  def getTileSize(): GridLayer = js.native
}
