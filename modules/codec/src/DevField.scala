package fit4s.codec

import scodec.Err
import scodec.bits.BitVector

sealed trait DevField:
  def fold[A](ft: TypedDevField => A, fu: UntypedDevField => A): A
  def meta: DefinitionMessage.Meta
  def fieldDef: DevFieldDef
  def isTyped: Boolean
  final def toEither: Either[UntypedDevField, TypedDevField] =
    fold(Right(_), Left(_))

final case class TypedDevField(
    meta: DefinitionMessage.Meta,
    fieldDef: DevFieldDef,
    fieldDescription: TypedDevField.FieldDescription,
    data: Vector[FitBaseValue],
    rawData: BitVector,
    invalid: Boolean
) extends DevField:
  val isTyped: Boolean = true
  def fold[A](ft: TypedDevField => A, fu: UntypedDevField => A): A = ft(this)

  def isArray: Boolean =
    fieldDef.sizeBytes > baseType.size.toBytes

  def baseType = fieldDescription.baseType
  def fieldName: Option[String] = fieldDescription.fieldName

  /** Combines devIndex and fieldDefNumber in a single value. */
  val key: DevFieldId = fieldDescription.key
  def isInvalid: Boolean = invalid
  def isValid: Boolean = !invalid

  def value(adjust: Iterable[ValueAdjust] = Nil): Vector[FitBaseValue] =
    val va =
      if adjust.isEmpty then fieldDescription.valueAdjust(data.size)
      else adjust
    ValueAdjust.applyAll(baseType, va, data)

object TypedDevField:
  final case class FieldDescription(
      devIndex: Short,
      fieldDefNum: Short,
      fieldName: Option[String],
      baseType: FitBaseType,
      scale: List[Double],
      offset: Double,
      record: DataRecord
  ):
    val key: DevFieldId = DevFieldId(this)
    def valueAdjust(targetSize: Int) =
      ValueAdjust.from(scale, List(offset), targetSize)

final case class UntypedDevField(
    meta: DefinitionMessage.Meta,
    fieldDef: DevFieldDef,
    reason: UntypedDevField.Reason,
    data: BitVector
) extends DevField:
  def isTyped: Boolean = false

  def fold[A](ft: TypedDevField => A, fu: UntypedDevField => A): A = fu(this)

object UntypedDevField:
  enum Reason:
    case NoFieldDescription
    case Decode(baseType: FitBaseType, err: Err)
