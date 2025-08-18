package fit4s.codec.internal

import scala.collection.mutable.ArrayBuffer

import scodec.bits.ByteVector

private[codec] object Crc:
  private val crcTable = ArrayBuffer(
    0x0000, 0xcc01, 0xd801, 0x1400, 0xf001, 0x3c00, 0x2800, 0xe401, 0xa001, 0x6c00,
    0x7800, 0xb401, 0x5000, 0x9c01, 0x8801, 0x4400
  )

  @inline private def updateLower(crc: Int, byte: Byte) =
    val temp = crcTable(crc & 0xf)
    val c1 = (crc >> 4) & 0x0fff
    c1 ^ temp ^ crcTable(byte & 0xf)

  @inline private def updateUpper(crc: Int, byte: Byte) =
    val temp = crcTable(crc & 0xf)
    val c1 = (crc >> 4) & 0x0fff
    c1 ^ temp ^ crcTable((byte >> 4) & 0xf)

  private def update(crc: Int, byte: Byte) =
    updateUpper(updateLower(crc, byte), byte)

  def apply(bv: ByteVector): Int =
    bv.foldLeft(0)(update)
