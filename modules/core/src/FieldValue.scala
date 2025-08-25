package fit4s.core

import fit4s.codec.*
import fit4s.codec.FitBaseValue$package.FitBaseValue.syntax.*
import fit4s.core.data.Display
import fit4s.profile.*

/** Combines profile information about a field with its decoded data. The data is already
  * adjusted to the scale/offset if applicable.
  */
final case class FieldValue(
    fieldName: String,
    fieldNumber: Int,
    baseType: FitBaseType,
    profileType: Option[ProfileType],
    unit: Option[MeasurementUnit],
    data: Vector[FitBaseValue]
):
  /** Looks up the value in the profile enumeration of the field type. */
  def asEnum: Option[ProfileEnum] =
    for
      key <- data.headOption.flatMap(_.asUInt)
      pt <- profileType
      v <- ProfileEnum(pt, key)
    yield v

  def as[A](using r: FieldReader[A]): Either[String, A] =
    r.read(this)

  def show(using d: Display[FieldValue]): String =
    d.show(this)

object FieldValue:
  extension (self: Option[FieldValue])
    def as[A](using r: FieldReader[A]): Option[Either[String, A]] =
      self.map(r.read)
    def to[A](using r: FieldReader[A]): Option[Either[String, A]] =
      self.map(r.read)

    def asEnum: Option[ProfileEnum] =
      self.flatMap(_.asEnum)

  def from(mf: MsgField, data: TypedDataField): FieldValue =
    val adjusts = ValueAdjust.from(mf.scale, List(mf.offset), data.data.size)
    val value = data.value(adjusts)
    FieldValue(
      mf.fieldName,
      mf.fieldDefNum,
      data.baseType,
      mf.profileType,
      mf.units.headOption,
      value
    )
