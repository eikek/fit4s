package fit4s.activities.data

final class PostCode(val zip: String) extends AnyVal {
  override def toString = s"PostCode($zip)"
}

object PostCode {
  def apply(zip: String): PostCode = new PostCode(zip)
}
