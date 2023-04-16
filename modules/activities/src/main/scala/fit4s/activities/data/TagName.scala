package fit4s.activities.data

import cats.Eq

final class TagName private (val name: String) extends AnyVal {

  override def toString = name

  def /(next: TagName): TagName =
    TagName.unsafeFromString(s"$name/${next.name}")

  def startsWith(other: TagName): Boolean =
    other.name.equalsIgnoreCase(name.take(other.name.length))

  def stripPrefix(prefix: TagName): TagName =
    if (startsWith(prefix)) {
      val suffix = name.drop(prefix.name.length)
      if (suffix.nonEmpty) {
        new TagName(if (suffix.charAt(0) == '/') suffix.drop(1) else suffix)
      } else this
    } else this

  def toLowerCase: TagName =
    new TagName(name.toLowerCase)
}

object TagName {

  def fromString(name: String): Either[String, TagName] =
    if (name.contains(',') || name.contains('+') || name.contains('%'))
      Left(s"Tag names must not contain commas, % and +")
    else Right(new TagName(name))

  def unsafeFromString(name: String): TagName =
    fromString(name).fold(sys.error, identity)

  implicit def equality: Eq[TagName] =
    Eq.instance((a, b) => a.name.equalsIgnoreCase(b.name))
}
