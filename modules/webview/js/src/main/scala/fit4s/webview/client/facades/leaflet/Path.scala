package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal("L.Path")
@js.native
abstract class Path extends Layer {
  def setStyle(pathOptions: Path.Options): Path = js.native

  def redraw(): Path = js.native

  def bringToFront(): Path = js.native
}

object Path {
  trait Options extends js.Object {
    val color: UndefOr[String] = js.undefined
    val opacity: UndefOr[Double] = js.undefined
    val fillOpacity: UndefOr[Double] = js.undefined
    val className: UndefOr[String] = js.undefined
    val weight: UndefOr[Int] = js.undefined
  }
}
