package fit4s.activities.data

final class StravaTokenId(val id: Long) extends AnyVal {
  override def toString = s"StravaTokenId($id)"
}

object StravaTokenId {
  def apply(id: Long): StravaTokenId = new StravaTokenId(id)
}
