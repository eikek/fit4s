package fit4s.cli.activity

object ConsoleUtil {
  val bold = Console.BOLD
  val reset = Console.RESET
  val red = Console.RED
  val green = Console.GREEN
  val boldRed = s"$bold$red"

  def printHeader(header: String): String = {
    val len = header.length
    val dashes = List.fill(len)('-').mkString
    s"$bold$green$header\n$dashes$reset\n"
  }

}
