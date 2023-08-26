package fit4s.webview.client.facades.leaflet

// see https://wiki.openstreetmap.org/wiki/Raster_tile_providers
enum Provider(val url: String, val attr: String, maxZoom: Int):
  case OSM
      extends Provider(
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors",
        19
      )
  case ArcGIS
      extends Provider(
        "http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        "&copy; <a href=\"https://www.arcgis.com/index.html\">ArcGIS</a> contributors",
        19
      )

  case CyclOSM
      extends Provider(
        "https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
        "&copy; <a href=\"https://www.cyclosm.org/\">CyclOSM</a> contributors",
        20
      )

  def toTileOptions: TileLayer.Options = TileLayer.Options(this.maxZoom, this.attr)
