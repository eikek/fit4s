package fit4s.webview.client.shared

object Styles {

  val distanceIcon = "fa fa-right-long text-sky-400"
  val elevationIcon = "fa fa-up-long text-sky-400"
  val speedIcon = "fa fa-gauge text-sky-400"
  val timeIcon = "fa fa-stopwatch text-sky-400"

  def apply(p: (String, Boolean)*): List[String] =
    p.toList.filter(_._2).map(_._1)
}
