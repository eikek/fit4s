package fit4s.profile

final case class ProfileEnum(profile: ProfileType, ordinal: Int):
  val value: Option[String] = profile.values.get(ordinal)

  override def toString() =
    s"${profile.name}($value, $ordinal)"

  def isValue(v: Int | String): Boolean = v match
    case n: Int    => ordinal == n
    case n: String => value.contains(n)

object ProfileEnum:

  def first(value: Int, pt: ProfileType, more: ProfileType*): Option[ProfileEnum] =
    (pt +: more).collectFirst(Function.unlift(ofType(_, value)))

  def unsafe(value: Int, pt: ProfileType, more: ProfileType*): ProfileEnum =
    first(value, pt, more*).getOrElse(
      sys.error(s"No enum $value found in ${pt.name +: more.map(_.name)}")
    )

  def ofType(pt: ProfileType, value: Int): Option[ProfileEnum] =
    pt.values.get(value).map(_ => new ProfileEnum(pt, value))

  def ofType(pt: ProfileType, value: String): Option[ProfileEnum] =
    pt.valuesReverse.get(value).map(n => new ProfileEnum(pt, n))
