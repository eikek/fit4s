package fit4s.data

final private class NumericFrom[A, B: Numeric](f: A => B, g: B => A) extends Numeric[A] {
  private[this] val n: Numeric[B] = Numeric[B]

  override def plus(x: A, y: A): A =
    g(n.plus(f(x), f(y)))

  override def minus(x: A, y: A): A =
    g(n.minus(f(x), f(y)))

  override def times(x: A, y: A): A =
    g(n.times(f(x), f(y)))

  override def negate(x: A): A =
    g(n.negate(f(x)))

  override def fromInt(x: Int): A =
    g(n.fromInt(x))

  override def parseString(str: String): Option[A] =
    n.parseString(str).map(g)

  override def toInt(x: A): Int =
    n.toInt(f(x))

  override def toLong(x: A): Long =
    n.toLong(f(x))

  override def toFloat(x: A): Float =
    n.toFloat(f(x))

  override def toDouble(x: A): Double =
    n.toDouble(f(x))

  override def compare(x: A, y: A): Int =
    n.compare(f(x), f(y))
}

object NumericFrom {
  def apply[A, B: Numeric](f: A => B, g: B => A): Numeric[A] =
    new NumericFrom[A, B](f, g)

  val javaDuration: Numeric[java.time.Duration] =
    NumericFrom[java.time.Duration, Long](_.toMillis, java.time.Duration.ofMillis)

}
