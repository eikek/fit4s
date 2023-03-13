package fit4s

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

  case class BaseType(endianAbility: Boolean, reserved: Int, baseTypeNum: Int)

  object BaseType {

    def codec: Codec[BaseType] =
      (bool :: uint2 :: uintL(5)).as[BaseType]
  }

  def codec: Codec[FieldDefinition] =
    (uint8 :: uint8 :: BaseType.codec).as[FieldDefinition]
}
