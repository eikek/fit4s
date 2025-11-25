package fit4s.profile

final case class ProfileEnum private (profile: ProfileType, value: String):
  val ordinal: Int = profile.valuesReverse(value)

  override def toString() =
    s"${profile.name}($value, $ordinal)"

  def isValue(v: Int | String): Boolean = v match
    case n: Int    => ordinal == n
    case n: String => value == n

object ProfileEnum:

  def first(value: Int, pt: ProfileType, more: ProfileType*): Option[ProfileEnum] =
    (pt +: more).collectFirst(Function.unlift(apply(_, value)))

  def unsafe(value: Int, pt: ProfileType, more: ProfileType*): ProfileEnum =
    first(value, pt, more*).getOrElse(
      sys.error(s"No enum $value found in ${pt.name +: more.map(_.name)}")
    )

  def apply(pt: ProfileType, value: Int): Option[ProfileEnum] =
    pt.values.get(value).map(name => new ProfileEnum(pt, name))

  def apply(pt: ProfileType, value: String): Option[ProfileEnum] =
    pt.valuesReverse.get(value).map(_ => new ProfileEnum(pt, value))
