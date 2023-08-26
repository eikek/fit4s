package fit4s.common.borer

import io.bullet.borer.Encoder

trait EncoderHelper {

  def from[A](vs: A => List[(String, Option[JsonValue])]): Encoder[A] =
    Encoder { (w, a) =>
      val values = vs(a).flatMap(t => t._2.map(v => t._1 -> v))
      if (w.writingJson) w.writeMapStart()
      else w.writeMapHeader(values.size)
      values.foreach { case (k, v) =>
        w.writeMapMember(k, v)
      }
      w.writeMapClose()
    }
}

object EncoderHelper extends EncoderHelper
