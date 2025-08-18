package fit4s.profile

/** Base trait for a profile message definition. */
trait MsgSchema:
  def globalNum: Int

  def allFields: Map[Int, MsgField]

/** Certain fields are common across all FIT messages. These fields have a reserved field
  * number that is also common across all FIT messages (including in the manufacturer
  * specific space).
  */
object CommonMsg:

  val messageIndex = MsgField(
    fieldDefNum = 254,
    fieldName = "message_index",
    profileType = Some(MessageIndexType),
    baseTypeName = MessageIndexType.baseType
  )

  val timestamp = MsgField(
    fieldDefNum = 253,
    fieldName = "timestamp",
    profileType = Some(DateTimeType),
    baseTypeName = DateTimeType.baseType,
    units = List(MeasurementUnit.Second)
  )

  val partIndex = MsgField(
    fieldDefNum = 250,
    fieldName = "part_index",
    profileType = None,
    baseTypeName = "uint32"
  )
