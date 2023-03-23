package fit4s

import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.Err

sealed trait FieldDecodeResult {
  def widen: FieldDecodeResult = this

  def isKnownSuccess: Boolean
}

object FieldDecodeResult {
  final case class InvalidValue(localField: FieldDefinition) extends FieldDecodeResult {
    val isKnownSuccess = false
  }

  final case class LocalSuccess(
      localField: FieldDefinition,
      value: TypedValue[_]
  ) extends FieldDecodeResult {
    val isKnownSuccess = false
  }

  final case class Success(
      localField: FieldDefinition,
      fieldValue: FieldValue[TypedValue[_]]
  ) extends FieldDecodeResult {
    val isKnownSuccess = true
  }

  final case class DecodeError(
      localField: FieldDefinition,
      err: Err
  ) extends FieldDecodeResult {
    val isKnownSuccess = false
  }

  final case class NoReferenceSubfield(
      localField: FieldDefinition,
      globalField: Msg.Field[TypedValue[_]]
  ) extends FieldDecodeResult {
    val isKnownSuccess = false
  }
}
