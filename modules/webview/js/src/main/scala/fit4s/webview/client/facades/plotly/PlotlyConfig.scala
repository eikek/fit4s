package fit4s.webview.client.facades.plotly

import scala.scalajs.js

trait PlotlyConfig extends js.Object:
  val responsive: js.UndefOr[Boolean] = js.undefined
  val displayModeBar: js.UndefOr[Boolean] = js.undefined
  val displayLogo: js.UndefOr[Boolean] = js.undefined

object PlotlyConfig:

  val default = new PlotlyConfig:
    override val responsive = true
    override val displayModeBar = false
