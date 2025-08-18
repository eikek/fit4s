package fit4s.core

import fit4s.core.data.Position

import munit.Assertions

trait TestSyntax extends Assertions:

  def assertEqualsPosition(obtained: Position, expected: Position, delta: Int) =
    val exact = obtained == expected
    val p = expected - obtained
    val almost = Math.abs(p.latitude.toSemicircle) <= delta && Math.abs(
      p.longitude.toSemicircle
    ) <= delta
    if (!exact && !almost)
      failComparison(
        s"Positions are not equal (delta=$delta): expected $expected, got $obtained",
        obtained,
        expected
      )

  extension [A](self: Option[Either[String, A]])
    def value: A =
      self.map(_.fold(sys.error, identity)).getOrElse(sys.error("empty option"))

  extension [A](self: Either[String, Option[A]])
    def value: A =
      self.fold(sys.error, _.getOrElse(sys.error("empty option")))

  extension [A, B](eab: Either[A, B])
    def get: B = eab.fold(
      {
        case e: Throwable => throw e
        case e            => sys.error(e.toString)
      },
      identity
    )
