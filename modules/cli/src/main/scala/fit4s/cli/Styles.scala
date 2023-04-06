package fit4s.cli

import fit4s.data.Temperature
import fit4s.profile.types.Sport

case class Styles(style: String) {
  def ++(other: Styles): Styles = Styles(style + other.style)
}

object Styles {
  private def frgb(r: Int, g: Int, b: Int): Styles =
    Styles(s"""\u001b[38;2;$r;$g;${b}m""")

  // private def brgb(r: Int, g: Int, b: Int): Styles =
  //  Styles(s"""\u001b[48;2;$r;$g;${b}m""")

  private val bold = Styles(Console.BOLD)

  val error = frgb(255, 0, 0) ++ bold

  val activityId = frgb(120, 120, 192)

  val sessionSeparator = frgb(38, 38, 38)

  val activityDate = Styles(Console.BOLD)

  val activityName = frgb(153, 230, 255)

  val headerOne = frgb(0, 230, 0) ++ bold

  val summaryFieldName = frgb(153, 204, 255)
  val summaryFieldValue = frgb(255, 255, 230) ++ bold

  def sport(implicit s: Sport) =
    s match {
      case Sport.Cycling  => frgb(0, 255, 64)
      case Sport.Running  => frgb(255, 191, 0)
      case Sport.Swimming => frgb(0, 191, 255)
      case _              => frgb(153, 102, 102)
    }

  val distance = frgb(230, 230, 0)
  val duration = frgb(230, 230, 0)
  val speed = frgb(230, 230, 0)
  val calories = frgb(230, 230, 0)
  val heartRate = frgb(255, 128, 128)
  val device = frgb(102, 102, 102)

  def temperature(t: Option[Temperature]): Styles =
    t match {
      case None                      => Styles("")
      case Some(t) if t.celcius < 1  => frgb(5, 102, 199)
      case Some(t) if t.celcius < 10 => frgb(77, 210, 255)
      case Some(t) if t.celcius < 25 => frgb(255, 77, 77)
      case _                         => frgb(255, 128, 128)
    }
}
