package fit4s.activities.data

import cats.data.NonEmptyList

sealed trait PositionName extends Product {

  final def name: String =
    productPrefix.toLowerCase
}

object PositionName {

  case object Start extends PositionName
  case object End extends PositionName

  def all: NonEmptyList[PositionName] =
    NonEmptyList.of(Start, End)

  def fromString(str: String): Either[String, PositionName] =
    all
      .find(_.name.equalsIgnoreCase(str))
      .toRight(s"Invalid position name: $str")

  def unsafeFromString(str: String): PositionName =
    fromString(str).fold(sys.error, identity)
}
