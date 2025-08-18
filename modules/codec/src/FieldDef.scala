package fit4s.codec

final case class FieldDef(
    fieldDefNumber: Int,
    sizeBytes: Short,
    baseType: FieldBaseType
):
  def size: ByteSize = ByteSize.bytes(sizeBytes.toLong)

/** Field definition for a developer data field.
  *
  * The field-description message defines the base type for this field and other profile
  * data.
  *
  * @param fieldDefNumber
  *   maps to the `field_def_number` field in a field-description message
  * @param sizeBytes
  *   the size in bytes
  * @param devIndex
  *   maps to the `dev_index` of a developer-data-id message
  */
final case class DevFieldDef(
    fieldDefNumber: Int,
    sizeBytes: Short,
    devIndex: Short
):
  def size: ByteSize = ByteSize.bytes(sizeBytes.toLong)

  /** Combines devIndex and fieldDefNumber in a single value. */
  val key: DevFieldId = DevFieldId(this)
