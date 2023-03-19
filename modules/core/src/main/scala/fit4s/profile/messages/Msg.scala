package fit4s.profile.messages

import fit4s.profile.types._
import scodec.Codec
import scodec.bits.ByteOrdering

import java.util.concurrent.atomic.AtomicReference

abstract class Msg {
  private[this] val fields: AtomicReference[Map[Int, Msg.Field[GenFieldType]]] =
    new AtomicReference[Map[Int, Msg.Field[GenFieldType]]](Map.empty)

  protected def add[A <: GenFieldType](field: Msg.Field[A]): Msg.Field[A] = {
    fields.updateAndGet(
      _.updated(field.fieldDefinitionNumber, field.asInstanceOf[Msg.Field[GenFieldType]])
    )
    field
  }

  def globalMessageNumber: MesgNum

  lazy val allFields: List[Msg.Field[GenFieldType]] =
    fields.get().values.toList

  def findField(fieldDefNumber: Int): Option[Msg.Field[GenFieldType]] =
    fields.get().get(fieldDefNumber)
}

object Msg {

  final case class Field[A <: GenFieldType](
      fieldDefinitionNumber: Int,
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: Option[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      subFields: List[SubField[_ <: GenFieldType]]
  ) {
    lazy val baseTypeLen: Int = BaseTypeCodec.length(fieldBaseType)
    lazy val baseTypeCodec: ByteOrdering => Codec[Long] =
      BaseTypeCodec.baseCodec(fieldBaseType)

    lazy val unit: Option[MeasurementUnit] = units.map(MeasurementUnit.fromString)

    val isDynamicField: Boolean = subFields.nonEmpty
  }

  final case class SubField[A <: GenFieldType](
      references: List[ReferencedField[_]],
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: Option[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int]
  )

  final case class ReferencedField[V <: GenFieldType](
      refField: Msg.Field[V],
      refFieldValue: V
  )

  sealed trait ArrayDef

  object ArrayDef {
    case object NoArray extends ArrayDef
    case object DynamicSize extends ArrayDef
    final case class Sized(n: Int) extends ArrayDef

    val noArray: ArrayDef = NoArray
    val dynamic: ArrayDef = DynamicSize
    def sized(n: Int): ArrayDef = Sized(n)
  }
}
