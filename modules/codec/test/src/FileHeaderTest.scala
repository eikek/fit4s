package fit4s.codec

import munit.FunSuite
import scodec.bits.hex

class FileHeaderTest extends FunSuite:
  val sample = FileHeader(32, 21169, ByteSize.bytes(753603), 52716)
  val sampleEncoded = hex"0e20b152c37f0b002e464954eccd"

  test("header crc"):
    assertEquals(sample.updateCrc.crc, sample.crc)
    val h = FileHeader(16, 254, ByteSize.bytes(31546), 0)
    assertEquals(h.updateCrc.crc, 57167)

  test("header encode"):
    assertEquals(sample.encoded, sampleEncoded)
    val h = FileHeader(16, 254, ByteSize.bytes(31546), 0).updateCrc
    assertEquals(h.encoded, hex"0e10fe003a7b00002e4649544fdf")

  test("header decode"):
    assertEquals(FileHeader.decode(sampleEncoded).require, sample)
    assertEquals(
      FileHeader.decode(hex"0e10fe003a7b00002e4649544fdf").require,
      FileHeader(16, 254, ByteSize.bytes(31546), 0).updateCrc
    )
    // allow crc==0
    assertEquals(
      FileHeader.decode(hex"0e10fe003a7b00002e4649540000").require,
      FileHeader(16, 254, ByteSize.bytes(31546), 0)
    )

  test("decode: remaining bytes trigger a failure"):
    assert(FileHeader.decode(hex"0e10fe003a7b00002e4649544fdfcaffee").isFailure)

  test("decode: not .FIT ascii"):
    assert(FileHeader.decode(hex"0e10fe003a7b00002e4650494fdf").isFailure)

  test("decode: invalid crc"):
    assert(FileHeader.decode(hex"0e10fe003a7b00002e4649544fac").isFailure)

  test("decode: allow with invalid crc if requested"):
    assertEquals(
      FileHeader.decode(hex"0e10fe003a7b00002e4649544fac", checkCrc = false).require,
      FileHeader(16, 254, ByteSize.bytes(31546), 44111)
    )

  test("decode 12 byte header"):
    assertEquals(
      FileHeader.decode(hex"0c003900d91200002e464954").require,
      FileHeader(0, 57, ByteSize.bytes(4825), 0)
    )

  test("reading from fit files"):
    val c = FileHeader.codec(checkCrc = true)
    TestData.Activities.all.foreach { file =>
      val header = c.decodeValue(file.contents.bits).require
      val origLen = scodec.codecs.ushort8.decodeValue(file.contents.bits).require
      if (origLen == 14) {
        assertEquals(
          header.encoded,
          file.contents.take(origLen),
          s"Header encoding doesn't match for ${file.name}: ${header.encoded} vs ${file.contents.take(origLen)}"
        )
      }
    }
