package fit4s

import scodec._
import scodec.codecs._

case class FieldDefinition(
  /** Uniquley identifies a spcific FIT field of the given FIT message. */
  fieldDefNum: Int

  /** Size of the defined field in bytes */
  , sizeBytes: Int

  /** Describes the FIT field as a specific type of FIT variable (uchar,
    * uint, etc). This allows the FIT decoder to appropriately handle
    * invalid or unknown data of this type.
    */
  , baseType: FieldDefinition.BaseType)


object FieldDefinition {

  case class BaseType(endianAbility: Boolean
    , reserved: Int
    , baseTypeNum: Int)

  object BaseType {

    def codec: Codec[BaseType] =
      (bool :: uint2 :: uint4).as[BaseType]
  }

  def codec: Codec[FieldDefinition] =
    (uint8 :: uint8 :: BaseType.codec).as[FieldDefinition]
}
