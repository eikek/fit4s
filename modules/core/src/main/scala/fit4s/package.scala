import scodec.bits.ByteOrdering
import scodec._
import scodec.codecs._

package object fit4s {

  object codecs {

    def uintx(bits: Int, bo: ByteOrdering): Codec[Int] =
      if (bo == ByteOrdering.BigEndian) uint(bits) else uintL(bits)

    def ulongx(bits: Int, bo: ByteOrdering): Codec[Long] =
      if (bo == ByteOrdering.BigEndian) ulong(bits) else ulongL(bits)

    def longx(bits: Int, bo: ByteOrdering): Codec[Long] =
      if (bo == ByteOrdering.BigEndian) long(bits) else longL(bits)

    def floatx(bo: ByteOrdering): Codec[Float] =
      if (bo == ByteOrdering.BigEndian) float else floatL

    def doublex(bo: ByteOrdering): Codec[Double] =
      if (bo == ByteOrdering.BigEndian) double else doubleL

  }
}
