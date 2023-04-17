package fit4s

import fit4s.profile.types.FitBaseType
import scodec._
import scodec.codecs._

/** @param fieldDefNum
  *   The Field Definition Number uniquely identifies a specific FIT field of the given
  *   FIT message. The field definition numbers for each global FIT message are provided
  *   in the SDK. 255 represents an invalid field number.
  * @param sizeBytes
  *   Size (in bytes) of the specified FIT message’s field. The Size indicates the size of
  *   the defined field in bytes. The size may be a multiple of the underlying FIT Base
  *   Type size indicating the field contains multiple elements represented as an array.
  * @param baseType
  *   Base type of the specified FIT message’s field. Base Type describes the FIT field as
  *   a specific type of FIT variable (unsigned char, signed short, etc). This allows the
  *   FIT decoder to appropriately handle invalid or unknown data of this type.
  */
final case class FieldDefinition(
    fieldDefNum: Int,
    sizeBytes: Int,
    baseType: FieldDefinition.BaseType
)

object FieldDefinition {

  case class BaseType(
      decoded: BaseType.Raw,
      fitBaseType: FitBaseType
  )

  object BaseType {

    final case class Raw(endianAbility: Boolean, reserved: Int, baseTypeNum: Int)
    object Raw {
      def codec: Codec[Raw] =
        (bool :: uint2L :: uintL(5)).as[Raw]
    }

    def codec: Codec[BaseType] =
      Raw.codec
        .flatZip[FitBaseType] { raw =>
          FitBaseType.byOrdinal(raw.baseTypeNum) match {
            case Some(ft) => provide(ft)
            case None =>
              provide(
                FitBaseType.Uint8
              ) // fail(Err(s"Failed to lookup fit base type for: $raw"))
          }
        }
        .flattenLeftPairs
        .as[BaseType]
  }

  def codec: Codec[FieldDefinition] =
    (uint8L :: uint8L :: BaseType.codec).as[FieldDefinition]
}
