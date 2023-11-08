package fit4s.webview.client.facades.leaflet

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, JSName}

@JSGlobal("L.Point")
@js.native
@annotation.nowarn
class Point(x: Double, y: Double, round: UndefOr[Boolean] = js.undefined)
    extends js.Object {
  def this(coords: js.Array[Double]) = this(coords(0), coords(1))

  def add(point: Point): Point = js.native

  def subtract(point: Point): Point = js.native

  def divideBy(num: Double): Point = js.native

  def multiplyBy(num: Double): Point = js.native

  def scaleBy(point: Point): Point = js.native

  def unscaleBy(point: Point): Point = js.native

  @JSName("round")
  def roundPoint(): Point = js.native

  def floor(): Point = js.native

  def ceil(): Point = js.native

  def distanceTo(): Double = js.native

  def contains(): Boolean = js.native
}
