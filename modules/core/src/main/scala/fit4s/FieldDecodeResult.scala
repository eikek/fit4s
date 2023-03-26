package fit4s

import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.Err

sealed trait FieldDecodeResult {
  def widen: FieldDecodeResult = this

  def asSuccess: Option[FieldDecodeResult.Success]

  final def isKnownSuccess: Boolean = asSuccess.isDefined
}

object FieldDecodeResult {
  final case class InvalidValue(localField: FieldDefinition) extends FieldDecodeResult {
    val asSuccess = None
  }

  final case class LocalSuccess(
      localField: FieldDefinition,
      value: TypedValue[_]
  ) extends FieldDecodeResult {
    val asSuccess = None
  }

  final case class Success(
      localField: FieldDefinition,
      fieldValue: FieldValue[TypedValue[_]]
  ) extends FieldDecodeResult {
    val asSuccess = Some(this)
  }

  final case class DecodeError(
      localField: FieldDefinition,
      err: Err
  ) extends FieldDecodeResult {
    val asSuccess = None
  }

  final case class NoReferenceSubfield(
      localField: FieldDefinition,
      globalField: Msg.Field[TypedValue[_]]
  ) extends FieldDecodeResult {
    val asSuccess = None
  }
}
