package fit4s.decode

import fit4s.FieldDefinition
import fit4s.FitMessage.DataMessage
import scodec.bits.ByteVector

object DataMessageDecoder {

  def makeDataFields(dm: DataMessage): List[DataField] = {
    @annotation.tailrec
    def loop(
        fields: List[FieldDefinition],
        bytes: ByteVector,
        result: List[DataField]
    ): List[DataField] =
      fields match {
        case Nil => result.reverse
        case h :: t =>
          val field = dm.definition.profileMsg.flatMap(_.findField(h.fieldDefNum))
          val (raw, next) = bytes.splitAt(h.sizeBytes)
          loop(t, next, DataField(h, field, raw) :: result)
      }

    loop(dm.definition.fields, dm.raw, Nil)
  }

  def expandSubFields(fields: List[DataField]): List[DataField] =
    ???
}
