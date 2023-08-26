package fit4s.decode

import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue

final class DataFields private (
    val allFields: Vector[DataField],
    protected val byName: Map[String, DataField.KnownField]
) {
  lazy val size = allFields.size

  def getByName(fieldName: String): Option[DataField.KnownField] =
    byName.get(fieldName)

  def get(field: Msg.FieldAttributes): Option[DataField.KnownField] =
    getByName(field.fieldName)

  def getDecodedValue(field: Msg.FieldAttributes): Option[TypedValue[_]] =
    for {
      field <- get(field)
      decoded <- field.decodedValue.toOption
      value <- decoded.asSuccess
    } yield value.fieldValue.value

  def getDecodedField[A <: TypedValue[_]](
      field: Msg.FieldWithCodec[A]
  ): Either[String, Option[FieldValue[A]]] =
    get(field)
      .map(_.decodedValue.toEither.left.map(_.messageWithContext))
      .map(_.map(_.asSuccess.map(_.fieldValue.asInstanceOf[FieldValue[A]])))
      .getOrElse(Right(None))

  def ++(other: DataFields): DataFields =
    new DataFields(allFields ++ other.allFields, byName.concat(other.byName))

  def +:(field: DataField): DataFields =
    new DataFields(
      allFields :+ field,
      field match {
        case f: DataField.KnownField =>
          byName.updated(f.field.fieldName, f)
        case _ => byName
      }
    )

  def flatMap(f: DataField => DataFields): DataFields =
    // TODO better perf
    DataFields(allFields.flatMap(field => f(field).allFields))

  def filter(f: DataField => Boolean): DataFields =
    flatMap(df => if (f(df)) DataFields.of(df) else DataFields.empty)

  override def toString =
    s"DataFields($allFields)"

  override def equals(obj: Any) = obj match {
    case t: DataFields => t.allFields == allFields
    case _             => false
  }

  override def hashCode() = allFields.hashCode()
}

object DataFields {

  val empty: DataFields = DataFields(Vector.empty)

  def apply(allFields: Vector[DataField]): DataFields = {
    val byName = allFields.flatMap {
      case f: DataField.KnownField => List(f.field.fieldName -> f)
      case _                       => Nil
    }.toMap
    new DataFields(allFields, byName)
  }

  def of(field: DataField*): DataFields =
    apply(field.toVector)
}
