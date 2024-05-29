package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr

trait ZoomPanOptions extends js.Object:
  val paddingTopLeft: UndefOr[Point] = js.undefined
  val paddingBottomRight: UndefOr[Point] = js.undefined
  val padding: UndefOr[Point] = js.undefined
  val maxZoom: UndefOr[Double] = js.undefined
