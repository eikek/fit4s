package fit4s.profile.types

import fit4s.data.Nel
import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs.{fixedSizeBytes, list}

final case class ArrayFieldType[A <: TypedValue](values: Nel[A]) extends TypedValue {
  val rawValue = -1L // TODO remove?
  val typeName = "a[]"
}

object ArrayFieldType {

  def codecLong(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  ): Codec[ArrayFieldType[LongFieldType]] = {
    val bc = BaseTypeCodec.baseCodec(base)(bo)
    if (sizeBytes > BaseTypeCodec.length(base)) {
      fixedSizeBytes(sizeBytes, list(bc))
        .xmapc(Nel.unsafeFromList)(_.toList)
        .xmapc(_.map(LongFieldType(_, base)))(_.map(_.rawValue))
        .xmapc(ArrayFieldType.apply)(_.values)
    } else {
      bc.xmap(n => ArrayFieldType(Nel.of(LongFieldType(n, base))), _.values.head.rawValue)
    }
  }

  object LongArray {
    def unapply(f: ArrayFieldType[_]): Option[Nel[Long]] =
      f.values.head match {
        case _: LongFieldType =>
          Some(f.values.map(_.asInstanceOf[LongFieldType]).map(_.rawValue))
        case _ => None
      }
  }
}
