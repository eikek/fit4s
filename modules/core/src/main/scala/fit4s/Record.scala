package fit4s

import scodec._

case class Record(header: RecordHeader, content: FitMessage)

object Record {

  val encoder: Encoder[Record] =
    Encoder(r =>
      Encoder.encodeBoth(RecordHeader.codec, FitMessage.encoder)(r.header, r.content)
    )

  def decoder(prev: List[Record]): Decoder[Record] =
    RecordHeader.codec.flatMap(rh => FitMessage.decoder(prev)(rh).map(m => Record(rh, m)))

  object DefinitionRecord {
    def unapply(r: Record): Option[(RecordHeader, FitMessage.DefinitionMessage)] =
      if (r.header.messageType.isDefinitionMessage)
        Some(r.header -> r.content.asInstanceOf[FitMessage.DefinitionMessage])
      else None
  }

  object DataRecord {
    def unapply(r: Record): Option[(RecordHeader, FitMessage.DataMessage)] =
      if (r.header.messageType.isDataMessage)
        Some(r.header -> r.content.asInstanceOf[FitMessage.DataMessage])
      else None
  }
}
