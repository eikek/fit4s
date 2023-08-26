package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

// This facade was adopted from https://github.com/cibotech/leaflet-facade

@JSGlobal("L")
@js.native
object Leaflet extends js.Object {

  def map(id: String, options: UndefOr[LeafletMap.Options] = js.undefined): LeafletMap =
    js.native

  def tileLayer(
      urlTemplate: String,
      options: UndefOr[TileLayer.Options] = js.undefined
  ): TileLayer = js.native

  def point(x: Double, y: Double, round: UndefOr[Boolean] = js.undefined): Point =
    js.native

  def polyline(
      coords: js.Array[LatLng],
      opts: UndefOr[Polyline.Options] = js.undefined
  ): Polyline = js.native

  def circle(latLng: LatLng, radius: Double, pathOptions: Path.Options): Circle =
    js.native

  def marker(latlngs: LatLng, options: UndefOr[Marker.Options] = js.undefined): Marker =
    js.native

  def icon(options: UndefOr[Icon.Options] = js.undefined): Icon = js.native

  def latLngBounds(points: js.Array[LatLng]): LatLngBounds = js.native
}
