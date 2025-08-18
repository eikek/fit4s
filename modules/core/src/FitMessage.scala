package fit4s.core

import fit4s.codec.*
import fit4s.core.data.DateTime
import fit4s.core.internal.{ComponentField, FieldDescription}
import fit4s.profile.*

import scodec.Attempt
import scodec.bits.BitVector

/** Combines the data fields from a fit data record with a message schema from the fit
  * profile.
  */
final case class FitMessage(
    mesgNum: Int,
    schema: Map[Int, MsgField],
    timestamp: Option[DateTime],
    data: Map[Int, TypedDataField]
):
  private val schemaByName: Map[String, MsgField] =
    schema.map { case (_, v) => (v.fieldName, v) }

  private def addData(f: TypedDataField): FitMessage =
    copy(data = data.updated(f.fieldDef.fieldDefNumber, f))

  private def addSchema(f: MsgField): FitMessage =
    copy(schema = schema.updated(f.fieldDefNum, f))

  private def dataFieldImpl(key: Int | String): Option[TypedDataField] =
    key match
      case name: String => schemaByName.get(name).flatMap(f => data.get(f.fieldDefNum))
      case num: Int     => data.get(num)

  private def schemaFieldImpl(key: Int | String): Option[MsgField] =
    key match
      case n: Int    => schema.get(n)
      case n: String => schemaByName.get(n)

  def dataField[N](n: N)(using g: GetFieldNumber[N]): Option[TypedDataField] =
    dataFieldImpl(g.get(n))

  def schemaField[N](n: N)(using g: GetFieldNumber[N]): Option[MsgField] =
    schemaFieldImpl(g.get(n))

  def field[N](n: N)(using GetFieldNumber[N]): Option[FieldValue] =
    (schemaField(n), dataField(n)) match
      case (Some(sf), Some(d)) => Some(FieldValue.from(sf, d))
      case _                   => None

  def allValues: Vector[FieldValue] =
    data.values
      .flatMap(dataField =>
        schemaField(dataField).map(schema => FieldValue.from(schema, dataField))
      )
      .toVector

  def allDevValues: Vector[FieldValue] =
    allValues.filter(fv => DevFieldId.isDevFieldId(fv.fieldNumber))

  def as[R](using r: MessageReader[R]): Either[String, Option[R]] =
    r.read(this)

  override def toString(): String =
    s"FitMessage(mesgNum=$mesgNum, schemaFields=${schema.size}, dataFields=${data.size})"

