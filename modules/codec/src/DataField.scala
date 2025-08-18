package fit4s.codec

import fit4s.codec.internal.FitBaseTypeCodec

import scodec.Err
import scodec.bits.BitVector

sealed trait DataField:
  def fold[A](ft: TypedDataField => A, fu: UntypedDataField => A): A
  def meta: DefinitionMessage.Meta
  def fieldDef: FieldDef
  def isTyped: Boolean
  final def toEither: Either[UntypedDataField, TypedDataField] =
    fold(Right(_), Left(_))

final case class TypedDataField(
    meta: DefinitionMessage.Meta,
    fieldDef: FieldDef,
    baseType: FitBaseType,
    data: Vector[FitBaseValue],
    invalid: Boolean
) extends DataField:
  val isTyped = true

  def rawValue: BitVector =
    FitBaseTypeCodec.encoder(meta.byteOrder, baseType).encode(data).require

  def fold[A](ft: TypedDataField => A, fu: UntypedDataField => A): A = ft(this)

  def isArray: Boolean =
    fieldDef.sizeBytes > baseType.size.toBytes

  def isInvalid: Boolean = invalid
  def isValid: Boolean = !invalid

  def value(adjust: Iterable[ValueAdjust] = Nil): Vector[FitBaseValue] =
    if adjust.isEmpty then data
    else ValueAdjust.applyAll(baseType, adjust, data)

/** Field that produced errors while decoding. Either the basetype couldn't be determined
  * or an error occured while decoding the data.
  *
  * A missing base type may still be able to decode with the message schema.
  */
final case class UntypedDataField(
    meta: DefinitionMessage.Meta,
    fieldDef: FieldDef,
    reason: UntypedDataField.Reason,
    data: BitVector
) extends DataField:
  val isTyped = false
  def fold[A](ft: TypedDataField => A, fu: UntypedDataField => A): A = fu(this)

object UntypedDataField:
  enum Reason:
    case NoBaseType
    case Decode(baseType: FitBaseType, err: Err)
