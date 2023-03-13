package fit4s

sealed trait MessageType {
  def isDefinitionMessage: Boolean = this == MessageType.DefinitionMessage
  def isDataMessage: Boolean = this == MessageType.DataMessage
}

object MessageType {
  case object DefinitionMessage extends MessageType
  case object DataMessage extends MessageType
}
