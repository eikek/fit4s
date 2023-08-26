package fit4s

sealed trait MessageType extends Product {
  def isDefinitionMessage: Boolean = this == MessageType.DefinitionMessage
  def isDataMessage: Boolean = this == MessageType.DataMessage
  def widen: MessageType = this
}

object MessageType {
  case object DefinitionMessage extends MessageType
  case object DataMessage extends MessageType
}
