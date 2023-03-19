package fit4s.profile.messages

import fit4s.FieldDefinition
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

  trait FieldWithCodec[A <: GenFieldType] {
    def fieldName: String
    def fieldBaseType: FitBaseType
    def fieldCodec: FieldDefinition => ByteOrdering => Codec[A]
    def isArray: ArrayDef
    def components: Option[String]
    def scale: List[Double]
    def offset: Option[Double]
    def units: Option[String]
    def bits: List[Int]

    lazy val baseTypeLen: Int = BaseTypeCodec.length(fieldBaseType)
    lazy val baseTypeCodec: ByteOrdering => Codec[Long] =
      BaseTypeCodec.baseCodec(fieldBaseType)

    lazy val unit: Option[MeasurementUnit] = units.map(MeasurementUnit.fromString)
  }

  final case class Field[A <: GenFieldType](
      fieldDefinitionNumber: Int,
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: FieldDefinition => ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: Option[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      subFields: List[SubField[_ <: GenFieldType]]
  ) extends FieldWithCodec[A] {
    val isDynamicField: Boolean = subFields.nonEmpty
  }

  final case class SubField[A <: GenFieldType](
      references: List[ReferencedField[_]],
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: FieldDefinition => ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: Option[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int]
  ) extends FieldWithCodec[A]

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
