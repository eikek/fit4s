package fit4s.activities.data

final class TagName private (val name: String) extends AnyVal {

  override def toString = name

  def /(next: TagName): TagName =
    TagName.unsafeFromString(s"$name/${next.name}")
}

object TagName {

  def fromString(name: String): Either[String, TagName] =
    if (name.contains(',') || name.contains('+') || name.contains('%'))
      Left(s"Tag names must not contain commas, % and +")
    else Right(new TagName(name))

  def unsafeFromString(name: String): TagName =
    fromString(name).fold(sys.error, identity)
}
