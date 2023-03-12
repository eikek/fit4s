package fit4s.build

import scala.util.Try

object model {

  final case class TypeDesc(
      name: String,
      baseType: String,
      valueName: String,
      value: Int,
      comment: Option[String]
  )

  final case class MessageDef(
      messageName: String,
      fieldDefNumber: Option[Int],
      fieldName: String,
      fieldType: String,
      isArray: ArrayDef,
      components: Option[String],
      scale: List[Double],
      offset: Option[Double],
      units: Option[String],
      bits: List[Int],
      accumulate: List[Int],
      refFieldName: Option[String],
      refFieldValue: Option[String],
      comment: Option[String],
      products: Option[String],
      example: Option[String]
  )

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
