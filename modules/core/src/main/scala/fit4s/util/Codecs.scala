package fit4s.util

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

object Codecs:
  def uintx(bits: Int, bo: ByteOrdering): Codec[Int] =
    if (bo == ByteOrdering.BigEndian) uint(bits) else uintL(bits)

  def ulongx(bits: Int, bo: ByteOrdering): Codec[Long] =
    if (bo == ByteOrdering.BigEndian) ulong(bits) else ulongL(bits)

  def longx(bits: Int, bo: ByteOrdering): Codec[Long] =
    if (bo == ByteOrdering.BigEndian) long(bits) else longL(bits)

  def floatx(bo: ByteOrdering): Codec[Double] =
    (if (bo == ByteOrdering.BigEndian) float else floatL).xmap(_.toDouble, _.toFloat)

  def doublex(bo: ByteOrdering): Codec[Double] =
    if (bo == ByteOrdering.BigEndian) double else doubleL
