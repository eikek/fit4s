package fit4s.profile.messages

import fit4s.FieldDefinition
import fit4s.profile.FieldValue
import fit4s.profile.types._
import scodec.Codec
import scodec.bits.ByteOrdering

import java.util.concurrent.atomic.AtomicReference

abstract class Msg {
  private[this] val fields: AtomicReference[Map[Int, Msg.Field[TypedValue[_]]]] =
    new AtomicReference[Map[Int, Msg.Field[TypedValue[_]]]](Map.empty)

  private[this] val fieldsByName: AtomicReference[Map[String, Msg.Field[TypedValue[_]]]] =
    new AtomicReference[Map[String, Msg.Field[TypedValue[_]]]](Map.empty)

  protected def add[A <: TypedValue[_]](field: Msg.Field[A]): Msg.Field[A] = {
    val f = field.asInstanceOf[Msg.Field[TypedValue[_]]]
    fields.updateAndGet(
      _.updated(field.fieldDefinitionNumber, f)
    )
    fieldsByName.updateAndGet(
      _.updated(field.fieldName, f)
    )
    field
  }

  def globalMessageNumber: MesgNum

  lazy val allFields: List[Msg.Field[TypedValue[_]]] =
    fields.get().values.toList

  def findField(fieldDefNumber: Int): Option[Msg.Field[TypedValue[_]]] =
    fields.get().get(fieldDefNumber)

  def getFieldByName(name: String): Option[Msg.Field[TypedValue[_]]] =
    fieldsByName.get().get(name)
}

object Msg {

  sealed trait FieldAttributes {
    def fieldName: String
    def fieldBaseType: FitBaseType
    def isArray: ArrayDef
    def components: List[String]
    def scale: List[Double]
    def offset: Option[Double]
    def units: Option[String]
    def bits: List[Int]

    lazy val baseTypeLen: Int = BaseTypeCodec.length(fieldBaseType)

    lazy val unit: Option[MeasurementUnit] = units.map(MeasurementUnit.fromString)
  }

  sealed trait FieldWithCodec[A <: TypedValue[_]] extends FieldAttributes {
    def fieldName: String
    def fieldBaseType: FitBaseType
    def fieldCodec: FieldDefinition => ByteOrdering => Codec[A]
    def isArray: ArrayDef
    def components: List[String]
    def scale: List[Double]
    def offset: Option[Double]
    def units: Option[String]
    def bits: List[Int]
  }

  final case class Field[A <: TypedValue[_]](
      fieldDefinitionNumber: Int,
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: FieldDefinition => ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: List[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      subFields: () => List[SubField[_ <: TypedValue[_]]]
  ) extends FieldWithCodec[A]

  final case class SubField[A <: TypedValue[_]](
      references: List[ReferencedField[_]],
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: FieldDefinition => ByteOrdering => Codec[A],
      isArray: ArrayDef,
      components: List[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int]
  ) extends FieldWithCodec[A]

  final case class ReferencedField[V <: TypedValue[_]](
      refField: Msg.Field[V],
      refFieldValue: V
  ) {
    val asFieldValue: FieldValue[V] =
      FieldValue(refField, refFieldValue)
  }

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