object FitMessage:

  def apply(
      record: DataRecord,
      msg: MsgSchema,
      withSubFields: Boolean = true,
      withComponents: Boolean = true,
      withDeveloperFields: Boolean = true
  ): FitMessage =
    val init = new FitMessage(
      msg.globalNum,
      msg.allFields.values.map(f => f.fieldDefNum -> f).toMap,
      record.timestamp.map(DateTime.apply),
      record.typedFields.map(t => t.fieldDef.fieldDefNumber.toInt -> t).toMap
    )
    applyMods(init)(
      enableMod(withDeveloperFields, addDeveloperFields(_, record)),
      enableMod(withSubFields, replaceSubFields),
      enableMod(withComponents, expandComponents)
    )

  private def enableMod(
      flag: Boolean,
      f: FitMessage => FitMessage
  ): FitMessage => FitMessage =
    if flag then f else identity

  private def applyMods(init: FitMessage)(fs: (FitMessage => FitMessage)*): FitMessage =
    fs.foldLeft(init)((el, f) => f(el))

  /** SubFields are used to interpret the data depending on the value of another field.
    * Resolving sub fields changes the schema, adopting it to decode the corresponding
    * data correctly.
    *
    * https://developer.garmin.com/fit/protocol/#dynamicfields
    */
  private[core] def replaceSubFields(m: FitMessage): FitMessage =
    val resolved = m.schema.values.flatMap { f =>
      f.subFields
        .find(subFieldApplies(m.data))
        .map(f.merge)
        .map(f.fieldDefNum -> _)
    }.toMap
    m.copy(schema = m.schema ++ resolved)

  /** Components allow to generate more data fields from another field. Resolving
    * components means to generate new data that is extracted from some field to adopt
    * data to fields existing in the schema.
    *
    * https://developer.garmin.com/fit/protocol/#components
    */
  private[core] def expandComponents(m: FitMessage): FitMessage =
    val fieldsToExpand = m.schema.values.filter(_.components.nonEmpty).toList
    expandFields(fieldsToExpand, m)

  @annotation.tailrec
  private def expandFields(fieldsToExpand: List[MsgField], m: FitMessage): FitMessage =
    fieldsToExpand match
      case Nil                  => m
      case parentSchema :: rest =>
        m.dataField(parentSchema) match
          case None =>
            expandFields(rest, m)
          case Some(parentData) =>
            val rawValue = parentData.rawValue
            val components = ComponentField.from(parentSchema)
            val nextM = expandField(components, rawValue, parentData, m)
            expandFields(rest, nextM)

  @annotation.tailrec
  private def expandField(
      components: List[ComponentField],
      data: BitVector,
      parent: TypedDataField,
      m: FitMessage
  ): FitMessage =
    components match
      case Nil        => m
      case cf :: rest =>
        val bits = data.take(cf.bits)
        if (bits.length < cf.bits) m
        else if (m.dataField(cf.name).isDefined)
          expandField(rest, data.drop(cf.bits), parent, m)
        else
          m.schemaByName.get(cf.name) match {
            case None =>
              // target field is not in the schema, this should not happen
              expandField(rest, data.drop(cf.bits), parent, m)

            case Some(targetField) =>
              FitBaseType.byName(targetField.baseTypeName) match
                case None =>
                  expandField(rest, data.drop(cf.bits), parent, m)
                case Some(baseType) =>
                  val dataField =
                    createExpandedField(baseType, parent, targetField, bits)
                  val nextM =
                    dataField.fold(m.addData, _ => m).addSchema(cf.merge(targetField))
                  expandField(rest, data.drop(cf.bits), parent, nextM)
          }

  private def createExpandedField(
      targetBaseType: FitBaseType,
      parent: TypedDataField,
      targetField: MsgField,
      bits: BitVector
  ): DataField =
    val data = targetBaseType.align(parent.meta.byteOrder, bits)
    val fieldDef = FieldDef(
      targetField.fieldDefNum.toShort,
      (data.size / 8).toShort,
      targetBaseType.toFieldBaseType
    )
    val decoder = targetBaseType
      .codec(parent.meta.byteOrder, ByteSize.bits(data.size))
      .complete

    decoder.decodeValue(data) match
      case Attempt.Successful(fv) =>
        TypedDataField(
          parent.meta,
          fieldDef,
          targetBaseType,
          fv,
          targetBaseType.isInvalid(parent.meta.byteOrder, bits)
        )

      case Attempt.Failure(err) =>
        val reason = UntypedDataField.Reason.Decode(targetBaseType, err)
        UntypedDataField(parent.meta, fieldDef, reason, bits)

  private def subFieldApplies(
      dataFields: Map[Int, TypedDataField]
  )(sf: SubField): Boolean =
    sf.references.exists(ref =>
      dataFields
        .get(ref.refField.fieldDefNum)
        .map(_.data)
        .exists(_ == Vector(ref.refFieldValue))
    )

  /** Developer fields are carrying their profile in a special message in the same fit
    * file. The message is read here and the profile schema created accordingly. Then
    * these fields are added as normal fields to this message. The fieldNumber is treated
    * specially to not overwrite other "normal" fields.
    */
  private def addDeveloperFields(m: FitMessage, dr: DataRecord) =
    dr.typedDevFields.foldLeft(m) { (msg, devField) =>
      FitMessage(
        devField.fieldDescription.record,
        FieldDescriptionMsg,
        false,
        false,
        false
      ).as[FieldDescription] match
        case Right(Some(fd)) =>
          val fieldDef =
            FieldDef(
              devField.key.toInt,
              devField.fieldDef.sizeBytes,
              FieldBaseType.from(devField.baseType)
            )
          val data = TypedDataField(
            devField.meta,
            fieldDef,
            devField.baseType,
            devField.data,
            devField.invalid
          )
          msg.addData(data).addSchema(fd.toMsgField)
        case _ =>
          msg
    }
