package fit4s.core

import java.nio.charset.StandardCharsets
import java.time.Instant

import fit4s.codec.*
import fit4s.core.MessageEncoder.EncodedField
import fit4s.profile.*

import scodec.bits.ByteOrdering

/** A builder helping to create fit files. */
trait FitBuilder:
  def build: FitFile

  def record[M <: MsgSchema](msg: M)(
      make: FitBuilder.RecordBuilder[M] => FitBuilder.RecordBuilder[M]
  ): FitBuilder
  def record[A: MessageEncoder](value: A): FitBuilder
  def records[A: MessageEncoder](value: Iterable[A]): FitBuilder

object FitBuilder:
  def newBuilder(fileId: FileId): FitBuilder =
    Builder(fileId)

  def newBuilder(
      ftype: FileType.type => Int,
      manufacturer: ProfileEnum | None.type = None,
      product: ProfileEnum | None.type = None,
      serialNumber: Long | None.type = None,
      createdAt: Instant | None.type = None,
      number: Int | None.type = None,
      productName: String = ""
  ): FitBuilder =
    Builder(
      FileId(
        ProfileEnum(FileType, ftype(FileType)),
        toOption(manufacturer),
        toOption(product),
        toOption(serialNumber),
        toOption(createdAt),
        toOption(number),
        Option(productName).filter(_.nonEmpty)
      )
    )

  private def toOption[A](v: A | None.type): Option[A] =
    v match
      case None => None
      case a    => Some(a.asInstanceOf[A])

  trait RecordBuilder[M <: MsgSchema]:
    def field[V](fn: M => MsgField, value: V)(using
        FieldValueEncoder[V]
    ): RecordBuilder[M]
    def fields(fs: EncodedField*): RecordBuilder[M]
    def fieldOpt[V](fn: M => MsgField, value: Option[V])(using
        FieldValueEncoder[V]
    ): RecordBuilder[M] = value.map(field(fn, _)).getOrElse(this)
    def set[A](value: A)(using MessageEncoder[A]): RecordBuilder[M]
    def build(header: NormalRecordHeader): (DefinitionRecord, DataRecord)

  final private case class RecordBuilderImpl[M <: MsgSchema](
      msg: M,
      fields: Vector[EncodedField] = Vector.empty
  ) extends RecordBuilder[M]:
    def field[V](fn: M => MsgField, value: V)(using
        enc: FieldValueEncoder[V]
    ): RecordBuilder[M] =
      val msgField = fn(msg)
      copy(fields =
        fields.appended(EncodedField(msgField, enc.fitValue(msgField, value)))
      )

    def fields(fs: EncodedField*): RecordBuilder[M] =
      copy(fields = fields ++ fs.toVector)

    def set[A](value: A)(using me: MessageEncoder[A]): RecordBuilder[M] =
      copy(fields = fields ++ me.encode(value).fields.toVector)

    def build(header: NormalRecordHeader): (DefinitionRecord, DataRecord) = {
      val meta: DefinitionMessage.Meta =
        DefinitionMessage.Meta(ByteOrdering.BigEndian, msg.globalNum)

      val defFields = Vector.newBuilder[FieldDef]
      val dataFields = Vector.newBuilder[TypedDataField]

      fields.filter(_.fitValue.nonEmpty).foreach { ef =>
        val values = ef.fitValue
        val fieldDefNum = ef.field.fieldDefNum
        val defField = ef.fitBaseType match
          case fbt @ FitBaseType.string =>
            val size =
              values.map(_.toString.getBytes(StandardCharsets.UTF_8).length + 1).sum
            FieldDef(
              fieldDefNum,
              size.toShort,
              FieldBaseType.from(fbt)
            )
          case fbt =>
            val size = fbt.size.toBytes * values.size
            FieldDef(
              fieldDefNum,
              size.toShort,
              FieldBaseType.from(fbt)
            )
        val dataField = TypedDataField(meta, defField, ef.fitBaseType, values, false)
        defFields.addOne(defField)
        dataFields.addOne(dataField)
      }

      val defMessage: DefinitionMessage =
        DefinitionMessage(meta, defFields.result().toList, Nil)

      val dataRecord =
        DataRecord(header, defMessage, None, dataFields.result(), Vector.empty)

      (DefinitionRecord(header, defMessage), dataRecord)
    }

  final private case class Builder(
      fileId: FileId,
      messageData: Vector[NormalRecordHeader => (DefinitionRecord, DataRecord)] =
        Vector.empty
  ) extends FitBuilder:
    self =>

    def build: FitFile =
      val (firstDef, firstData) =
        RecordBuilderImpl(FileIdMsg).set(fileId).build(NormalRecordHeader(false, 0))
      val records =
        messageData.zipWithIndex
          .map { case (makeRecords, idx) =>
            makeRecords(NormalRecordHeader(false, idx + 1))
          }
          .groupMap(_._1)(_._2)
          .flatMap { case (defRecord, dataRecords) =>
            defRecord +: dataRecords
          }
          .toVector
      FitFile(
        FileHeader(0, 0, ByteSize.zero, 0),
        firstDef +: firstData +: records,
        0
      )

    def record[M <: MsgSchema](msg: M)(
        make: FitBuilder.RecordBuilder[M] => FitBuilder.RecordBuilder[M]
    ): FitBuilder =
      val nd = make(RecordBuilderImpl(msg))
      copy(messageData = messageData.appended(nd.build))

    def record[A: MessageEncoder](value: A): FitBuilder =
      val em = summon[MessageEncoder[A]].encode(value)
      val md = RecordBuilderImpl(em.msg, em.fields.toVector)
      copy(messageData = messageData.appended(md.build))

    def records[A: MessageEncoder](value: Iterable[A]): FitBuilder =
      val me = summon[MessageEncoder[A]]
      val recs = value
        .map(me.encode)
        .map(em => RecordBuilderImpl(em.msg, em.fields.toVector).build)
        .toVector
      copy(messageData = messageData ++ recs)

  extension (ef: EncodedField)
    def fitBaseType: FitBaseType =
      FitBaseType
        .byName(ef.field.baseTypeName)
        .getOrElse(sys.error(s"No basetype found for profile field: ${ef.field}"))
