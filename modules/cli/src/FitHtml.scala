package fit4s.cli

import fs2.io.file.Path

import fit4s.core.data.FieldValueDisplay.given
import fit4s.core.data.Polyline
import fit4s.core.{Fit, FitMessage}
import fit4s.profile.*

import scalatags.Text.all.*

object FitHtml:
  private val cls = `class`

  def apply(fits: Vector[Fit], track: Polyline, file: Path) =
    doctype("html")(
      html(
        makeHead(file),
        body(
          cls := "px-2",
          h1(
            cls := "w-full text-3xl font-bold my-3 mt-2",
            s"${fileType(fits)} ${file.fileName} (${fits.size} files)"
          ),
          div(
            cls := "flex flex-row",
            div(
              cls := "flex w-96 px-2 py-2",
              ul(
                cls := "sticky top-2 h-150",
                li(
                  a(
                    cls := "text-blue-500 hover:text-blue-700",
                    href := s"#map-container",
                    "Map"
                  )
                ),
                MsgData.all(fits.head).map { data =>
                  li(
                    a(
                      cls := "text-blue-500 hover:text-blue-700",
                      href := s"#${data.mesgName}",
                      data.mesgName
                    )
                  )
                }
              )
            ),
            div(
              cls := "flex flex-grow flex-col",
              style := "width: calc(100% - 20rem);",
              Option.when(track.nonEmpty)(mapDiv(track)),
              Option.when(track.isEmpty)(
                div(cls := "w-full", "The fit files has no coordinates.")
              ),
              fits.flatMap(fitDetails)
            )
          ),
          Option.when(track.nonEmpty)(leafletScript(track))
        )
      )
    )

  def mapDiv(track: Polyline) =
    val btnStyle =
      "px-3 py-2 rounded border absolute right-2 z-1000 bg-white pointer hover:bg-gray-100"
    div(
      cls := "w-full h-140 relative",
      id := "map-container",
      div(id := "map", cls := "flex w-full h-full"),
      button(
        id := "map-center-btn",
        title := "Re-center map",
        cls := s"$btnStyle bottom-5",
        i(cls := "fa fa-rotate ")
      ),
      button(
        id := "map-fullscreen-btn",
        title := "Fullscreen",
        cls := s"$btnStyle bottom-17",
        i(cls := "fa fa-expand ")
      )
    )

  def fitDetails(fit: Fit) =
    MsgData.all(fit).map { data =>
      div(
        id := data.mesgName,
        div(
          cls := s"font-bold text-xl py-2 px-4 bg-blue-100 text-left w-full mt-4",
          if data.messages.size > 1 then
            span(
              data.mesgName,
              span(
                cls := "ml-4 text-sm font-normal",
                s"(${data.messages.size} messages)"
              )
            )
          else data.mesgName
        ),
        div(
          cls := "w-full max-w-full max-h-100 overflow-x-scroll overflow-y-scroll flex flex-col",
          div(
            cls := s"grid grid-cols-[repeat(${data.fields.size + 1},minmax(150px,_1fr))] min-w-full py-2 text-center",
            div(
              cls := "contents font-bold",
              div(cls := "sticky top-0 bg-white", "Timestamp"),
              data.fields.map(f =>
                div(cls := "sticky top-0 bg-white", f.fieldName.replace("_", " "))
              )
            ),
            data.messages.map(messageDetails(data.fields))
          )
        )
      )
    }

  def messageDetails(fields: List[MsgField])(fm: FitMessage) =
    div(fm.timestamp.map(_.asInstant.toString).getOrElse("-")) ::
      fields
        .map(f => fm.field(f).map(_.show).getOrElse(""))
        .map(str => div(str))

  def fileType(fits: Vector[Fit]) =
    fits.head.fileId.map(_.fileType.value.capitalize).getOrElse("")

  def leafletScript(track: Polyline) =
    script(
      `type` := "text/javascript",
      raw(s"""
        // street
        const streetsLayer = L.tileLayer('https://api.maptiler.com/maps/basic/{z}/{x}/{y}@2x.png?key=HrARH01SH6sg5I6HoXdU', {
            attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.maptiler.com/">Maptiler</a>',
            maxZoom: 24,
            id: 'mapbox/streets-v11',
            tileSize: 512,
            zoomOffset: -1
        });

        const bikeLayer = L.tileLayer('https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png', {
            attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.maptiler.com/">Cyclosm</a>'
        });

        const map = L.map('map', {
            center: [0, 0],
            zoom: 3,
            layers: [streetsLayer, bikeLayer]
        });

        const baseLayers = {
            "Bicycle": bikeLayer,
            "Streets": streetsLayer,
        };
        const precision = ${track.cfg.precision};

        L.control.layers(baseLayers).addTo(map);

        const addPolyline = (line, color, arrowRepeat) => {
            const pl = L.PolylineUtil.decode(line, precision);
            const polyline = L.polyline(pl, {
                weight: 4,
                color: color
            }).addTo(map);
            const decorator = L.polylineDecorator(polyline, {
                patterns: [
                    // arrows on the polyline indicating the direction
                    {
                        offset: 50,
                        repeat: arrowRepeat,
                        symbol: L.Symbol.arrowHead({ pathOptions: { fillOpacity: 0.7, weight: 3, color: color } })
                    }
                ]
            }).addTo(map);
            map.fitBounds(polyline.getBounds());

            // start marker
            const startIcon = L.divIcon({
                className: "start-icon",
                html: '<i class="fa fa-flag fa-2x"></i>',
                iconSize: [10, 10],
            });
            const start = L.marker(pl[0], { title: "Start", icon: startIcon });
            start.addTo(map);


            const finishIcon = L.divIcon({
                className: "finish-icon",
                html: '<i class="fa fa-flag-checkered fa-2x"></i>',
                iconSize: [10, 10],
            });
            const end = L.marker(pl[pl.length - 1], { title: "Finish", icon: finishIcon });
            end.addTo(map);

        };

        const addNormalPolyline = (line, color) => {
            const pl = L.polyline(line, { color: color, weight: 5, opacity: 0.5 }).addTo(map);
            var decorator = L.polylineDecorator(pl, {
                patterns: [
                    // arrows on the polyline indicating the direction
                    {
                        offset: 10,
                        repeat: 400,
                        symbol: L.Symbol.arrowHead({ pathOptions: { fillOpacity: 0.7, weight: 1, color: color } })
                    }
                ]
            }).addTo(map);

            map.fitBounds(pl.getBounds());
        };

      const track = "${track.encoded.replace("\\", "\\\\")}";

      addPolyline(track, "blue", 400);

      const mapEl = document.getElementById("map-container");
      const btnFs = document.getElementById("map-fullscreen-btn");
      const btnCenter = document.getElementById("map-center-btn");
      if (btnFs && mapEl) {
        btnFs.addEventListener("click", function(ev) {
          console.log("clicked fullscreen");
          mapEl.requestFullscreen();
        });
      }
      if (btnCenter && mapEl) {
        const pl = L.PolylineUtil.decode(track, precision);
        const polyline = L.polyline(pl);
        btnCenter.addEventListener("click", function(ev) {
          console.log("clicked center");
          map.fitBounds(polyline.getBounds());
        });
      }
    </script> """)
    )
  def makeHead(file: Path) =
    head(
      tag("title")(s"View ${file.fileName}"),
      meta(charset := "utf-8"),
      meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
      link(
        rel := "stylesheet",
        href := "https://cdn.jsdelivr.net/npm/leaflet@1.7.1/dist/leaflet.css",
        integrity := "sha384-VzLXTJGPSyTLX6d96AxgkKvE/LRb7ECGyTxuwtpjHnVWVZs2gp5RDjeM/tgBnVdM",
        crossorigin := "anonymous"
      ),
      script(
        src := "https://cdn.jsdelivr.net/npm/leaflet@1.7.1/dist/leaflet.js",
        integrity := "sha384-RFZC58YeKApoNsIbBxf4z6JJXmh+geBSgkCQXFyh+4tiFSJmJBt+2FbjxW7Ar16M",
        crossorigin := "anonymous"
      ),
      script(
        src := "https://cdn.jsdelivr.net/npm/polyline-encoded@0.0.9/Polyline.encoded.js",
        integrity := "sha384-Unn6s4TuHyJguuREG7OCHeFEG3FhZ8dS8er0GGMt7mUzkyM0O/VpRwuKSTEbzVvu",
        crossorigin := "anonymous"
      ),
      script(
        src := "https://cdn.jsdelivr.net/npm/leaflet-polylinedecorator@1.6.0/dist/leaflet.polylineDecorator.js",
        integrity := "sha384-FhPn/2P/fJGhQLeNWDn9B/2Gml2bPOrKJwFqJXgR3xOPYxWg5mYQ5XZdhUSugZT0",
        crossorigin := "anonymous"
      ),
      script(
        src := " https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.0.0/js/all.min.js"
      ),
      script(src := "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"),
      link(
        href := " https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.0.0/css/fontawesome.min.css",
        rel := "stylesheet"
      ),
      tag("style")(
        tpe := "text/css",
        """
        html,
        body {height: 100%; }

        td { text-align: center; padding-left: 0.25rem; padding-right: 0.25rem;}

        tr { margin-top: 0.25rem; margin-bottom: 0.25rem; }
        tr th.text-left:first-child  { text-align: left; }
        tr th  { text-align: center; padding-left: 0.25rem; padding-right: 0.25rem;}

        .finish-icon {
            text-align: center;
            line-height: 10px;
            color: darkred;
        }

        .start-icon {
            text-align: center;
            line-height: 10px;
            color: goldenrod;
        }"""
      )
    )

  final case class MsgData(
      mesgNum: Int,
      mesgName: String,
      messages: Vector[FitMessage]
  ):
    private val all: List[MsgField] = messages.headOption.toList
      .flatMap(_.schema.values.toList)
      .filter(_.fieldDefNum != CommonMsg.timestamp.fieldDefNum)
      .toList
      .sortBy(_.fieldName)
    val fields =
      all.filter(f => messages.exists(m => m.field(f).isDefined))

    val hasData = messages.nonEmpty && fields.nonEmpty

  object MsgData:
    val mesgNums = MesgNumType.values.keySet.toList.sorted

    def apply(fit: Fit, n: Int): MsgData =
      val msgs = fit.getMessages(n)
      val msgName = MesgNumType.values(n)
      MsgData(n, msgName, msgs.toVector)

    def all(fit: Fit) =
      mesgNums
        .map(MsgData(fit, _))
        .filter(_.hasData)
