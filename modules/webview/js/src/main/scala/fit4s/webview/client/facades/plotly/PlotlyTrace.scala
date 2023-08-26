package fit4s.webview.client.facades.plotly

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class PlotlyTrace(
    var x: js.Array[Double],
    var y: js.Array[Double],
    var name: String,
    var `type`: String = "scatter",
    var visible: Boolean | String = "legendonly",
    var mode: String = "lines",
    var fill: String = "none"
) {

  def isEmpty: Boolean = x.isEmpty
  def nonEmpty: Boolean = !isEmpty
}

object PlotlyTrace {

  def of(data: List[(Double, Double)], name: String): PlotlyTrace = {
    val empty: (List[Double], List[Double]) = Nil -> Nil
    val (x, y) = data.reverse.foldLeft(empty) { (r, e) =>
      (e._1 :: r._1, e._2 :: r._2)
    }
    new PlotlyTrace(x.toJSArray, y.toJSArray, name)
  }
}
