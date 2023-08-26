package fit4s.build

import scala.util.Try

object model {
  @annotation.tailrec
  def snakeCamelType(str: String): String = {
    val digits = str.takeWhile(_.isDigit)
    if (digits.isEmpty) str.split('_').map(_.capitalize).mkString
    else snakeCamelType(str.drop(digits.length) + digits)
  }

  private val reserved = Set("type", "def", "val")
  def snakeCamelIdent(str: String): String = {
    val s = snakeCamelType(str)
    val ident = s.charAt(0).toLower + s.drop(1)
    if (reserved.contains(ident.toLowerCase)) s"`$ident`" else ident
  }

  final case class TypeDesc(
      name: String,
      baseType: String,
      valueName: String,
      value: Int,
      comment: Option[String]
  )

  final case class MessageFieldLine(
      messageName: String,
      fieldDefNumber: Int,
      fieldName: String,
      fieldType: String,
      isArray: ArrayDef,
      components: List[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      accumulate: List[Int],
      comment: Option[String],
      products: Option[String],
      example: Option[String],
      subFields: List[MessageSubFieldLine]
  ) {
    def addSubfield(f: MessageSubFieldLine): MessageFieldLine =
      copy(subFields = f :: subFields)
  }

  final case class MessageSubFieldLine(
      fieldName: String,
      fieldType: String,
      isArray: ArrayDef,
      components: List[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      accumulate: List[Int],
      refFieldName: List[String],
      refFieldValue: List[String],
      comment: Option[String]
  )

  final case class MessageDef(
      messageName: String,
      fields: List[MessageFieldLine]
  ) {
    def findField(name: String): Option[MessageFieldLine] =
      fields.find(_.fieldName.equalsIgnoreCase(name))
  }

  sealed trait ArrayDef
  object ArrayDef {
    case object NoArray extends ArrayDef
    case object DynamicSize extends ArrayDef
    case class Sized(n: Int) extends ArrayDef

    def fromString(str: String): Either[String, ArrayDef] =
      str.toUpperCase.trim match {
        case "[N]" => Right(DynamicSize)
        case s if s.startsWith("[") && s.endsWith("]") =>
          Try(s.drop(1).dropRight(1).toInt).toEither.left
            .map(_.getMessage)
            .map(Sized)

        case "" => Right(NoArray)
        case s  => Left(s"Unexpected array definition: $s")
      }
  }
}
