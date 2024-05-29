package fit4s.tcx

final case class TcxCreator(
    name: String,
    productId: Int,
    unitId: Long
):

  def nameNormalized: Option[String] =
    Option(name.split("\\s+").toList match {
      case a :: Nil    => a
      case a :: b :: _ => s"$a $b"
      case _           => name.trim
    }).map(_.trim).filter(_.nonEmpty)
