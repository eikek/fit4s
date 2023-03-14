import scodec.bits.ByteOrdering
import scodec._
import scodec.codecs._

package object fit4s {

  object codecs {

    def uintx(bits: Int, bo: ByteOrdering): Codec[Int] =
      if (bo == ByteOrdering.BigEndian) uint(bits) else uintL(bits)

    def ulongx(bits: Int, bo: ByteOrdering): Codec[Long] =
      if (bo == ByteOrdering.BigEndian) ulong(bits) else ulongL(bits)
  }
}
