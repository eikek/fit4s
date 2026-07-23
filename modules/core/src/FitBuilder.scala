package fit4s.core

import java.nio.charset.StandardCharsets
import java.time.Instant

import fit4s.codec.*
import fit4s.core.FitMessage.toMsgField
import fit4s.core.MessageEncoder.EncodedDevField
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
    def devField[V](fd: FieldDescription, value: V)(using
        enc: FieldValueEncoder[V]
    ): RecordBuilder[M]
    def fields(fs: EncodedField*): RecordBuilder[M]
    def devFields(fs: EncodedDevField*): RecordBuilder[M]
    def fieldOpt[V](fn: M => MsgField, value: Option[V])(using
        FieldValueEncoder[V]
    ): RecordBuilder[M] = value.map(field(fn, _)).getOrElse(this)
    def set[A](value: A)(using MessageEncoder[A]): RecordBuilder[M]
    def build: (DefinitionMessage, Vector[DataField], Vector[DevField])

  final private case class RecordBuilderImpl[M <: MsgSchema](
      msg: M,
      fields: Vector[EncodedField] = Vector.empty,
      devFields: Vector[EncodedDevField] = Vector.empty
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

    def devFields(fs: EncodedDevField*): RecordBuilder[M] =
      copy(devFields = devFields ++ fs.toVector)

    def set[A](value: A)(using me: MessageEncoder[A]): RecordBuilder[M] =
      copy(fields = fields ++ me.encode(value).fields.toVector)

    def devField[V](fd: FieldDescription, value: V)(using
        enc: FieldValueEncoder[V]
    ): RecordBuilder[M] =
      val ev = enc.fitValue(fd.toMsgField, value)
      copy(devFields = devFields.appended(EncodedDevField(fd, ev)))

    def build: (DefinitionMessage, Vector[DataField], Vector[DevField]) = {
      val meta: DefinitionMessage.Meta =
        DefinitionMessage.Meta(ByteOrdering.BigEndian, msg.globalNum)

      val defFields = Vector.newBuilder[FieldDef]
      val dataFields = Vector.newBuilder[TypedDataField]

      fields.filter(_.fitValue.nonEmpty).foreach { ef =>
        val values = ef.fitValue
        val fieldDefNum = ef.field.fieldDefNum
        val size = computeSize(values, ef.fitBaseType)
        val defField = FieldDef(fieldDefNum, size, FieldBaseType.from(ef.fitBaseType))
        val dataField = TypedDataField(meta, defField, ef.fitBaseType, values, false)
        defFields.addOne(defField)
        dataFields.addOne(dataField)
      }

      val devDeffields = Vector.newBuilder[DevFieldDef]
      val devDataFields = Vector.newBuilder[TypedDevField]
      devFields.filter(_.fitValue.nonEmpty).foreach { ef =>
        val size = computeSize(ef.fitValue, ef.field.baseType)
        val defField = DevFieldDef(ef.field.fieldDefNum, size, ef.field.devDataIdx)
        val dataField = TypedDevField(meta, defField, ef.field, ef.fitValue, false)
        devDeffields.addOne(defField)
        devDataFields.addOne(dataField)
      }

      val defMessage: DefinitionMessage =
        DefinitionMessage(meta, defFields.result().toList, devDeffields.result().toList)

      (defMessage, dataFields.result(), devDataFields.result())
    }

  final private case class Builder(
      fileId: FileId,
      messageData: Vector[(DefinitionMessage, Vector[DataField], Vector[DevField])] =
        Vector.empty
  ) extends FitBuilder:
    self =>

    def build: FitFile =
      val (firstDef, firstData, _) =
        RecordBuilderImpl(FileIdMsg).set(fileId).build
      val first = Vector(
        DefinitionRecord(NormalRecordHeader(false, 0), firstDef),
        DataRecord(NormalRecordHeader(false, 0), firstDef, None, firstData, Vector.empty)
      )
      // add records for each dev-field field-description
      val devFieldMsg = messageData
        .flatMap(_._3)
        .flatMap(_.fold(e => Some(e.fieldDescription), _ => None))
        .distinct
        .flatMap { fdd =>
          val (defm, df, _) = RecordBuilderImpl(FieldDescriptionMsg).set(fdd).build
          val rh = NormalRecordHeader(false, 1)
          Vector(DefinitionRecord(rh, defm), DataRecord(rh, defm, None, df, Vector.empty))
        }

      val records =
        messageData
          .groupBy(_._1)
          .zipWithIndex
          .flatMap { case ((defMsg, vs), idx) =>
            val rheader = NormalRecordHeader(false, idx % 15)
            val defr = DefinitionRecord(idx % 15, defMsg)
            val datr = vs.map { case (_, fs, ds) =>
              DataRecord(rheader, defMsg, None, fs, ds)
            }
            defr +: datr
          }
          .toVector

      FitFile(
        FileHeader(0, 0, ByteSize.zero, 0),
        first ++ devFieldMsg ++ records,
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

  private def computeSize(values: Vector[FitBaseValue], ft: FitBaseType): Short =
    ft match
      case fbt @ FitBaseType.string =>
        values.map(_.toString.getBytes(StandardCharsets.UTF_8).length + 1).sum.toShort
      case fbt =>
        (fbt.size.toBytes * values.size).toShort
