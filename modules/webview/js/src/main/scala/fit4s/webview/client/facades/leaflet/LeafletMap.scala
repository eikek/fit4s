package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal

import org.scalajs.dom.HTMLElement

@JSGlobal("L.Map")
@js.native
class LeafletMap(id: String, options: UndefOr[LeafletMap.Options] = js.undefined)
    extends js.Object {
  def setView(
      center: LatLng,
      zoom: Double,
      options: UndefOr[ZoomPanOptions] = js.undefined
  ): LeafletMap = js.native

  def setZoom(level: Int): LeafletMap = js.native

  def addControl(control: Control): LeafletMap = js.native

  def removeControl(control: Control): LeafletMap = js.native

  def addLayer(layer: Layer): LeafletMap = js.native

  def removeLayer(layer: Layer): LeafletMap = js.native

  def getZoom(): Int = js.native

  def hasLayer(layer: Layer): Boolean = js.native

  def eachLayer(fn: js.Function1[Layer, Unit]): LeafletMap = js.native

  def getCenter(): LatLng = js.native

  def panTo(latLng: LatLng): LeafletMap = js.native

  def remove(): LeafletMap = js.native

  def zoomIn(): LeafletMap = js.native

  def zoomOut(): LeafletMap = js.native

  def invalidateSize(): LeafletMap = js.native

  def getContainer(): HTMLElement = js.native

  def fitBounds(bounds: LatLngBounds): LeafletMap = js.native

  def getBounds(): LatLngBounds = js.native
}

object LeafletMap {

  trait Options extends js.Object {
    val preferCanvas: UndefOr[Boolean] = js.undefined
    val attributionControl: UndefOr[Boolean] = js.undefined
    val zoomControl: UndefOr[Boolean] = js.undefined
    val closePopupOnClick: UndefOr[Boolean] = js.undefined
    val zoomSnap: UndefOr[Int] = js.undefined
    val zoomDelta: UndefOr[Int] = js.undefined
    val trackResize: UndefOr[Boolean] = js.undefined
    val boxZoom: UndefOr[Boolean] = js.undefined
    val doubleClickZoom: UndefOr[Boolean | String] = js.undefined
    val dragging: UndefOr[Boolean] = js.undefined
    val zoom: UndefOr[Number] = js.undefined
    val minZoom: UndefOr[Number] = js.undefined
    val maxZoom: UndefOr[Number] = js.undefined
    val fadeAnimation: UndefOr[Boolean] = js.undefined
    val markerZoomAnimation: UndefOr[Boolean] = js.undefined
    val transform3DLimit: UndefOr[Int] = js.undefined
    val zoomAnimation: UndefOr[Boolean] = js.undefined
    val zoomAnimationThreshold: UndefOr[Int] = js.undefined
    val touchZoom: UndefOr[Boolean] = js.undefined
    val tap: UndefOr[Boolean] = js.undefined
    val scrollWheelZoom: UndefOr[Boolean] = js.undefined
    val layers: UndefOr[js.Array[_ <: Layer]] = js.undefined
  }

  object Options {
    def apply(mLayers: js.Array[_ <: Layer]): Options =
      new Options {
        override val layers = mLayers
      }
  }
}
