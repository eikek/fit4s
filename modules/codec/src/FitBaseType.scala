package fit4s.codec

import fit4s.codec.internal.Codecs.{fold, padTo}
import fit4s.codec.internal.{Codecs, FitBaseTypeCodec}

import scodec.Attempt
import scodec.Codec
import scodec.Encoder
import scodec.Err
import scodec.bits.BitVector
import scodec.bits.ByteOrdering
import scodec.bits.{ByteVector, hex}

/** The fit base type as described here:
  * https://developer.garmin.com/fit/protocol/#basetype
  */
sealed trait FitBaseType extends Product:
  /** The base type field number. It is the position in the table at the above link. */
  def number: Short

  /** The base type byte. This is the byte that represents the base type, with endianess
    * bit and two reserved bits set to 0.
    */
  def fieldByte: Byte

  /** marking an invalid value of that base type */
  def invalidValue(bo: ByteOrdering): ByteVector

  /** Codec for a value using this base type. */
  def codec(bo: ByteOrdering, fieldSize: ByteSize): Codec[Vector[FitBaseValue]] =
    FitBaseTypeCodec.codec(this, bo, fieldSize)

  /** The size of a single value of this type. This is either 1, 2, 4 or 8 bytes. */
  def size: ByteSize

  final def name: String = productPrefix.toLowerCase

  def toFieldBaseType: FieldBaseType =
    FieldBaseType(size.toBytes > 1, number)

  @annotation.tailrec
  final def isInvalid(bo: ByteOrdering, data: BitVector): Boolean =
    val (first, rest) = data.splitAt(size.toBits)
    if first.isEmpty then true
    else if first != invalidValue(bo).bits then false
    else isInvalid(bo, rest)

  /** Aligns the given bit vector to be a multiple of this base types size. */
  def align(bo: ByteOrdering, bits: BitVector): BitVector =
    FitBaseType.align(this, bo, bits)

object FitBaseType:
  sealed abstract class AbstractBase(
      val number: Short,
      val fieldByte: Byte,
      invalid: ByteVector
  ) extends FitBaseType {
    val size: ByteSize = ByteSize.bytes(invalid.size)
    val endianAbility: Boolean = size > ByteSize.bytes(1)
    def invalidValue(bo: ByteOrdering) =
      bo.fold(invalid, invalid.reverse)

    override def toString(): String =
      f"${name.capitalize}($number, byte=0x$fieldByte%x, size=${size.toBytes})"
  }
  sealed abstract class IntBased(
      number: Short,
      fieldByte: Byte,
      invalid: ByteVector,
      val signed: Boolean
  ) extends AbstractBase(number, fieldByte, invalid)

  sealed abstract class LongBased(
      number: Short,
      fieldByte: Byte,
      invalid: ByteVector,
      val signed: Boolean
  ) extends AbstractBase(number, fieldByte, invalid)

  sealed abstract class FloatBased(number: Short, fieldByte: Byte, invalid: ByteVector)
      extends AbstractBase(number, fieldByte, invalid)

  case object Enum extends IntBased(0, 0x00, hex"ff", false)
  case object Sint8 extends IntBased(1, 0x01, hex"7f", true)
  case object Uint8 extends IntBased(2, 0x02, hex"ff", false)
  case object Sint16 extends IntBased(3, 0x83.toByte, hex"7fff", true)
  case object Uint16 extends IntBased(4, 0x84.toByte, hex"ffff", false)
  case object Sint32 extends LongBased(5, 0x85.toByte, hex"7fffffff", true)
  case object Uint32 extends LongBased(6, 0x86.toByte, hex"ffffffff", false)
  case object string extends AbstractBase(7, 0x07, hex"00") {
    val codec: Codec[String] = Codecs.stringUtf8.withContext(name)
    val encoder: Encoder[FitBaseValue] = Encoder {
      case n: String => codec.encode(n)
      case v         => Attempt.failure(Err(s"Invalid value for string encoding: $v"))
    }
  }
  case object Float32 extends FloatBased(8, 0x88.toByte, hex"ffffffff")
  case object Float64 extends FloatBased(9, 0x89.toByte, hex"ffffffffffffffff")
  case object Uint8Z extends IntBased(10, 0x0a.toByte, hex"00", false)
  case object Uint16Z extends IntBased(11, 0x8b.toByte, hex"0000", false)
  case object Uint32Z extends LongBased(12, 0x8c.toByte, hex"00000000", false)
  case object FByte extends AbstractBase(13, 0x0d, hex"ff") {
    val codec: Codec[Byte] = scodec.codecs.byte.withContext(name)
    val encoder: Encoder[FitBaseValue] =
      Encoder {
        case n: Byte => codec.encode(n)
        case v       => Attempt.failure(Err(s"Invalid value for byte encoding: $v"))
      }
  }
  case object Sint64 extends LongBased(14, 0x8e.toByte, hex"7fffffffffffffff", true)
  case object Uint64 extends LongBased(15, 0x8f.toByte, hex"ffffffffffffffff", false)
  case object Uint64Z extends LongBased(16, 0x90.toByte, hex"0000000000000000", false)

  val all: List[FitBaseType] = List(
    Enum,
    Sint8,
    Uint8,
    Sint16,
    FByte,
    Uint8Z,
    Uint16,
    Uint16Z,
    Sint32,
    Uint32,
    Uint32Z,
    Sint64,
    Uint64,
    Uint64Z,
    Float32,
    Float64,
    string
  )

  def byFieldNum(num: Short): Option[FitBaseType] =
    all.find(_.number == num)

  def byFieldDef(fd: FieldDef): Option[FitBaseType] =
    byFieldNum(fd.baseType.baseTypeNum)

  def byName(name: String): Option[FitBaseType] =
    all.find(_.name.equalsIgnoreCase(name))

  def byFieldByte(id: Short): Option[FitBaseType] =
    all.find(v => v.fieldByte == id.toByte)

  private def align(bt: FitBaseType, bo: ByteOrdering, bits: BitVector): BitVector =
    val numBits = bits.size
    val targetNumBits = bt.size.toBits
    if numBits == targetNumBits then bits
    else if numBits < targetNumBits then bo.padTo(bits, targetNumBits)
    else if numBits % targetNumBits == 0 then bits
    else bo.padTo(bits, ((numBits / targetNumBits) + 1) * targetNumBits)
