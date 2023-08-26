package fit4s.webview.client.facades.plotly

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

import org.scalajs.dom.{Element, HTMLElement}

@js.native
@JSImport("plotly.js-dist/plotly.js", JSImport.Namespace)
object Plotly extends js.Object {

  def newPlot(
      el: HTMLElement | String,
      data: js.Array[PlotlyTrace],
      layout: js.UndefOr[PlotlyLayout] = js.undefined,
      config: js.UndefOr[PlotlyConfig] = PlotlyConfig.default
  ): Unit = js.native

}
