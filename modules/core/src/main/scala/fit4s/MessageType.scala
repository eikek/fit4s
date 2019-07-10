package fit4s

sealed trait MessageType

object MessageType {
  case object DefinitionMessage extends MessageType
  case object DataMessage extends MessageType
}
