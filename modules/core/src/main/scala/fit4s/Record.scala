package fit4s

import scodec._
import scodec.codecs._

case class Record(header: RecordHeader, content: FitMessage)

object Record {

  val encoder: Encoder[Record] =
    Encoder(r =>
      Encoder.encodeBoth(RecordHeader.codec, FitMessage.encoder)(r.header, r.content)
    )

  def decoder(prev: List[Record]): Decoder[Record] =
    RecordHeader.codec.flatMap(rh => FitMessage.decoder(prev)(rh).map(m => Record(rh, m)))

  private[fit4s] def dmCodec: Codec[Record] =
    RecordHeader.codec
      .flatPrepend(rh =>
        if (rh.messageType.isDefinitionMessage)
          FitMessage.DefinitionMessage.codec.upcast[FitMessage].hlist
        else fail[FitMessage](Err("Expected a definition message in first record!")).hlist
      )
      .as[Record]
}
