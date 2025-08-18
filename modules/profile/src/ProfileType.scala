package fit4s.profile

trait ProfileType extends (Int => Option[String]):
  def name: String
  def baseType: String
  def values: Map[Int, String]
  lazy val valuesReverse: Map[String, Int] =
    values.map { case (a, b) => (b, a) }

  final def apply(n: Int): Option[String] = values.get(n)
  final def apply(name: String): Option[Int] = valuesReverse.get(name)
