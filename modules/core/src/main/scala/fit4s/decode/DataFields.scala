package fit4s.decode

final class DataFields private (
    protected val allFields: List[DataField],
    protected val byName: Map[String, DataField.KnownField]
) {
  def getByName(fieldName: String): Option[DataField.KnownField] =
    byName.get(fieldName)

  def ++(other: DataFields): DataFields =
    new DataFields(allFields ::: other.allFields, byName.concat(other.byName))

  def flatMap(f: DataField => List[DataField]): DataFields =
    // TODO better perf
    DataFields(allFields.flatMap(f))
}

object DataFields {

  def apply(allFields: List[DataField]): DataFields = {
    val byName = allFields.flatMap {
      case f: DataField.KnownField => List(f.field.fieldName -> f)
      case _                       => Nil
    }.toMap
    new DataFields(allFields, byName)
  }

  def of(field: DataField*): DataFields =
    apply(field.toList)
}
