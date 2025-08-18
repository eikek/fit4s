package fit4s.codec

import fit4s.codec.internal.RecordCodec

import scodec.bits.ByteVector

sealed trait Record:
  def recordHeader: RecordHeader
  def fold[A](fdm: DefinitionRecord => A, fdd: DataRecord => A): A
  def encoded: ByteVector
  def localMessageType = recordHeader.localMessageType
  def isDataRecord: Boolean = fold(_ => false, _ => true)
  def isDefinitionRecord: Boolean = fold(_ => true, _ => false)
  def toEither: Either[DefinitionRecord, DataRecord] =
    fold(Left(_), Right(_))
  def globalMessage: Int

final case class DefinitionRecord(
    header: NormalRecordHeader,
    message: DefinitionMessage
) extends Record:
  val recordHeader = header
  val globalMessage: Int = message.meta.globalMessageNum
  def fold[A](fdm: DefinitionRecord => A, fdd: DataRecord => A): A = fdm(this)
  def encoded: ByteVector =
    RecordCodec.defRecord.encode(this).require.bytes

  override def toString(): String = s"DefinitionRecord($header, $message)"

final case class DataRecord(
    header: RecordHeader,
    definition: DefinitionMessage,
    lastTimestamp: Option[Long],
    fields: Vector[DataField],
    devFields: Vector[DevField]
) extends Record:
  val recordHeader = header
  val globalMessage = definition.meta.globalMessageNum
  def fold[A](fdm: DefinitionRecord => A, fdd: DataRecord => A): A = fdd(this)
  def encoded: ByteVector =
    RecordCodec.dataRecordEncoder.encode(this).require.bytes

  override def toString(): String =
    s"DataRecord($header, #fields=${fields.size}, #devFields=${devFields.size})"

  /** All known, decoded data fields with valid values. */
  lazy val typedFields: Vector[TypedDataField] =
    fields.flatMap(_.toEither.toOption.filter(_.isValid))

  lazy val typedDevFields: Vector[TypedDevField] =
    devFields.flatMap(_.toEither.toOption.filter(_.isValid))

  def findFieldByNum(num: Int): Option[TypedDataField] =
    typedFields.find(_.fieldDef.fieldDefNumber == num)

  def fieldData(num: Int): Option[Vector[FitBaseValue]] =
    findFieldByNum(num).map(_.data)

  def findDevField(id: DevFieldId): Option[TypedDevField] =
    typedDevFields.find(_.key == id)

  def devFieldData(id: DevFieldId): Option[Vector[FitBaseValue]] =
    findDevField(id).map(_.data)

  /** The timestamp field is common to all messages. If it is not found in the message, it
    * is derived from the compressed timestamp header, if applicable. Otherwise there is
    * no timestamp.
    */
  lazy val timestamp: Option[Long] =
    val fromField =
      fields.collectFirst {
        case df: TypedDataField if df.fieldDef.fieldDefNumber == 253 =>
          df.value().headOption.flatMap(FitBaseValue.toLong)
      }.flatten
    fromField.orElse {
      (header, lastTimestamp) match
        case (h @ CompressedTimestampHeader(_, _), Some(ref)) =>
          Some(h.resolve(ref))
        case _ =>
          None
    }
