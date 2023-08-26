package fit4s.profile.types

import fit4s.profile.types.BaseTypedValue.{FloatBaseValue, LongBaseValue, StringBaseValue}
import fit4s.util.Nel

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs.{fixedSizeBytes, list}

final case class ArrayFieldType[A](rawValue: Nel[A], base: FitBaseType)
    extends TypedValue[Nel[A]] {
  val typeName = base.typeName
}

object ArrayFieldType {

  def codec(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  ): Codec[ArrayFieldType[_]] =
    (base match {
      case FitBaseType.Enum    => codecT(sizeBytes, bo, FitBaseType.Enum)
      case FitBaseType.Sint8   => codecT(sizeBytes, bo, FitBaseType.Sint8)
      case FitBaseType.Uint8   => codecT(sizeBytes, bo, FitBaseType.Uint8)
      case FitBaseType.Sint16  => codecT(sizeBytes, bo, FitBaseType.Sint16)
      case FitBaseType.Uint16  => codecT(sizeBytes, bo, FitBaseType.Uint16)
      case FitBaseType.Sint32  => codecT(sizeBytes, bo, FitBaseType.Sint32)
      case FitBaseType.Uint32  => codecT(sizeBytes, bo, FitBaseType.Uint32)
      case FitBaseType.String  => codecT(sizeBytes, bo, FitBaseType.String)
      case FitBaseType.Float32 => codecT(sizeBytes, bo, FitBaseType.Float32)
      case FitBaseType.Float64 => codecT(sizeBytes, bo, FitBaseType.Float64)
      case FitBaseType.Uint8z  => codecT(sizeBytes, bo, FitBaseType.Uint8z)
      case FitBaseType.Uint16z => codecT(sizeBytes, bo, FitBaseType.Uint16z)
      case FitBaseType.Uint32z => codecT(sizeBytes, bo, FitBaseType.Uint32z)
      case FitBaseType.Byte    => codecT(sizeBytes, bo, FitBaseType.Byte)
      case FitBaseType.Sint64  => codecT(sizeBytes, bo, FitBaseType.Sint64)
      case FitBaseType.Uint64  => codecT(sizeBytes, bo, FitBaseType.Uint64)
      case FitBaseType.Uint64z => codecT(sizeBytes, bo, FitBaseType.Uint64z)
    }).asInstanceOf[Codec[ArrayFieldType[_]]]

  def codecLong(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  )(implicit e: BaseTypeCodec[base.type, Long]): Codec[ArrayFieldType[Long]] =
    codecT[Long](sizeBytes, bo, base)

  def codecDouble(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  )(implicit e: BaseTypeCodec[base.type, Double]): Codec[ArrayFieldType[Double]] =
    codecT[Double](sizeBytes, bo, base)

  def codecString(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  )(implicit e: BaseTypeCodec[base.type, String]): Codec[ArrayFieldType[String]] =
    codecT[String](sizeBytes, bo, base)

  def codecT[T](
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  )(implicit e: BaseTypeCodec[base.type, T]): Codec[ArrayFieldType[T]] = {
    val bc = BaseTypeCodec.baseCodec(base, bo)
    if (sizeBytes > BaseTypeCodec.length(base)) {
      fixedSizeBytes(sizeBytes, list(bc))
        .xmapc(Nel.unsafeFromList)(_.toList)
        .xmapc(nl => ArrayFieldType(nl, base))(arr => arr.rawValue)
    } else {
      bc.xmapc(n => ArrayFieldType(Nel.of(n), base))(_.rawValue.head)
    }
  }

  object LongArray {
    def unapply(f: ArrayFieldType[_]): Option[Nel[Long]] =
      f.rawValue.head match {
        case _: LongBaseValue =>
          Some(f.rawValue.asInstanceOf[Nel[LongBaseValue]].map(_.rawValue))
        case _: Long =>
          Some(f.rawValue.asInstanceOf[Nel[Long]])
        case _ => None
      }
  }

  object FloatArray {
    def unapply(f: ArrayFieldType[_]): Option[Nel[Double]] =
      f.rawValue.head match {
        case _: FloatBaseValue =>
          Some(f.rawValue.asInstanceOf[Nel[FloatBaseValue]].map(_.rawValue))
        case _: Double =>
          Some(f.rawValue.asInstanceOf[Nel[Double]])
        case _ => None
      }
  }

  object StringArray {
    def unapply(f: ArrayFieldType[_]): Option[Nel[String]] =
      f.rawValue.head match {
        case _: StringBaseValue =>
          Some(f.rawValue.asInstanceOf[Nel[StringBaseValue]].map(_.rawValue))
        case _: String =>
          Some(f.rawValue.asInstanceOf[Nel[String]])
        case _ => None
      }
  }
}
