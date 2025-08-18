package fit4s.core
package internal

import fit4s.codec.DevFieldId
import fit4s.codec.FitBaseType
import fit4s.core.MessageReader as MR
import fit4s.profile.*

final case class FieldDescription(
    devDataIdx: Short,
    fieldDefNum: Short,
    fieldName: Option[String],
    baseType: FitBaseType,
    offset: Option[Double],
    scale: Option[Vector[Double]],
    components: Option[String],
    units: Option[String],
    bits: Option[String],
    nativeMsgNum: Option[ProfileEnum],
    nativeFieldNum: Option[Int]
):

  val devFieldId: DevFieldId = DevFieldId(devDataIdx, fieldDefNum)

  def toMsgField = MsgField(
    fieldDefNum = devFieldId.toInt,
    fieldName = fieldName.getOrElse(s"dev field ${devDataIdx}/${fieldDefNum}"),
    profileType = None,
    baseTypeName = baseType.name,
    components = components.toList.flatMap(split),
    scale = scale.map(_.toList).getOrElse(Nil),
    offset = offset.getOrElse(0d),
    units = units.toList.flatMap(split).map(MeasurementUnit.fromString),
    bits = bits.toList.flatMap(split).flatMap(_.toIntOption),
    subFields = Nil
  )

  private def split(s: String): List[String] =
    s.split(',').map(_.trim).filter(_.nonEmpty).toList

object FieldDescription:

  given MR[FieldDescription] =
    MR.forMsg(FieldDescriptionMsg) { m =>
      (MR.field(m.developerDataIndex).as[Short] ::
        MR.field(m.fieldDefinitionNumber).as[Short] ::
        MR.field(m.fieldName).as[String].option ::
        MR.field(m.fitBaseTypeId)
          .asEnum
          .emap(pt =>
            FitBaseType.byName(pt.value).toRight(s"No fit base type found for $pt")
          ) ::
        MR.field(m.offset).as[Double].option ::
        MR.field(m.scale).as[Vector[Double]].option ::
        MR.field(m.components).as[String].option ::
        MR.field(m.units).as[String].option ::
        MR.field(m.bits).as[String].option ::
        MR.field(m.nativeMesgNum).asEnum.option ::
        MR.field(m.nativeFieldNum).as[Int].option.tuple).as[FieldDescription]
    }
