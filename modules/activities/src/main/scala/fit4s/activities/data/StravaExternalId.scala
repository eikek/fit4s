package fit4s.activities.data

final class StravaExternalId(val id: Long) extends AnyVal {
  override def toString = s"StravaExternal($id)"
}

object StravaExternalId {
  def apply(id: Long): StravaExternalId = new StravaExternalId(id)
}
