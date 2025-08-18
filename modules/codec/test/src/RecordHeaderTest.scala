package fit4s.codec

import fit4s.codec.internal.RecordCodec

import munit.FunSuite
import scodec.DecodeResult
import scodec.bits.ByteOrdering.BigEndian
import scodec.bits.{BitVector, hex}

class RecordHeaderTest extends FunSuite:

  test("read compressed ts header"):
    val data = hex"f3"
    assertEquals(
      RecordCodec.dataRecordHeader.decode(data.bits).require,
      DecodeResult(CompressedTimestampHeader(3, 19), BitVector.empty)
    )

  test("read normal header"):
    val data = hex"40"
    assertEquals(
      RecordCodec.defMsgHeader.complete.decodeValue(data.bits).require,
      NormalRecordHeader(false, 0)
    )

  test("decode compressed timestamp 1"):
    val reference = hex"0101013b"
    val referenceTs = reference.toLong(false, BigEndian)

    val rh1 =
      CompressedTimestampHeader.create(3, BitVector.fromValidBin("11011")).require
    assertEquals(rh1.resolve(referenceTs), 16843067L)

    val rh2 =
      CompressedTimestampHeader.create(3, BitVector.fromValidBin("11101")).require
    assertEquals(rh2.resolve(referenceTs), 16843069L)

    val rh3 = CompressedTimestampHeader.create(3, BitVector.fromValidBin("00010")).require
    assertEquals(rh3.resolve(referenceTs), 16843074L)

    val rh4 = CompressedTimestampHeader.create(3, BitVector.fromValidBin("00101")).require
    assertEquals(rh4.resolve(referenceTs), 16843077L)

    val rh5 = CompressedTimestampHeader.create(3, BitVector.fromValidBin("00001")).require
    assertEquals(rh5.resolve(referenceTs), 16843073L)

  test("decode compressed timestamp 2"):
    val reference = hex"01010163"
    val referenceTs = reference.toLong(false, BigEndian)

    val rh1 =
      CompressedTimestampHeader.create(3, BitVector.fromValidBin("10010")).require
    assertEquals(rh1.resolve(referenceTs), 16843122L)

    val rh2 =
      CompressedTimestampHeader.create(3, BitVector.fromValidBin("00001")).require
    assertEquals(rh2.resolve(referenceTs), 16843137L)
