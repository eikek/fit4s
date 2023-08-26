package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs.fixedSizeBytes

sealed trait BaseTypedValue[V] extends TypedValue[V] {
  def base: FitBaseType
}

object BaseTypedValue {
  final case class LongBaseValue(rawValue: Long, base: FitBaseType)
      extends TypedValue[Long] {

    override def typeName = base.typeName
  }

  object LongBaseValue {

    def codec(bo: ByteOrdering, base: FitBaseType)(implicit
        e: BaseTypeCodec[base.type, Long]
    ): Codec[LongBaseValue] =
      BaseTypeCodec.baseCodec(base, bo).xmap(LongBaseValue(_, base), _.rawValue)
  }

  final case class FloatBaseValue(rawValue: Double, base: FitBaseType)
      extends BaseTypedValue[Double] {
    override def typeName = base.typeName
  }

  object FloatBaseValue {
    def codec(bo: ByteOrdering, base: FitBaseType)(implicit
        e: BaseTypeCodec[base.type, Double]
    ): Codec[FloatBaseValue] =
      BaseTypeCodec.baseCodec(base, bo).xmapc(FloatBaseValue(_, base))(_.rawValue)
  }

  final case class StringBaseValue(rawValue: String) extends TypedValue[String] {
    val base: FitBaseType = StringBaseValue.baseType
    override def typeName = "string"
  }

  object StringBaseValue {
    def codec(sizeBytes: Int): Codec[StringBaseValue] = {
      val sc = BaseTypeCodec.baseCodec(FitBaseType.String, ByteOrdering.LittleEndian)
      fixedSizeBytes(sizeBytes, sc).xmap(StringBaseValue.apply, _.rawValue)
    }

    val baseType: FitBaseType = FitBaseType.String
  }

  def codec(bt: FitBaseType, bo: ByteOrdering): Codec[BaseTypedValue[_]] =
    (bt match {
      case FitBaseType.Enum =>
        LongBaseValue.codec(bo, FitBaseType.Enum)
      case FitBaseType.Sint8 =>
        LongBaseValue.codec(bo, FitBaseType.Sint8)
      case FitBaseType.Uint8 =>
        LongBaseValue.codec(bo, FitBaseType.Uint8)
      case FitBaseType.Sint16 =>
        LongBaseValue.codec(bo, FitBaseType.Sint16)
      case FitBaseType.Uint16 =>
        LongBaseValue.codec(bo, FitBaseType.Uint16)
      case FitBaseType.Sint32 =>
        LongBaseValue.codec(bo, FitBaseType.Sint32)
      case FitBaseType.Uint32 =>
        LongBaseValue.codec(bo, FitBaseType.Uint32)
      case FitBaseType.String =>
        StringBaseValue.codec(BaseTypeCodec.length(bt))
      case FitBaseType.Float32 =>
        FloatBaseValue.codec(bo, FitBaseType.Float32)
      case FitBaseType.Float64 =>
        FloatBaseValue.codec(bo, FitBaseType.Float64)
      case FitBaseType.Uint8z =>
        LongBaseValue.codec(bo, FitBaseType.Uint8z)
      case FitBaseType.Uint16z =>
        LongBaseValue.codec(bo, FitBaseType.Uint16z)
      case FitBaseType.Uint32z =>
        LongBaseValue.codec(bo, FitBaseType.Uint32z)
      case FitBaseType.Byte =>
        LongBaseValue.codec(bo, FitBaseType.Byte)
      case FitBaseType.Sint64 =>
        LongBaseValue.codec(bo, FitBaseType.Sint64)
      case FitBaseType.Uint64 =>
        LongBaseValue.codec(bo, FitBaseType.Uint64)
      case FitBaseType.Uint64z =>
        LongBaseValue.codec(bo, FitBaseType.Uint64)
    }).asInstanceOf[Codec[BaseTypedValue[_]]]
}
