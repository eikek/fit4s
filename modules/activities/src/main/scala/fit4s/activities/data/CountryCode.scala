package fit4s.activities.data

final class CountryCode private (val cc: String) extends AnyVal {
  override def toString = s"CountryCode($cc)"
}

object CountryCode {
  def apply(cc: String): CountryCode = new CountryCode(cc.toLowerCase)
}
