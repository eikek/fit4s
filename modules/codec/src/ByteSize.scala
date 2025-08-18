package fit4s.codec

opaque type ByteSize = Long

object ByteSize:
  val zero: ByteSize = 0
  def bits(n: Long): ByteSize =
    if (n % 8 == 0) n / 8 else (n / 8) + 1
  def bytes(n: Long): ByteSize = n
  def kibi(n: Double): ByteSize = (n * 1024).toLong
  def mb(n: Double): ByteSize = (n * 1024 * 1024).toLong

  given Numeric[ByteSize] = Numeric.LongIsIntegral

  extension (self: ByteSize)
    def toBytes: Long = self
    def bs: Long = toBytes
    def toBits: Long = self * 8
    def isZero: Boolean = self == 0
    def isNegative: Boolean = self < 0
    def isPositive: Boolean = self > 0

    private def ord: Ordered[ByteSize] =
      Ordered.orderingToOrdered(self)(using Ordering[ByteSize])
    export ord.*

    def *(f: Double): ByteSize = (self * f).toLong
    def +(other: ByteSize): ByteSize = self + other
    def -(other: ByteSize): ByteSize = self - other
    def /(d: Double): ByteSize = (self / d).toLong

    def asString: String =
      if (math.abs(self) < 1024 && self != Long.MinValue) s"${self}B"
      else {
        val k = self / 1024.0
        if (math.abs(k) < 1024) f"$k%.02fK"
        else {
          val m = k / 1024.0
          if (math.abs(m) < 1024) f"$m%.02fM"
          else f"${m / 1024.0}%.02fG"
        }
      }
