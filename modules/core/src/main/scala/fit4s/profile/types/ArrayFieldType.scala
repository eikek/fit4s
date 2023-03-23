package fit4s.profile.types

import fit4s.data.Nel
import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs.{fixedSizeBytes, list}

final case class ArrayFieldType[A](rawValue: Nel[A], base: FitBaseType)
    extends TypedValue[Nel[A]] {
  val typeName = base.typeName
}

object ArrayFieldType {

  def codecLong(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  )(implicit e: BaseTypeCodec[base.type, Long]): Codec[ArrayFieldType[Long]] = {
    val bc = BaseTypeCodec.baseCodec(base, bo)
    if (sizeBytes > BaseTypeCodec.length(base)) {
      fixedSizeBytes(sizeBytes, list(bc))
        .xmapc(Nel.unsafeFromList)(_.toList)
        .xmapc(_.map(LongTypedValue(_, base)))(_.map(_.rawValue))
        .xmapc(nl => ArrayFieldType(nl.map(_.rawValue), nl.head.base))(arr =>
          arr.rawValue.map(n => LongTypedValue(n, arr.base))
        )
    } else {
      bc.xmapc(n => ArrayFieldType(Nel.of(n), base))(_.rawValue.head)
    }
  }

  object LongArray {
    def unapply(f: ArrayFieldType[_]): Option[Nel[Long]] =
      f.rawValue.head match {
        case _: LongTypedValue =>
          Some(f.rawValue.asInstanceOf[Nel[LongTypedValue]].map(_.rawValue))
        case _: Long =>
          Some(f.rawValue.asInstanceOf[Nel[Long]])
        case _ => None
      }
  }
}
