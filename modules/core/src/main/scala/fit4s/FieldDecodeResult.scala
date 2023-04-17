package fit4s

import fit4s.profile.FieldValue
import fit4s.profile.types.TypedValue

import scodec.Err

sealed trait FieldDecodeResult {
  def widen: FieldDecodeResult = this

  def asSuccess: Option[FieldDecodeResult.Success]
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
}
