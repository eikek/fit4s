package fit4s.webview.client.detail

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import fit4s.activities.data.ActivityDetailResult
import fit4s.webview.client.facades.leaflet.*
import fit4s.webview.client.facades.plotly.*
import fit4s.webview.client.shared.ClickAction

import calico.html.io.{*, given}
import org.scalajs.dom.{ResizeObserver, document}

object MapPanel:

  final case class Model(
      initialized: Boolean,
      largeView: Boolean,
      leafletId: String
  ):
    def initDone: Model = copy(initialized = true)
    def toggleLargeView = copy(largeView = !largeView)
    def requestFullscreen = IO:
      Option
        .when(leafletId.nonEmpty)(leafletId)
        .flatMap(id => Option(document.getElementById(id)))
        .map(_.requestFullscreen())
      ()
    def setMapId(id: String): Model =
      copy(leafletId = id)
  object Model:
    val empty: Model = Model(false, false, "")
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)
    given Eq[Model] = Eq.fromUniversalEquals

  private val viewButtonStyle =
    "px-1 py-1 rounded border dark:border-stone-500 hover:bg-sky-500 hover:border-sky-400 hover:text-stone-800 text-xs"

  def apply(details: ActivityDetailResult) =
    if (!hasPositionData(details)) div(cls := "hidden")
    else
      val mapElId = s"map${details.activity.id.id}"
      val plotElId = s"plot${details.activity.id.id}"
      Resource.eval(Model.makeEmpty).flatMap { model =>
        div(
          cls := "flex flex-col mt-2",
          // Map
          div(
            cls := "flex flex-col",
            onMouseMove --> ClickAction(initNative(mapElId, plotElId, model, details)),
            fullscreenBtn(model),
            div(
              idAttr := mapElId,
              // TODO changing via CSS and listening on resize, tiles are
              // invisible afterwards. that's the reason the
              // `requestFullscreen` is used
              cls <-- model.map(_.largeView).changes.map { b =>
                if (b) "w-full h-screen border dark:border-stone-700" :: Nil
                else "w-full h-128 border dark:border-stone-700" :: Nil
              },
              div(
                cls <-- model.map(_.initialized).changes.map { b =>
                  if (b) "hidden" :: Nil
                  else "w-64 mx-auto text-center mt-8 opacity-75 cursor-pointer" :: Nil
                },
                "Please move mouse here to load map"
              )
            )
          ),

          // Plot
          div(
            cls := (if (hasPlotData(details)) "h-80 w-full border dark:border-stone-700"
                    else "hidden"),
            idAttr := plotElId
          )
        )
      }

  private def initNative(
      mapId: String,
      plotId: String,
      model: SignallingRef[IO, Model],
      details: ActivityDetailResult
  ): IO[Unit] =
    val doInit = List(
      initializeMap(details, mapId),
      initializePlot(details, plotId),
      model.update(_.setMapId(mapId))
    ).parSequence_

    model.getAndUpdate(_.initDone).flatMap { m =>
      if (m.initialized) IO.unit
      else doInit
    }

  private def fullscreenBtn(model: SignallingRef[IO, Model]) =
    div(
      cls := "",
      button(
        cls := viewButtonStyle,
        "Fullscreen",
        onClick --> ClickAction(model.get.flatMap(_.requestFullscreen))
      )
    )

  private def hasPositionData(details: ActivityDetailResult): Boolean =
    val s = details.sessions.head
    details.sessionData.get(s.id).getOrElse(Nil).exists(_.position.isDefined)

  private def hasPlotData(details: ActivityDetailResult): Boolean =
    val s = details.sessions.head
    val data = details.sessionData.get(s.id).getOrElse(Nil)
    data.exists(_.heartRate.isDefined) ||
    data.exists(_.altitude.isDefined) ||
    data.exists(_.temperature.isDefined)

  @annotation.nowarn
  private def debug[A](msg: String, a: A): A =
    scribe.warn(s"$msg: $a")
    a

  private def initializeMap(details: ActivityDetailResult, elId: String): IO[LeafletMap] =
    IO.delay:
      val s = details.sessions.head
      val sd = details.sessionData.get(s.id).getOrElse(Nil).filter(_.position.isDefined)

      val track = sd.flatMap(_.position).map(LatLng.apply).toJSArray

      val osm = Leaflet.tileLayer(Provider.OSM.url, Provider.OSM.toTileOptions)
      val gis = Leaflet.tileLayer(Provider.ArcGIS.url, Provider.ArcGIS.toTileOptions)
      val cosm = Leaflet.tileLayer(Provider.CyclOSM.url, Provider.CyclOSM.toTileOptions)

      val leafletMap = Leaflet
        .map(elId, options = LeafletMap.Options(js.Array(osm)))
        .fitBounds(Leaflet.latLngBounds(track))

      track.headOption match
        case Some(pos0) =>
          Leaflet.marker(pos0).addTo(leafletMap)
        case _ => ()

      ControlLayers(Map("OSM" -> osm, "ArcGIS" -> gis, "CyclOSM" -> cosm))
        .addTo(leafletMap)

      // invalidateSize refreshes the map when its container changes size
      new ResizeObserver((_, _) => {
        leafletMap.invalidateSize()
        leafletMap.fitBounds(Leaflet.latLngBounds(track))
      }).observe(leafletMap.getContainer())

      Leaflet
        .polyline(track)
        .addTo(leafletMap)

      leafletMap

  private def initializePlot(details: ActivityDetailResult, elId: String): IO[Unit] =
    IO.delay:
      val s = details.sessions.head
      val sd = details.sessionData.get(s.id).getOrElse(Nil)

      val elevationTrace = PlotlyTrace.of(
        sd.flatMap(e => e.pair(_.distance, _.altitude)(_.meter -> _.meter)),
        "Elevation"
      )
      // elevationTrace.fill = "tozeroy"

      val hrTrace = PlotlyTrace.of(
        sd.flatMap(e => e.pair(_.distance, _.heartRate)(_.meter -> _.bpm.toDouble)),
        "Heart Rate"
      )
      val tempTrace = PlotlyTrace.of(
        sd.flatMap(e => e.pair(_.distance, _.temperature)(_.meter -> _.celcius.toDouble)),
        "Temperature"
      )

      // make the first non-empty visible
      List(elevationTrace, hrTrace, tempTrace).foldLeft(false) { (b, e) =>
        if (b) e.visible = "legendonly"
        else if (e.nonEmpty) e.visible = true
        e.visible match
          case x: Boolean => b || x
          case _          => b
      }

      val layout = PlotlyLayout.default
      val data = js.Array(elevationTrace, hrTrace, tempTrace)

      Plotly.newPlot(elId, data, layout, PlotlyConfig.default)
