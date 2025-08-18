package fit4s.codec

type FitBaseValue = Byte | Int | Long | Double | String

object FitBaseValue:

  def toByte(self: FitBaseValue) = self match
    case b: Byte   => Some(b)
    case _: Int    => None
    case _: Long   => None
    case _: Double => None
    case _: String => None
  def toInt(self: FitBaseValue) = self match
    case _: Byte   => None
    case n: Int    => Some(n)
    case _: Long   => None
    case _: Double => None
    case _: String => None
  def toLong(self: FitBaseValue) = self match
    case _: Byte   => None
    case _: Int    => None
    case n: Long   => Some(n)
    case _: Double => None
    case _: String => None
  def toDouble(self: FitBaseValue) = self match
    case _: Byte   => None
    case _: Int    => None
    case _: Long   => None
    case d: Double => Some(d)
    case s: String => None
  def toString(self: FitBaseValue) = self match
    case _: Byte   => None
    case _: Int    => None
    case _: Long   => None
    case _: Double => None
    case s: String => Some(s)

  object syntax:
    extension (self: FitBaseValue)
      def asByte = FitBaseValue.toByte(self)
      def asInt = FitBaseValue.toInt(self)
      def asLong = FitBaseValue.toLong(self)
      def asDouble = FitBaseValue.toDouble(self)
      def asString = FitBaseValue.toString(self)
      def asLongOrInt = self.asLong.orElse(self.asInt.map(_.toLong))
      def asUInt = self.asByte.map(_ & 0xff).orElse(self.asInt)

  // extension (n: Int) def widen: FitBaseValue = n
  // extension (n: Long) def widen: FitBaseValue = n
  // extension (n: Byte) def widen: FitBaseValue = n
  // extension (s: String) def widen: FitBaseValue = s
  // extension (d: Double) def widen: FitBaseValue = d
