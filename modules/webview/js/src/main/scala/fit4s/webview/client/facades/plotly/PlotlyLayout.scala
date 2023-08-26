package fit4s.webview.client.facades.plotly

import scala.scalajs.js

trait PlotlyLayout extends js.Object {

  val margin: js.UndefOr[PlotlyLayout.Margin] = js.undefined
}

object PlotlyLayout {
  val default = new PlotlyLayout {
    override val margin = new Margin {
      override val t = 15
      override val r = 15
      override val b = 50
    }
  }

  trait Margin extends js.Object {
    val autoexpand: js.UndefOr[Boolean] = js.undefined
    val b: js.UndefOr[Int] = js.undefined
    val l: js.UndefOr[Int] = js.undefined
    val pad: js.UndefOr[Int] = js.undefined
    val r: js.UndefOr[Int] = js.undefined
    val t: js.UndefOr[Int] = js.undefined
  }
}
