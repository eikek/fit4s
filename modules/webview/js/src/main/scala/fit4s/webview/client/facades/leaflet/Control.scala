package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, JSName}

import org.scalajs.dom.HTMLElement

@JSGlobal("L.Control")
@js.native
class Control(position: UndefOr[String] = "topright") extends js.Object {
  def getPosition(): String = js.native

  def setPosition(position: String): Control = js.native

  def getContainer(): HTMLElement = js.native

  def addTo(map: LeafletMap): Control = js.native

  def remove(): Control = js.native
}
