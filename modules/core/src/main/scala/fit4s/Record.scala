package fit4s

import scodec._

case class Record(header: RecordHeader, content: FitMessage)

object Record {

  val codec: Codec[Record] =
    RecordHeader.codec.flatPrepend(rh => FitMessage.codec(rh).hlist).as[Record]

}
