package fit4s.activities.data

final class TagName private (val name: String) extends AnyVal

object TagName {

  def fromString(name: String): Either[String, TagName] =
    if (name.contains(',')) Left(s"Tag names must not contain commas")
    else Right(new TagName(name))

  def unsafeFromString(name: String): TagName =
    fromString(name).fold(sys.error, identity)
}
